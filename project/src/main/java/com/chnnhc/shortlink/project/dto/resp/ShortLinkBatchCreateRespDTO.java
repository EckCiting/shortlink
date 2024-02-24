package com.chnnhc.shortlink.project.dto.resp;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 短链接批量创建响应对象 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkBatchCreateRespDTO {

  /** 成功数量 */
  private Integer total;

  /** 批量创建返回参数 */
  private List<ShortLinkBaseInfoRespDTO> baseLinkInfos;
}
