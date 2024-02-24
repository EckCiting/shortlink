package com.chnnhc.shortlink.project.controller;

import com.chnnhc.shortlink.project.common.result.Result;
import com.chnnhc.shortlink.project.common.result.Results;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.chnnhc.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecycleBinController {

  private final RecycleBinService recycleBinService;

  /** 保存回收站 */
  @PostMapping("/api/short-link/v1/recycle-bin/save")
  public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
    recycleBinService.saveRecycleBin(requestParam);
    return Results.success();
  }

  /**
   * 恢复短链接
   */
  @PostMapping("/api/short-link/v1/recycle-bin/recover")
  public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
    recycleBinService.recoverRecycleBin(requestParam);
    return Results.success();
  }

  /**
   * 移除短链接
   */
  @PostMapping("/api/short-link/v1/recycle-bin/remove")
  public Result<Void> removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam) {
    recycleBinService.removeRecycleBin(requestParam);
    return Results.success();
  }


}
