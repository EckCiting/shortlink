package com.chnnhc.shortlink.admin.remote;

import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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

}
