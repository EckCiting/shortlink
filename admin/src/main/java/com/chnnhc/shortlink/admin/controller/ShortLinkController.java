package com.chnnhc.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.admin.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.chnnhc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "shortLinkControllerByAdmin")
@RequiredArgsConstructor
public class ShortLinkController {
  private final ShortLinkActualRemoteService shortLinkActualRemoteService;

  /** 创建短链接 */
  @PostMapping("/api/short-link/admin/v1/create")
  public Result<ShortLinkCreateRespDTO> createShortLink(
      @RequestBody ShortLinkCreateReqDTO requestParam) {
    return shortLinkActualRemoteService.createShortLink(requestParam);
  }

  /** 分页查询短链接 */
  /** 分页查询短链接 */
  @GetMapping("/api/short-link/admin/v1/page")
  public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
    return shortLinkActualRemoteService.pageShortLink(
        requestParam.getGid(),
        requestParam.getOrderTag(),
        requestParam.getCurrent(),
        requestParam.getSize());
  }
}
