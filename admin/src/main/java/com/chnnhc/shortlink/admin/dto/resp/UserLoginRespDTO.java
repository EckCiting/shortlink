package com.chnnhc.shortlink.admin.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户登录接口返回响应 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRespDTO {
  /** token */
  private String token;
}
