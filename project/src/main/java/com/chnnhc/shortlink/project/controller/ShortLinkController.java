package com.chnnhc.shortlink.project.controller;

import com.chnnhc.shortlink.project.common.result.Result;
import com.chnnhc.shortlink.project.common.result.Results;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.engine.IterationStatusVar;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {

  private final ShortLinkService shortLinkService;

  /** 创建短链接 */
  @PostMapping("/api/short-link/v1/create") public Result<ShortLinkCreateRespDTO> createShortLink(
      @RequestBody ShortLinkCreateReqDTO requestParam) {
    return Results.success(shortLinkService.createShortLink(requestParam));
  }

}
