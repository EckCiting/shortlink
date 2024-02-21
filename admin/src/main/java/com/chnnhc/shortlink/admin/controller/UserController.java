package com.chnnhc.shortlink.admin.controller;

/*
 * 用户管理控制层
 */

import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.common.result.Results;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /** 注册用户 */
  @PostMapping("/api/short-link/admin/v1/user")
  public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
    userService.register(requestParam);
    return Results.success();
  }

  /** 查询用户名是否存在 */
  @GetMapping("/api/short-link/admin/v1/user/has-username")
  public Result<Boolean> hasUsername(@RequestParam("username") String username) {
    return Results.success(userService.hasUsername(username));
  }
}
