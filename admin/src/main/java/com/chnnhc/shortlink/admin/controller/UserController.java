package com.chnnhc.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.chnnhc.shortlink.admin.common.result.Result;
import com.chnnhc.shortlink.admin.common.result.Results;
import com.chnnhc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserActualRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserRespDTO;
import com.chnnhc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/*
 * 用户管理控制层
 */
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
  public Result<Boolean> checkLogin(
      @RequestParam("username") String username, @RequestParam("token") String token) {
    return Results.success(userService.checkLogin(username, token));
  }

  /** 用户退出登录 */
  @DeleteMapping("/api/short-link/admin/v1/user/logout")
  public Result<Void> logout(
      @RequestParam("username") String username, @RequestParam("token") String token) {
    userService.logout(username, token);
    return Results.success();
  }

  /** 根据用户名查询用户信息 */
  @GetMapping("/api/short-link/admin/v1/user/{username}")
  public Result<UserRespDTO> getUsreByUsername(@PathVariable("username") String username) {
    return Results.success(userService.getUserByUsername(username));
  }

  /** 根据用户名查询无脱敏用户信息 */
  @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
  public Result<UserActualRespDTO> getActualUsreByUsername(
      @PathVariable("username") String username) {
    // BeanUtil.toBean 将 UserRespDTO 转成了 UserActualRespDTO. 依靠反射原理进行属性匹配
    // 如果 UserRespDTO 和 UserActualRespDTO 之间有相同名称和兼容类型的属性，BeanUtil.toBean 方法可以将这些属性从一个对象复制到另一个对象
    return Results.success(
        BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
  }

  /** 修改用户 */
  @PutMapping("/api/short-link/admin/v1/user")
  public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
    userService.update(requestParam);
    return Results.success();
  }
}
