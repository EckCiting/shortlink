package com.chnnhc.shortlink.admin.common.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

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
