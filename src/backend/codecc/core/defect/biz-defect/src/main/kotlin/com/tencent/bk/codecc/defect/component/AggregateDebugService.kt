package com.tencent.bk.codecc.defect.component

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.io.Files
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.NewAggregateDebugConfig
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit

@Component
class AggregateDebugService constructor(
    private val client: Client,
    private val redisTemplate: RedisTemplate<String, String>,
    private val scmJsonComponent: ScmJsonComponent
) {

    private val newAggregateDebugConfigCache = CacheBuilder.newBuilder()
            .maximumSize(ComConstants.COMMON_NUM_10L)
            .expireAfterWrite(ComConstants.COMMON_NUM_10L, TimeUnit.MINUTES)
            .build(object : CacheLoader<String, NewAggregateDebugConfig>() {
                override fun load(key: String): NewAggregateDebugConfig {
                    return try {
                        getNewAggregateDebugConfig() ?: NewAggregateDebugConfig()
                    } catch (t: Throwable) {
                        NewAggregateDebugConfig()
                    }
                }
            })

    private fun getNewAggregateDebugConfig(): NewAggregateDebugConfig? {
        val configBaseData = client.get(ServiceBaseDataResource::class.java)
                .getInfoByTypeAndCode(
                    NEW_AGGREGATE_DEBUG_CONFIG_TYPE,
                    NEW_AGGREGATE_DEBUG_CONFIG_TYPE
                ).data?.firstOrNull() ?: return null
        return if (configBaseData.paramValue.isNullOrEmpty()) {
            null
        } else {
            JsonUtil.to(configBaseData.paramValue, NewAggregateDebugConfig::class.java)
        }
    }

    fun isMatchNewAggregate(taskId: Long, toolName: String, checker: String): Boolean {
        try {
            val config = newAggregateDebugConfigCache.get(NEW_AGGREGATE_DEBUG_CONFIG_TYPE)
            // 判断是否为空配置（规则和任务ID都为空）
            if (config.rules.isEmpty() && config.taskIds.isEmpty()) {
                return false
            }
            if (!config.taskIds.contains(taskId)) {
                return false
            }
            return config.rules.any { it.toolName == toolName && it.checkers.contains(checker) }.also {
                if (it) {
                    logger.info("taskId: $taskId, toolName: $toolName, checker: $checker match new aggregate!")
                }
            }
        } catch (e: Exception) {
            logger.error("isMatchNewAggregate cause error. taskId: $taskId, toolName: $toolName, checker: $checker", e)
            return false
        }
    }

    fun regroupDefectsByMessage(
        taskId: Long,
        toolName: String,
        checker: String,
        oldAggregateOutputList: List<AggregateDefectOutputModelV2<LintDefectV2Entity>>
    ): List<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        try {
            if (oldAggregateOutputList.isEmpty()) {
                return oldAggregateOutputList
            }
            // 再次确认符合规则
            if (!isMatchNewAggregate(taskId, toolName, checker)) {
                return oldAggregateOutputList
            }
            // 按告警消息进行二次分组聚合
            return oldAggregateOutputList.map { it.defects }.flatMap { defects ->
                defects.groupBy { it.message }.values
            }.map {
                AggregateDefectOutputModelV2(
                    defects = it
                )
            }
        } catch (e: Exception) {
            logger.error("furtherAggregate cause error. taskId: $taskId, toolName: $toolName, checker: $checker", e)
            return oldAggregateOutputList
        }
    }

    fun isDebugTaskId(taskId: Long): Boolean {
        return try {
            redisTemplate.opsForSet()
                    .isMember(RedisKeyConstants.DEBUG_BUILD_ID_SET_KEY, taskId.toString()) ?: false
        } catch (e: Exception) {
            logger.error("postHandleDefectListDebugEnabled cause error. $taskId", e)
            false
        }
    }

    /**
     * 检查是否调试任务，如果是，写入调试文件
     */
    fun checkAndOutputDebugInfo(taskId: Long, info: Any) {
        try {
            if (!isDebugTaskId(taskId)) {
                return
            }
            val inputFileName = "${taskId}_${UUIDUtil.generate()}_aggregate_debug_data.json"
            val inputFilePath = scmJsonComponent.index(inputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate debug file path $inputFilePath")
            val inputFile = File(inputFilePath).apply {
                if (!exists()) {
                    parentFile.mkdirs()
                    createNewFile()
                }
            }
            Files.write(JsonUtil.toJson(info).toByteArray(), inputFile)
            scmJsonComponent.upload(inputFilePath, inputFileName, ScmJsonComponent.AGGREGATE)
            logger.info("aggregate debug upload success $taskId")
        } catch (e: Exception) {
            logger.error("outputDebugInfo cause error. $taskId", e)
        }
    }

    companion object {
        private const val NEW_AGGREGATE_DEBUG_CONFIG_TYPE = "NEW_AGGREGATE_DEBUG_CONFIG"
        private val logger: Logger = LoggerFactory.getLogger(AggregateDebugService::class.java)
    }
}
