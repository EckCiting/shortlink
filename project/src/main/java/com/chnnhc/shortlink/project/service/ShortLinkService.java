package com.chnnhc.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.SneakyThrows;

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

  /**
   * 批量创建短链接
   *
   * @param requestParam 批量创建短链接请求参数
   * @return 批量创建短链接返回参数
   */
  ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);

  /**
   * 修改短链接
   *
   * @param requestParam 修改短链接请求参数
   */
  void updateShortLink(ShortLinkUpdateReqDTO requestParam);

  /**
   * 短链接跳转
   *
   * @param shortUri 短链接后缀
   * @param request  HTTP 请求
   * @param response HTTP 响应
   */
  void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

  /**
   * 短链接统计
   *
   * @param fullShortUrl         完整短链接
   * @param gid                  分组标识
   * @param shortLinkStatsRecord 短链接统计实体参数
   */
  void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO shortLinkStatsRecord);

}
