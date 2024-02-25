/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chnnhc.shortlink.project.mq.consumer;

import com.chnnhc.shortlink.project.common.convention.exception.ServiceException;
import com.chnnhc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.chnnhc.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import com.chnnhc.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static com.chnnhc.shortlink.project.common.constant.RedisKeyConstant.DELAY_QUEUE_STATS_KEY;

/** 延迟记录短链接统计组件 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsConsumer implements InitializingBean {

  private final RedissonClient redissonClient;
  private final ShortLinkService shortLinkService;
  private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

  public void onMessage() {
    // 创建一个单线程的线程池，自定义线程工厂来设置线程的名称和守护线程标志
    Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable);
              thread.setName("delay_short-link_stats_consumer");
              thread.setDaemon(Boolean.TRUE);
              return thread;
            })
        .execute(
            () -> {
              // 获取阻塞队列
              RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque =
                  redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
              // 从阻塞队列创建延迟队列
              RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue =
                  redissonClient.getDelayedQueue(blockingDeque);
              for (; ; ) {
                try {
                  // 从延迟队列中获取消息，如果没有消息则立即返回null
                  ShortLinkStatsRecordDTO statsRecord = delayedQueue.poll();
                  if (statsRecord != null) {
                    // 检查消息是否已经被处理
                    if (!messageQueueIdempotentHandler.isMessageProcessed(statsRecord.getKeys())) {
                      // 检查消息是否已完成其业务流程
                      if (messageQueueIdempotentHandler.isAccomplish(statsRecord.getKeys())) {
                        return; // 如果已完成，则结束执行
                      }
                      throw new ServiceException("消息未完成流程，需要消息队列重试");
                    }
                    // 未被处理则处理消息
                    try {
                      // 调用业务方法处理消息
                      shortLinkService.shortLinkStats(null, null, statsRecord);
                    } catch (Throwable ex) {
                      // 处理异常，移除消息处理标记，记录错误日志
                      messageQueueIdempotentHandler.delMessageProcessed(statsRecord.getKeys());
                      log.error("延迟记录短链接监控消费异常", ex);
                    }
                    // 设置消息为已完成状态
                    messageQueueIdempotentHandler.setAccomplish(statsRecord.getKeys());
                    continue;
                  }
                  // 如果队列为空，暂停当前线程一段时间（这里是0.5秒），避免CPU空转
                  LockSupport.parkUntil(500);
                } catch (Throwable ignored) {
                }
              }
            });
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    onMessage();
  }
}
