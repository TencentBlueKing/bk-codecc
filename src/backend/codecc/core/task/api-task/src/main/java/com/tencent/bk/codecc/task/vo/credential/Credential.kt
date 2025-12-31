package com.tencent.bk.codecc.task.vo.credential

import io.swagger.v3.oas.annotations.media.Schema
data class Credential (
        @get:Schema(description = "凭证ID")
        val certificateId: String,
        @get:Schema(description = "凭据名称")
        val certificateName : String,
        @get:Schema(description = "凭据类型")
        val credentialType : String
)