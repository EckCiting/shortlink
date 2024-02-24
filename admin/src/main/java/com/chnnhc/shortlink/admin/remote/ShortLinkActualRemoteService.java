package com.chnnhc.shortlink.admin.remote;

import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** 短链接中台远程调用服务 */
@FeignClient(value = "short-link-project", url = "${aggregation.remote-url:}")
public interface ShortLinkActualRemoteService {
  /**
   * 查询分组短链接总量
   *
   * @param requestParam 分组短链接总量请求参数
   * @return 查询分组短链接总量响应
   */
  @GetMapping("/api/short-link/v1/count")
  Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(
      @RequestParam("requestParam") List<String> requestParam);

  /**
   * 保存回收站
   *
   * @param requestParam 请求参数
   */
  @PostMapping("/api/short-link/v1/recycle-bin/save")
  void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

  /**
   * 恢复短链接
   *
   * @param requestParam 短链接恢复请求参数
   */
  @PostMapping("/api/short-link/v1/recycle-bin/recover")
  void recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam);

  /**
   * 移除短链接
   *
   * @param requestParam 短链接移除请求参数
   */
  @PostMapping("/api/short-link/v1/recycle-bin/remove")
  void removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam);
}
