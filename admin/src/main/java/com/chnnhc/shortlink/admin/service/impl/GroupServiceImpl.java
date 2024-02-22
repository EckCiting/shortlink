package com.chnnhc.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.admin.common.biz.admin.UserContext;
import com.chnnhc.shortlink.admin.common.convention.exception.ClientException;
import com.chnnhc.shortlink.admin.dao.entity.GroupDO;
import com.chnnhc.shortlink.admin.dao.mapper.GroupMapper;
import com.chnnhc.shortlink.admin.service.GroupService;
import com.chnnhc.shortlink.admin.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.chnnhc.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

  private final RedissonClient redissonClient;

  @Value("${short-link.group.max-num}")
  private Integer groupMaxNum;

  @Override
  public void saveGroup(String username, String groupName) {
    // 获取一个分布式锁，锁的键是基于用户名的，确保对同一用户的操作是串行的
    RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
    lock.lock(); // 上锁
    try {
      // 构建查询条件，查询指定用户的分组信息，且分组未被标记为删除
      LambdaQueryWrapper<GroupDO> queryWrapper =
          Wrappers.lambdaQuery(GroupDO.class)
              .eq(GroupDO::getUsername, username)
              .eq(GroupDO::getDelFlag, 0);
      // 执行查询操作，获取该用户的所有有效分组
      List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
      // 检查该用户的分组数量是否已达到上限
      if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
        // 如果已达上限，则抛出异常，提示用户
        throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
      }
      String gid;
      do {
        // 生成一个随机的分组ID
        gid = RandomGenerator.generateRandom();
        // 确保生成的分组ID在该用户的分组中是唯一的
      } while (!hasGid(username, gid));
      // 使用建造者模式创建一个GroupDO对象，设置分组的相关属性
      GroupDO groupDO =
          GroupDO.builder()
              .gid(gid)
              .sortOrder(0) // 分组的排序顺序设置为0
              .username(username) // 设置分组所属的用户名
              .name(groupName) // 设置分组的名称
              .build();
      // 将新建的分组信息插入到数据库中
      baseMapper.insert(groupDO);
    } finally {
      // 在finally块中释放锁，确保锁一定会被释放，避免死锁的发生
      lock.unlock();
    }
  }

  private boolean hasGid(String username, String gid) {
    LambdaQueryWrapper<GroupDO> queryWrapper =
        Wrappers.lambdaQuery(GroupDO.class)
            .eq(GroupDO::getGid, gid)
            .eq(
                GroupDO::getUsername,
                Optional.ofNullable(username).orElse(UserContext.getUserName()));
    GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
    return hasGroupFlag == null;
  }
}
