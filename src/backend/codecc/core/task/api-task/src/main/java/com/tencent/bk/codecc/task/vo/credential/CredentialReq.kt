package com.tencent.bk.codecc.task.vo.credential

import io.swagger.v3.oas.annotations.media.Schema
data class CredentialReq (
        @get:Schema(description = "凭据名称")
        val credentialName : String,
        @get:Schema(description = "凭据类型")
        val credentialType : String,
        @get:Schema(description = "凭据内容1")
        val credentialV1 : String,
        @get:Schema(description = "凭据内容2")
        val credentialV2 : String?,
        @get:Schema(description = "凭据内容3")
        val credentialV3 : String?,
        @get:Schema(description = "凭据内容4")
        val credentialV4 : String?
)