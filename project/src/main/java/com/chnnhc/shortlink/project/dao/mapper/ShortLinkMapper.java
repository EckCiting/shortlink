package com.chnnhc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;

public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

  /** 分页统计短链接 */
  IPage<ShortLinkDO> pageLink(ShortLinkPageReqDTO requestParam);
}
