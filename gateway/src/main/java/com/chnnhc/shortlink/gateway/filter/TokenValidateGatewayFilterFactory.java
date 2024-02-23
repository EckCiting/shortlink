package com.chnnhc.shortlink.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chnnhc.shortlink.gateway.config.Config;
import com.chnnhc.shortlink.gateway.dto.GatewayErrorResult;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/** SpringCloud Gateway Token 拦截器 */
@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

  private final StringRedisTemplate stringRedisTemplate;

  // 构造函数，通过依赖注入的方式获取StringRedisTemplate实例
  public TokenValidateGatewayFilterFactory(StringRedisTemplate stringRedisTemplate) {
    super(Config.class);
    this.stringRedisTemplate = stringRedisTemplate;
  }

  // 重写apply方法，定义过滤器的具体逻辑
  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest(); // 获取当前请求对象
      String requestPath = request.getPath().toString(); // 获取请求路径
      String requestMethod = request.getMethod().name(); // 获取请求方法
      // 检查请求路径是否在白名单中，不在白名单中需要进行Token验证
      if (!isPathInWhiteList(requestPath, requestMethod, config.getWhitePathList())) {
        String username = request.getHeaders().getFirst("username"); // 从请求头中获取用户名
        String token = request.getHeaders().getFirst("token"); // 从请求头中获取Token
        Object userInfo;
        // 验证用户名和Token的有效性
        if (StringUtils.hasText(username)
            && StringUtils.hasText(token)
            && (userInfo =
                    stringRedisTemplate.opsForHash().get("short-link:login:" + username, token))
                != null) {
          JSONObject userInfoJsonObject = JSON.parseObject(userInfo.toString()); // 将用户信息转换为JSON对象
          // 重新构建请求，添加用户ID和真实姓名到请求头中
          ServerHttpRequest.Builder builder =
              exchange
                  .getRequest()
                  .mutate()
                  .headers(
                      httpHeaders -> {
                        httpHeaders.set("userId", userInfoJsonObject.getString("id"));
                        httpHeaders.set(
                            "realName",
                            URLEncoder.encode(
                                userInfoJsonObject.getString("realName"), StandardCharsets.UTF_8));
                      });
          return chain.filter(exchange.mutate().request(builder.build()).build()); // 继续过滤器链
        }
        // 如果Token验证失败，则返回401未授权状态码
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.writeWith(
            Mono.fromSupplier(
                () -> {
                  DataBufferFactory bufferFactory = response.bufferFactory();
                  GatewayErrorResult resultMessage =
                      GatewayErrorResult.builder()
                          .status(HttpStatus.UNAUTHORIZED.value())
                          .message("Token validation error")
                          .build();
                  return bufferFactory.wrap(JSON.toJSONString(resultMessage).getBytes()); // 返回错误信息
                }));
      }
      return chain.filter(exchange); // 对于白名单中的路径直接放行
    };
  }

  // 检查请求路径是否在白名单中
  private boolean isPathInWhiteList(
      String requestPath, String requestMethod, List<String> whitePathList) {
    return (!CollectionUtils.isEmpty(whitePathList)
            && whitePathList.stream().anyMatch(requestPath::startsWith))
        || (Objects.equals(requestPath, "/api/short-link/admin/v1/user")
            && Objects.equals(requestMethod, "POST"));
  }
}
