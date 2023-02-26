package com.tencent.devops.common.util

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * buildID 流转缓存
 * 用于缓存来自/api/build的buildId, 后续请求其他服务时，可以对defect与report进行区分
 * 限制100000个，避免内存泄漏
 */
object TraceBuildIdThreadCacheUtils {

    private val logger = LoggerFactory.getLogger(TraceBuildIdThreadCacheUtils::class.java)

    //限制100000个，避免内存泄漏
    private const val CACHE_LIMIT: Long = 100000L


    private val cache = Caffeine.newBuilder()
            // 一小时过期，避免一些比较长时间的提单任务
            .expireAfterWrite(1, TimeUnit.HOURS)
            // 设置最大的数量
            .maximumSize(CACHE_LIMIT)
            .build<Long, String>()


    /**
     * 设置buildid缓存, key 值为线程ID
     */
    fun setBuildId(buildId: String) {
        try {
            val threadId = Thread.currentThread().id
            cache.put(threadId, buildId)
        } catch (e: Exception) {
            logger.error("setBuildId to Thread Cache Cause Error. $buildId", e)
        }
    }

    /**
     * buildid缓存 失效
     */
    fun removeBuildId() {
        try {
            val threadId = Thread.currentThread().id
            val buildId = cache.getIfPresent(threadId)
            if (buildId.isNullOrBlank()) {
                return
            }
            cache.invalidate(threadId)
        } catch (e: Exception) {
            logger.error("removeBuildId From Thread Cache Cause Error.", e)
        }
    }

    /**
     * 获取BuildId
     */
    fun getBuildId(): String? {
        return try {
            val threadId = Thread.currentThread().id
            cache.getIfPresent(threadId)
        } catch (e: Exception) {
            logger.error("getBuildId From Thread Cache Cause Error.", e)
            null
        }
    }
}