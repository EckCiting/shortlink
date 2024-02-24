package com.chnnhc.shortlink.admin.remote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import com.chnnhc.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.chnnhc.shortlink.admin.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import com.chnnhc.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO;
import com.chnnhc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
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
   * 创建短链接
   *
   * @param requestParam 创建短链接请求参数
   * @return 短链接创建响应
   */
  @PostMapping("/api/short-link/v1/create")
  Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam);

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
   * 分页查询短链接
   *
   * @param gid 分组标识
   * @param orderTag 排序类型
   * @param current 当前页
   * @param size 当前数据多少
   * @return 查询短链接响应
   */
  @GetMapping("/api/short-link/v1/page")
  Result<Page<ShortLinkPageRespDTO>> pageShortLink(
      @RequestParam("gid") String gid,
      @RequestParam("orderTag") String orderTag,
      @RequestParam("current") Long current,
      @RequestParam("size") Long size);

  /**
   * 分页查询回收站短链接
   *
   * @param gidList 分组标识集合
   * @param current 当前页
   * @param size 当前数据多少
   * @return 查询短链接响应
   */
  @GetMapping("/api/short-link/v1/recycle-bin/page")
  Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(
      @RequestParam("gidList") List<String> gidList,
      @RequestParam("current") Long current,
      @RequestParam("size") Long size);

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

  /**
   * 批量创建短链接
   *
   * @param requestParam 批量创建短链接请求参数
   * @return 短链接批量创建响应
   */
  @PostMapping("/api/short-link/v1/create/batch")
  Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(
      @RequestBody ShortLinkBatchCreateReqDTO requestParam);

  /**
   * 根据 URL 获取标题
   *
   * @param url 目标网站地址
   * @return 网站标题
   */
  @GetMapping("/api/short-link/v1/title")
  Result<String> getTitleByUrl(@RequestParam("url") String url);
}
