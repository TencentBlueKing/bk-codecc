package com.tencent.devops.common.api.pojo

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "状态")
data class SimpleResult(
        @get:Schema(description = "是否成功", required = true)
        val success: Boolean,
        @get:Schema(description = "错误信息", required = false)
        val message: String? = null
)