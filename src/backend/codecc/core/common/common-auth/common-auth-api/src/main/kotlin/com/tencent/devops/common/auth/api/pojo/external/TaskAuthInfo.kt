package com.tencent.devops.common.auth.api.pojo.external

data class TaskAuthInfo(
    val taskId: Long,
    val pipelineId: String,
    val createFrom: String
)