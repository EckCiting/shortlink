package com.chnnhc.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chnnhc.shortlink.admin.common.biz.admin.UserContext;
import com.chnnhc.shortlink.admin.common.convention.exception.ServiceException;
import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dao.entity.GroupDO;
import com.chnnhc.shortlink.admin.dao.mapper.GroupMapper;
import com.chnnhc.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.chnnhc.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.chnnhc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.chnnhc.shortlink.admin.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * URL 回收站接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service(value = "recycleBinServiceImplByAdmin")
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

  private final ShortLinkActualRemoteService shortLinkActualRemoteService;
  private final GroupMapper groupMapper;

  @Override
  public Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
    LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
        .eq(GroupDO::getUsername, UserContext.getUsername())
        .eq(GroupDO::getDelFlag, 0);
    List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);
    if (CollUtil.isEmpty(groupDOList)) {
      throw new ServiceException("用户无分组信息");
    }
    requestParam.setGidList(groupDOList.stream().map(GroupDO::getGid).toList());
    return shortLinkActualRemoteService.pageRecycleBinShortLink(requestParam.getGidList(), requestParam.getCurrent(), requestParam.getSize());
  }
}