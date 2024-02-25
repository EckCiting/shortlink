package com.chnnhc.shortlink.project;

import static com.chnnhc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static com.chnnhc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 初始化短链接监控消息队列消费者组 */
@Component
@RequiredArgsConstructor
public class ShortLinkStatsStreamInitializeTask implements InitializingBean {

  private final StringRedisTemplate stringRedisTemplate;

  // 所有 Bean 都初始化，但在应用程序开始接收外部请求之前
  @Override
  public void afterPropertiesSet() throws Exception {
    // 启动时检查Redis中是否留下存在特定消息队列的记录
    Boolean hasKey = stringRedisTemplate.hasKey(SHORT_LINK_STATS_STREAM_TOPIC_KEY);
    if (hasKey == null || !hasKey) {
      // 如果不存在，会新建一个消费组，并通过
      stringRedisTemplate
          .opsForStream()
          .createGroup(SHORT_LINK_STATS_STREAM_TOPIC_KEY, SHORT_LINK_STATS_STREAM_GROUP_KEY);
    }
  }
}
