package com.chnnhc.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkPageRespDTO;

import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {

  /**
   * 创建短链接
   *
   * @param requestParam 创建短链接请求参数
   * @return 短链接创建信息
   */
  ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

  /**
   * 查询短链接分组内数量
   *
   * @param requestParam 查询短链接分组内数量请求参数
   * @return 查询短链接分组内数量响应
   */
  List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

  /**
   * 分页查询短链接
   *
   * @param requestParam 分页查询短链接请求参数
   * @return 短链接分页返回结果
   */
  IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}
