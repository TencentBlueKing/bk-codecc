package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.web.mq.EXCHANGE_SCAN_FINISH
import com.tencent.devops.common.web.mq.ROUTE_SCAN_FINISH
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 用于发送扫描完成事件
 */
@Service
class ScanFinishEventService @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>,
    private val rabbitTemplate: RabbitTemplate,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScanFinishEventService::class.java)
        private const val delay = 30 * 1000
    }

    fun sendScanFinishEvent(taskId: Long, buildId: String) {
        logger.info("start to send finish event: {}, {}", taskId, buildId)
        val task = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)?.data ?: return
        //检查是否已经通知过
        val redisLock = RedisLock(
            redisTemplate,
            "SCAN_FINISH_EVENT:TASK_ID:${taskId}:BUILD_ID:${buildId}",
            task.timeout?.toLong() ?: TimeUnit.HOURS.toSeconds(24)
        )

        if (redisLock.tryLock()) {
            rabbitTemplate.convertAndSend(
                EXCHANGE_SCAN_FINISH,
                ROUTE_SCAN_FINISH,
                ScanTaskTriggerDTO(taskId, buildId)
            ) { message ->
                message.messageProperties.delay = delay
                return@convertAndSend message
            }
        }
    }
}