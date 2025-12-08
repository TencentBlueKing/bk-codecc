package com.tencent.bk.codecc.task.pojo

import io.swagger.v3.oas.annotations.media.Schema
data class TriggerPipelineOldRsp(
    @get:Schema(description = "显示页面路径")
    val displayAddress: String,
    @get:Schema(description = "构建id")
    val buildId: String,
    @get:Schema(description = "任务id")
    val taskId: Long,
    @get:Schema(description = "工具清单")
    val toolList: List<String>
)