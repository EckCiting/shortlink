package com.chnnhc.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupUpdateReqDTO {

  /** 分组 id */
  private String gid;

  /** 分组名 */
  private String name;

}
