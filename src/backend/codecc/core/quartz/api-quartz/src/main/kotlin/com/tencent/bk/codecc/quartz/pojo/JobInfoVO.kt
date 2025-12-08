package com.tencent.bk.codecc.quartz.pojo

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "执行job信息")
data class JobInfoVO(
        @get:Schema(description = "类源码下载地址")
        val classUrl: String,
        @get:Schema(description = "类名字")
        val className: String,
        @get:Schema(description = "job名字，唯一标识")
        val jobName: String,
        @get:Schema(description = "触发器名字")
        val triggerName: String,
        @get:Schema(description = "定时表达式")
        val cronExpression: String,
        @get:Schema(description = "job入参")
        val jobParam: MutableMap<String, Any>?,
        @get:Schema(description = "分片tag名")
        val shardTag: String,
        @get:Schema(description = "最后更新时间")
        val updatedDate: Long
)