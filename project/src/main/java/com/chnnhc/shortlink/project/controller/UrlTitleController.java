package com.chnnhc.shortlink.project.controller;

import com.chnnhc.shortlink.project.common.result.Result;
import com.chnnhc.shortlink.project.common.result.Results;
import com.chnnhc.shortlink.project.service.UrlTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UrlTitleController {

  private final UrlTitleService urlTitleService;

  /** 根据 URL 获取对应网站的标题 */
  @GetMapping("/api/short-link/v1/title")
  public Result<String> getTitleByUrl(@RequestParam("url") String url) {
    return Results.success(urlTitleService.getTitleByUrl(url));
  }
}
