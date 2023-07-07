package com.tencent.bk.codecc.task.vo.credential

import io.swagger.annotations.ApiModelProperty

data class CredentialReq (
        @ApiModelProperty("凭据名称")
        val credentialName : String,
        @ApiModelProperty("凭据类型")
        val credentialType : String,
        @ApiModelProperty("凭据内容1")
        val credentialV1 : String,
        @ApiModelProperty("凭据内容2")
        val credentialV2 : String?,
        @ApiModelProperty("凭据内容3")
        val credentialV3 : String?,
        @ApiModelProperty("凭据内容4")
        val credentialV4 : String?
)