package com.tencent.bk.codecc.task.vo.credential

import io.swagger.v3.oas.annotations.media.Schema
data class CredentialRsp (
    @get:Schema(description = "项目ID")
        val projectId: String,
    @get:Schema(description = "凭证列表")
        val credentials: List<Credential>?,
    @get:Schema(description = "凭证列表")
        val credential: Credential?
)