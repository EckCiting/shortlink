package com.chnnhc.shortlink.admin.controller;

/*
 * 用户管理控制层
 */

import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.common.result.Results;
import com.chnnhc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserLoginRespDTO;
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

  /** 用户登录 */
  @PostMapping("/api/short-link/admin/v1/user/login")
  public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
    return Results.success(userService.login(requestParam));
  }
  /** 检查用户是否登录 */
  @GetMapping("/api/short-link/admin/v1/user/check-login")
  public Result<Boolean> checkLogin (@RequestParam("username") String username, @RequestParam("token") String token){
    return Results.success(userService.checkLogin(username,token));
  }

  /** 用户退出登录 */
  @DeleteMapping("/api/short-link/admin/v1/user/logout")
  public Result<Void> logout(@RequestParam("username") String username, @RequestParam("token") String token) {
    userService.logout(username, token);
    return Results.success();
  }
}
