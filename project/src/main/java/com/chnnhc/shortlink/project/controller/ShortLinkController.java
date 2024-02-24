package com.chnnhc.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chnnhc.shortlink.project.common.result.Result;
import com.chnnhc.shortlink.project.common.result.Results;
import com.chnnhc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.chnnhc.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {

  private final ShortLinkService shortLinkService;

  /** 创建短链接 */
  @PostMapping("/api/short-link/v1/create")
  public Result<ShortLinkCreateRespDTO> createShortLink(
      @RequestBody ShortLinkCreateReqDTO requestParam) {
    return Results.success(shortLinkService.createShortLink(requestParam));
  }

  /** 查询短链接分组内数量 */
  @GetMapping("/api/short-link/v1/count")
  public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(
      @RequestParam("requestParam") List<String> requestParam) {
    return Results.success(shortLinkService.listGroupShortLinkCount(requestParam));
  }

  /** 分页查询短链接 */
  @GetMapping("/api/short-link/v1/page")
  public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
    return Results.success(shortLinkService.pageShortLink(requestParam));
  }

  /** 批量创建短链接 */
  @PostMapping("/api/short-link/v1/create/batch")
  public Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(
      @RequestBody ShortLinkBatchCreateReqDTO requestParam) {
    return Results.success(shortLinkService.batchCreateShortLink(requestParam));
  }

  /**
   * 修改短链接
   */
  @PostMapping("/api/short-link/v1/update")
  public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
    shortLinkService.updateShortLink(requestParam);
    return Results.success();
  }
}
