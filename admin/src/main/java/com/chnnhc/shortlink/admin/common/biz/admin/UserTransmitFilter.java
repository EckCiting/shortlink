package com.chnnhc.shortlink.admin.common.biz.admin;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
* 用户信息传输过滤器
*/

@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

  @SneakyThrows
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
    // 将ServletRequest强制类型转换为HttpServletRequest，以便访问HTTP特定的方法。
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

    // 从HTTP请求头中获取用户名。
    String username = httpServletRequest.getHeader("username");
    // 检查用户名是否不为空或不仅仅是空白字符。
    if (StrUtil.isNotBlank(username)) {
      // 如果用户名有效，则进一步获取用户ID和真实姓名。
      String userId = httpServletRequest.getHeader("userId");
      String realName = httpServletRequest.getHeader("realName");

      // 使用获取的信息创建一个新的UserInfoDTO对象。
      UserInfoDTO userInfoDTO = new UserInfoDTO(userId, username, realName);

      // 将UserInfoDTO对象存储在UserContext中，以便在请求处理过程中随时访问用户信息。
      UserContext.setUser(userInfoDTO);

    }

    try {
      // 继续过滤器链的执行，传递控制给下一个过滤器或目标资源（如Servlet）。
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      // 在请求处理完成后，清理UserContext，移除之前存储的用户信息。
      // 这是为了避免内存泄漏和确保后续请求不会错误地访问上一个请求的用户信息。
      UserContext.removeUser();
    }
  }
}

