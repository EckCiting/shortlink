package com.chnnhc.shortlink.project.controller;

import com.chnnhc.shortlink.project.common.result.Result;
import com.chnnhc.shortlink.project.common.result.Results;
import com.chnnhc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import com.chnnhc.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {

  private final ShortLinkStatsService shortLinkStatsService;

  /**
   * 访问单个短链接指定时间内监控数据
   */
  @GetMapping("/api/short-link/v1/stats")
  public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
    return Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
  }

}
