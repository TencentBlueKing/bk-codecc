package com.tencent.bk.codecc.codeccjob.consumer

import com.tencent.bk.codecc.codeccjob.vo.LLMFilterProgressReqVO
import com.tencent.devops.common.constant.RedisKeyConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class LLMFilterProgressMQConsumer @Autowired constructor(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val redisTemplate: RedisTemplate<String, String>
) {
    companion object {
        private val logger = LoggerFactory.getLogger(LLMFilterProgressMQConsumer::class.java)
    }

    fun consumer(request: LLMFilterProgressReqVO) {
        logger.info("LLMFilterProgressMQConsumer request: {}", request)
        try {
            val done = redisTemplate.opsForValue().get(
                "${RedisKeyConstants.KEY_PROGRESS_DONE_PRE}${request.taskId}_${request.buildId}") ?: return
            val total = redisTemplate.opsForValue().get(
                "${RedisKeyConstants.KEY_PROGRESS_TOTAL_PRE}${request.taskId}_${request.buildId}") ?: return

            val message = "$done/$total"
            simpMessagingTemplate.convertAndSend(
                "/topic/defect/llm/${request.taskId}",
                message
            )
        } catch (e: Throwable) {
            logger.error("${e.message}: ${e.stackTraceToString()}")
        }
    }
}
