package com.tencent.bk.codecc.task.pojo

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "流水线触发请求体")
data class TriggerPipelineOldReq(
    @get:Schema(description = "仓库路径")
    val gitUrl: String?,
    @get:Schema(description = "分支")
    val branch: String?,
    @get:Schema(description = "是否显示告警")
    val defectDisplay: Boolean,
    @get:Schema(description = "触发来源")
    val triggerSource: String
)