package com.chnnhc.shortlink.admin.service;

import com.chnnhc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserActualRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.chnnhc.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService {
  /**
   * 注册用户
   *
   * @param requestParam 注册用户请求参数
   */
  void register(UserRegisterReqDTO requestParam);

  /**
   * 查询用户名是否存在
   *
   * @param username 用户名
   * @return 用户名存在返回 True，不存在返回 False
   */
  Boolean hasUsername(String username);

  /**
   * 用户登录
   *
   * @param requestParam 用户登录请求参数
   * @return 用户登录返回参数 Token
   */
  UserLoginRespDTO login(UserLoginReqDTO requestParam);

  /**
   * 检查用户是否登录
   *
   * @param username 用户名
   * @param token 用户登录 Token
   * @return 用户是否登录标识
   */
  Boolean checkLogin(String username, String token);

  /**
   * 退出登录
   *
   * @param username 用户名
   * @param token 用户登录 Token
   */
  void logout(String username, String token);

  /**
   * 根据用户名查询用户信息
   *
   * @param username 用户名
   * @return 用户返回实体
   */
  UserRespDTO getUserByUsername(String username);
}
