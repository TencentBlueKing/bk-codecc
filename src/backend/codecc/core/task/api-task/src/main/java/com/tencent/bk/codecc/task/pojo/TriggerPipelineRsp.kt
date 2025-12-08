package com.tencent.bk.codecc.task.pojo

import io.swagger.v3.oas.annotations.media.Schema
data class TriggerPipelineRsp(
    @get:Schema(description = "项目id")
    val projectId: String,
    @get:Schema(description = "流水线id")
    val pipelineId: String,
    @get:Schema(description = "任务id")
    val taskId: Long?,
    @get:Schema(description = "工具清单")
    val toolList: List<String>?,
    @get:Schema(description = "是否首次触发")
    val firstTrigger: String?,
    @get:Schema(description = "codecc构建id")
    val codeccBuildId: String?
)