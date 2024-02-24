package com.chnnhc.shortlink.project.dto.req;

import lombok.Data;

/** 回收站恢复功能*/
@Data
public class RecycleBinRecoverReqDTO {
  /** 分组 id */
  private String gid;

  /** 全部短链接 */
  private String fullShortUrl;
}

