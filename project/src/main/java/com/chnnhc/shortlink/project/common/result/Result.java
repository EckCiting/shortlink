package com.chnnhc.shortlink.project.common.result;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
  @Serial private static final long serialVersionUID = -481026807528097939L;

  /** 正确返回码 */
  public static final String SUCCESS_CODE = "0";

  /** 返回码 */
  private String code;

  /** 返回消息 */
  private String message;

  /** 响应数据 */
  private T data;

  /** 请求ID */
  private String requestId;

  private boolean isSuccess() {
    return SUCCESS_CODE.equals(code);
  }
}
