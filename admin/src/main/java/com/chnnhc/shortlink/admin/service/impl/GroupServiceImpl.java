package com.chnnhc.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.admin.common.biz.admin.UserContext;
import com.chnnhc.shortlink.admin.common.convention.exception.ClientException;
import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dao.entity.GroupDO;
import com.chnnhc.shortlink.admin.dao.mapper.GroupMapper;
import com.chnnhc.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.chnnhc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.chnnhc.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.chnnhc.shortlink.admin.service.GroupService;
import com.chnnhc.shortlink.admin.toolkit.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.chnnhc.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

  private final RedissonClient redissonClient;
  private final ShortLinkActualRemoteService shortLinkActualRemoteService;

  @Value("${short-link.group.max-num}")
  private Integer groupMaxNum;

  @Override
  public void saveGroup(String groupName) {
    saveGroup(UserContext.getUsername(), groupName);
  }

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

  @Override
  public List<ShortLinkGroupRespDTO> listGroup() {
    // 创建查询条件包装器，用于构建查询条件
    LambdaQueryWrapper<GroupDO> queryWrapper =
        Wrappers.lambdaQuery(GroupDO.class)
            .eq(GroupDO::getDelFlag, 0) // 查询条件：删除标志为0（未删除）
            .eq(GroupDO::getUsername, UserContext.getUsername()) // 查询条件：用户名等于当前登录用户的用户名
            .orderByDesc(
                GroupDO::getSortOrder, GroupDO::getUpdateTime); // 结果排序：首先按照排序顺序降序，然后按更新时间降序

    System.out.println(UserContext.getUsername());

    // 使用查询条件执行查询，获取符合条件的分组列表
    List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
    // 调用远程服务接口，获取每个分组对应的短链接数量
    Result<List<ShortLinkGroupCountQueryRespDTO>> listResult =
        shortLinkActualRemoteService.listGroupShortLinkCount(
            groupDOList.stream().map(GroupDO::getGid).toList());
    // 将查询结果的数据类型转换成所需的响应DTO类型
    List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList =
        BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
    // 遍历转换后的列表，为每个分组设置其对应的短链接数量
    shortLinkGroupRespDTOList.forEach(
        each -> {
          Optional<ShortLinkGroupCountQueryRespDTO> first =
              listResult.getData().stream()
                  .filter(item -> Objects.equals(item.getGid(), each.getGid())) // 筛选与当前分组GID相同的统计数据
                  .findFirst(); // 获取第一个匹配项，如果有的话
          // 如果找到匹配的统计数据，则设置当前分组的短链接数量
          first.ifPresent(item -> each.setShortLinkCount(first.get().getShortLinkCount()));
        });
    // 返回最终处理过的分组列表，每个分组包含其对应的短链接数量
    return shortLinkGroupRespDTOList;
  }

  @Override
  public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
    // 创建查询条件包装器，用于构建查询条件
    LambdaUpdateWrapper<GroupDO> updateWrapper =
        Wrappers.lambdaUpdate(GroupDO.class)
            .eq(GroupDO::getUsername, UserContext.getUsername())
            .eq(GroupDO::getGid, requestParam.getGid())
            .eq(GroupDO::getDelFlag, 0);
    // 构造需要存入的新 Group
    GroupDO groupDO = new GroupDO();
    groupDO.setName(requestParam.getName());
    // 更新数据库
    baseMapper.update(groupDO, updateWrapper);
  }

  @Override
  public void deleteGroup(String gid) {
    LambdaUpdateWrapper<GroupDO> updateWrapper =
        Wrappers.lambdaUpdate(GroupDO.class)
            .eq(GroupDO::getUsername, UserContext.getUsername())
            .eq(GroupDO::getGid, gid)
            .eq(GroupDO::getDelFlag, 0);
    GroupDO groupDO = new GroupDO();
    groupDO.setDelFlag(1);
    baseMapper.update(groupDO, updateWrapper);
  }

  @Override
  public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
    // 遍历请求参数中的每个元素
    requestParam.forEach(
        each -> {
          // 创建GroupDO对象，并设置排序顺序，使用建造者模式
          GroupDO groupDO =
              GroupDO.builder()
                  .sortOrder(each.getSortOrder()) // 设置排序顺序
                  .build();
          // 创建LambdaUpdateWrapper对象，用于构建更新条件
          LambdaUpdateWrapper<GroupDO> updateWrapper =
              Wrappers.lambdaUpdate(GroupDO.class)
                  .eq(GroupDO::getUsername, UserContext.getUsername())
                  .eq(GroupDO::getGid, each.getGid())
                  .eq(GroupDO::getDelFlag, 0);
          // 使用baseMapper执行更新操作，根据updateWrapper中的条件更新groupDO对象
          baseMapper.update(groupDO, updateWrapper);
        });
  }

  private boolean hasGid(String username, String gid) {
    LambdaQueryWrapper<GroupDO> queryWrapper =
        Wrappers.lambdaQuery(GroupDO.class)
            .eq(GroupDO::getGid, gid)
            .eq(
                GroupDO::getUsername,
                Optional.ofNullable(username).orElse(UserContext.getUsername()));
    GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
    return hasGroupFlag == null;
  }
}
