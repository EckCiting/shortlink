package com.chnnhc.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.admin.common.convention.exception.ClientException;
import com.chnnhc.shortlink.admin.common.convention.exception.ServiceException;
import com.chnnhc.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.chnnhc.shortlink.admin.dao.entity.UserDO;
import com.chnnhc.shortlink.admin.dao.mapper.UserMapper;
import com.chnnhc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserActualRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserRespDTO;
import com.chnnhc.shortlink.admin.service.GroupService;
import com.chnnhc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.chnnhc.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.chnnhc.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.chnnhc.shortlink.admin.common.enums.UserErrorCodeEnum.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

  private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
  private final RedissonClient redissonClient;
  private final StringRedisTemplate stringRedisTemplate;
  private final GroupService groupService;

  @Override
  public Boolean hasUsername(String username) {
    // 布隆过滤器
    return !userRegisterCachePenetrationBloomFilter.contains(username);
  }

  @Override
  public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
    // 使用LambdaQueryWrapper构建查询条件，查询用户信息
    LambdaQueryWrapper<UserDO> queryWrapper =
        Wrappers.lambdaQuery(UserDO.class)
            .eq(UserDO::getUsername, requestParam.getUsername())
            .eq(UserDO::getPassword, requestParam.getPassword())
            .eq(UserDO::getDelFlag, 0); // 确保用户未被删除，即DelFlag字段为0
    // 执行查询，返回单个用户对象
    UserDO userDO = baseMapper.selectOne(queryWrapper);
    if (userDO == null) {
      throw new ClientException("用户不存在");
    }
    // 查询Redis中是否已经有该用户的登录记录
    Map<Object, Object> hasLoginMap =
        stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + requestParam.getUsername());
    if (CollUtil.isNotEmpty(hasLoginMap)) {
      // 如果登录记录存在，从Redis中获取已有的token
      String token =
          hasLoginMap.keySet().stream()
              .findFirst()
              .map(Object::toString)
              .orElseThrow(() -> new ClientException("用户登录错误"));
      return new UserLoginRespDTO(token);
    }
    /** Hash Key：login_用户名 Value： Key：token标识 Val：JSON 字符串（用户信息） */
    // 生成一个新的UUID作为token
    String uuid = UUID.randomUUID().toString();
    // 将新的登录信息（token及用户信息）存入Redis
    stringRedisTemplate.opsForHash().put(USER_LOGIN_KEY + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));

    // 设置该登录记录的过期时间为30分钟
    stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
    return new UserLoginRespDTO(uuid);
  }

  @Override
  public void register(UserRegisterReqDTO requestParam) {
    // 判断用户是否存在
    if (!hasUsername(requestParam.getUsername())) {
      throw new ClientException(USER_NAME_EXIST);
    }
    // 借助 Redisson 的分布式锁来确保同一时间只有一个操作可以对同一个用户名执行注册操作
    RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
    try {
      // 尝试获取锁，如果获取成功，则继续执行；如果获取失败（即锁已被其他线程持有），则抛出异常
      if (lock.tryLock()) {
        try {
          // 将请求参数转换为用户实体对象，并插入到数据库中
          int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
          // 如果插入操作影响的行数小于1，说明保存用户信息失败，抛出异常
          if (inserted < 1) {
            throw new ClientException(USER_SAVE_ERROR);
          }
        } catch (DuplicateKeyException ex) {
          throw new ClientException(USER_EXIST);
        }
        // 将用户名添加到布隆过滤器中，用于缓存穿透保护
        userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        // 为新注册的用户创建默认分组
        groupService.saveGroup(requestParam.getUsername(), "默认分组");
        return;
      }
      throw new ClientException(USER_NAME_EXIST);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Boolean checkLogin(String username, String token) {
    return stringRedisTemplate.opsForHash().get("login_" + username, token) != null;
  }

  @Override
  public void logout(String username, String token) {
    if (checkLogin(username, token)) {
      stringRedisTemplate.delete("login_" + username);
      return;
    }
    throw new ClientException("用户Token不存在或用户未登录");
  }

  @Override
  public UserRespDTO getUserByUsername(String username) {
    LambdaQueryWrapper<UserDO> queryWrapper =
        Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
    UserDO userDO = baseMapper.selectOne(queryWrapper);
    if (userDO == null) {
      throw new ServiceException(USER_NULL);
    }
    UserRespDTO result = new UserRespDTO();
    // 不用构造器是因为参数太多
    BeanUtils.copyProperties(userDO, result);
    return result;
  }

  @Override
  public void update(UserUpdateReqDTO requestParam) {
    // TODO 验证当前用户名是否为登录用户
    LambdaUpdateWrapper<UserDO> updateWrapper =
        Wrappers.lambdaUpdate(UserDO.class).eq(UserDO::getUsername, requestParam.getUsername());
    // 将 UserUpdateReqDTO 对象转换成 UserDO对象。再将这个对象放入和 updateWrapper 匹配的记录中
    baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
  }
}
