package com.tencent.bk.codecc.task.vo.credential

import io.swagger.annotations.ApiModelProperty

data class CredentialRsp (
    @ApiModelProperty("项目ID")
        val projectId: String,
    @ApiModelProperty("凭证列表")
        val credentials: List<Credential>?,
    @ApiModelProperty("凭证列表")
        val credential: Credential?
)