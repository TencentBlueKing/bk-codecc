/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.schedule.component;

import com.tencent.bk.codecc.schedule.consumer.CommitPlatformConsumer;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 自定义redis mq 监听器
 *
 * @version V1.0
 * @date 2019/10/2
 */
@Component
@Slf4j
public class CustomRedisMQListener implements CommandLineRunner {
    /**
     * 因为commit是通过线程池asyncRedisMQExecutor起线程异步执行的，可能出现多个并发线程处理同一个key，虽然不会造成消息重复消费（在下一步
     * 获取消息处理时有加锁），但是拉起不必要的线程是浪费的，所以加个缓存记录正在执行的queue，并且通过异步线程同步缓存
     */
    private final Map<String, Future<Boolean>> processingQueue = new ConcurrentHashMap<>();
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CommitPlatformConsumer commitPlatformConsumer;
    @Autowired
    private Executor asyncRedisMQExecutor;

    @Override
    public void run(String... args) {

        // 创建一个异步线程，专门整理正在执行的queue
        asyncRedisMQExecutor.execute(() -> {
            log.info("************************ processing queue cleaner started ************************");
            int loopCount = 0;
            while (true) {
                try {
                    log.debug("begin clean, the processing queue size is {}", processingQueue.size());
                    Iterator<Map.Entry<String, Future<Boolean>>> it = processingQueue.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Future<Boolean>> queueResult = it.next();
                        try {
                            Boolean res = queueResult.getValue().get(ComConstants.COMMON_NUM_1L, TimeUnit.MILLISECONDS);
                            if (res != null && res) {
                                it.remove();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("An error occurred while cleaning the processingQueue.", e);
                            it.remove();
                        } catch (TimeoutException e) {
                            // 为了能快速的循环清理正在执行的queue，超时时间设置的比较短，会频繁的timeout，所以忽略该异常
                        }
                    }
                    log.debug("after clean, the processing queue size is {}", processingQueue.size());

                    // 每循环60000次（约1分钟）打印一次，表示线程存活
                    if (loopCount > 600) {
                        log.info("************************ processing queue cleaner working ************************");
                        loopCount = 0;
                    }

                    Thread.sleep(ComConstants.COMMON_NUM_100L);
                } catch (Throwable t) {
                    log.error("processing queue cleaner error:", t);
                } finally {
                    loopCount++;
                }
            }

        });

        // 创建一个异步线程，专门监听队列
        asyncRedisMQExecutor.execute(() -> {
            log.info("************************ custom redis mq listener started ************************");
            int loopCount = 0;
            while (true) {
                try {
                    Set<String> keys = redisTemplate.opsForSet().members(RedisKeyConstants.CUSTOM_REDIS_MQ_KEYS);
                    if (CollectionUtils.isNotEmpty(keys)) {
                        keys.forEach(key -> {
                            if (processingQueue.get(key) == null) {
                                log.debug("consumer redis mq: {}", key);
                                Future<Boolean> asyncResult = commitPlatformConsumer.commit(key);
                                processingQueue.put(key, asyncResult);
                            }
                        });
                    }

                    // 每循环6000次（约1分钟）打印一次，表示线程存活
                    if (loopCount > 60) {
                        log.info("************************ custom redis mq listener working ************************");
                        loopCount = 0;
                    }
                    Thread.sleep(ComConstants.COMMON_NUM_1000L);
                } catch (Throwable t) {
                    log.error("CustomRedisMQListener error:", t);
                } finally {
                    loopCount++;
                }
            }
        });
    }

}
