package com.chnnhc.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.admin.common.convention.exception.ClientException;
import com.chnnhc.shortlink.admin.dao.entity.UserDO;
import com.chnnhc.shortlink.admin.dao.mapper.UserMapper;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.service.GroupService;
import com.chnnhc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.chnnhc.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
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
}
