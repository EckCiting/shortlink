package com.chnnhc.shortlink.admin.dto.req;

import lombok.Data;

/** 回收站移除功能 */
@Data
public class RecycleBinRemoveReqDTO {
  /** 分组 id */
  private String gid;

  /** 全部短链接 */
  private String fullShortUrl;
}

