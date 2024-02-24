package com.chnnhc.shortlink.admin.controller;

import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.chnnhc.shortlink.admin.remote.dto.req.ShortLinkStatsReqDTO;
import com.chnnhc.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "shortLinkStatsControllerByAdmin")
@RequiredArgsConstructor
public class ShortLinkStatsController {

  private final ShortLinkActualRemoteService shortLinkActualRemoteService;

  /** 访问单个短链接指定时间内监控数据 */
  @GetMapping("/api/short-link/admin/v1/stats")
  public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
    return shortLinkActualRemoteService.oneShortLinkStats(
        requestParam.getFullShortUrl(),
        requestParam.getGid(),
        requestParam.getStartDate(),
        requestParam.getEndDate());
  }
}
