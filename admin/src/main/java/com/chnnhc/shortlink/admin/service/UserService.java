package com.chnnhc.shortlink.admin.service;

import com.chnnhc.shortlink.admin.dto.req.UserRegisterReqDTO;

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


}
