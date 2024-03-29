package com.chnnhc.shortlink.admin.config;

import com.chnnhc.shortlink.admin.common.biz.admin.UserTransmitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 用户配置自动装配 */
@Configuration
public class UserConfiguration {
  /** 用户信息传递过滤器 */
  @Bean
  public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter() {
    // 注册自定义过滤器
    FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new UserTransmitFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(0);
    return registration;
  }
}
