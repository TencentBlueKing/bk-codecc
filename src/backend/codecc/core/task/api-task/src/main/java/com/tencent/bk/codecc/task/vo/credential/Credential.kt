package com.tencent.bk.codecc.task.vo.credential

import io.swagger.annotations.ApiModelProperty

data class Credential (
        @ApiModelProperty("凭证ID")
        val certificateId: String,
        @ApiModelProperty("凭据名称")
        val certificateName : String,
        @ApiModelProperty("凭据类型")
        val credentialType : String
)