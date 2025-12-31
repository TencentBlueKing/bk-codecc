package com.tencent.devops.common.api.pojo

data class TaskInfo(
    val projectId: String,
    val taskId: Long,
    val pipelineId: String,
    val nameEn: String,
    val createFrom: String,
    val buildId: String,
    val createdBy: String
)
