package com.chnnhc.shortlink.project.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLinkCreateRespDTO {

  /** 分组信息 */
  private String gid;

  /** 原始链接 */
  private String originUrl;

  /** 短链接 */
  private String fullShortUrl;
}
