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

package com.tencent.bk.codecc.schedule.consumer;

import com.tencent.bk.codecc.schedule.dao.redis.AnalyzeHostPoolDao;
import com.tencent.bk.codecc.schedule.service.ScheduleService;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.redis.lock.JRedisLock;
import com.tencent.devops.common.codecc.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tencent.devops.common.constant.ComConstants.ACQUIRY_LOCK_EXPIRY_TIME_MILLIS;
import static com.tencent.devops.common.constant.ComConstants.COMMIT_PLATFORM_LOCK_EXPIRY_TIME_MILLIS;
import static com.tencent.devops.common.constant.ComConstants.REDIS_MQ_KEY_LOCK_EXPIRY_TIME_MILLIS;

/**
 * 分析任务消息队列的消费者
 *
 * @version V1.0
 * @date 2019/10/17
 */
@Component
@Slf4j
public class CommitPlatformConsumer {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private AnalyzeHostPoolDao analyzeHostPoolDao;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 告警提交到platform
     *
     * @param redisMQKey
     */
    @Async("asyncRedisMQExecutor")
    public Future<Boolean> commit(String redisMQKey) {
        log.debug("try to consumer queue: {}", redisMQKey);

        // 获取消息
        PushVO pushVO = getMessage(redisMQKey);

        if (pushVO == null) {
            return new AsyncResult<>(true);
        }
        log.info("commit: {}", pushVO);
        String ipAndPort = analyzeHostPoolDao.getAnalyzeHost(pushVO.getStreamName(), pushVO.getToolName(),
                pushVO.getBuildId());
        if (StringUtils.isEmpty(ipAndPort)) {
            log.error("can not find analyze host: {}", pushVO);
            // commit失败需要同步更新分析记录
            scheduleService.uploadAbortTaskLog(pushVO.getStreamName(), pushVO.getToolName(), pushVO.getBuildId(),
                    "调用分析服务器执行commit时找不到对应的服务器ip，请联系CodeCC助手");
            return new AsyncResult<>(true);
        }

        try {
            Boolean commitSuccess = scheduleService.commit(pushVO, ipAndPort);
            if (commitSuccess != null && commitSuccess) {
                log.info("commit success: {}", pushVO);
            } else {
                // commit失败需要同步更新分析记录
                scheduleService.uploadAbortTaskLog(pushVO.getStreamName(), pushVO.getToolName(), pushVO.getBuildId(),
                        "调用分析服务器执行commit失败，请联系CodeCC助手");
                log.info("commit fail: {}", pushVO);
            }
        } catch (Exception e) {
            log.error("commit fail!", e);
            // commit失败需要同步更新分析记录
            scheduleService.uploadAbortTaskLog(pushVO.getStreamName(), pushVO.getToolName(), pushVO.getBuildId(),
                    "调用分析服务器执行commit失败，请联系CodeCC助手" + e.getMessage());
        }
        return new AsyncResult<>(true);
    }

    /**
     * 从redis队列（左进右出）中获取消息，如果没有消息，就删除key
     *
     * @param redisMQKey
     * @return
     */
    private PushVO getMessage(String redisMQKey) {

        /* 获取消息前需要先加锁，两个目的：
         * 1.确保一个队列同一个时间只有一个客户端能取出消息，避免多个客户端同时取出消息后获取不到commit锁再塞回去，导致commit顺序错乱
         * 2.确保消息队列为空时，删除队列的同时，生产端往队列里面写消息，最终导致消息被误删
         */
        RLock lock = redissonClient.getLock(RedisKeyConstants.PREFIX_REDIS_MQ_KEY_LOCK + redisMQKey);
        try {
            // 获取锁最多等待10秒，上锁以后5秒自动解锁
            if (lock != null && lock.tryLock(ACQUIRY_LOCK_EXPIRY_TIME_MILLIS, REDIS_MQ_KEY_LOCK_EXPIRY_TIME_MILLIS,
                    TimeUnit.MILLISECONDS)) {

                // 如果队列中消息为空，则删除队列以及队列key
                final String msg = redisTemplate.opsForList().rightPop(redisMQKey);
                if (StringUtils.isEmpty(msg)) {
                    if (redisTemplate.opsForSet().isMember(RedisKeyConstants.CUSTOM_REDIS_MQ_KEYS, redisMQKey)) {
                        redisTemplate.delete(redisMQKey);
                        redisTemplate.opsForSet().remove(RedisKeyConstants.CUSTOM_REDIS_MQ_KEYS, redisMQKey);
                        log.info("delete queue success: {}", redisMQKey);
                    }
                } else {
                    PushVO pushVO = JsonUtil.INSTANCE.to(msg, PushVO.class);
                    String lockKey = String.format("%s:%s:%s",
                            RedisKeyConstants.CONCURRENT_COMMIT_LOCK,
                            pushVO.getStreamName(),
                            pushVO.getToolName());
                    JRedisLock jedisLock = new JRedisLock(
                            redisTemplate,
                            lockKey,
                            COMMIT_PLATFORM_LOCK_EXPIRY_TIME_MILLIS,
                            pushVO.getBuildId());

                    // 获取到commit platform锁才将消息返回给消费者处理，否则将消息放回队列最前面
                    if (jedisLock.acquireDiffClientLock()) {
                        return pushVO;
                    }

                    log.debug("can not acquire diff client lock, push back to the queue: {} - {}", redisMQKey, msg);
                    redisTemplate.opsForList().rightPush(redisMQKey, msg);
                }
            }
        } catch (Exception e) {
            log.error("deal queue fail: {}", redisMQKey, e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        return null;
    }

}
