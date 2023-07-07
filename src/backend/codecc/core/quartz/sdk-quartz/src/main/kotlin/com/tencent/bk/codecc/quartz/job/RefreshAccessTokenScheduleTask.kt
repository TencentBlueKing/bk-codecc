package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.auth.api.pojo.external.KEY_BACKEND_ACCESS_TOKEN
import com.tencent.devops.common.util.OkhttpUtils
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang.StringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

@Slf4j
class RefreshAccessTokenScheduleTask @Autowired constructor(
        private val redisTemplate: RedisTemplate<String, String>
) : IScheduleTask {


    override fun executeTask(quartzJobContext: QuartzJobContext) {
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshAccessTokenScheduleTask::class.java)
    }
}