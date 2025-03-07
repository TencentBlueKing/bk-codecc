package com.tencent.bk.codecc.defect.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import lombok.Data

/**
 * 大语言模型响应数据对象
 *
 * @version V1.0
 * @date 2023/11/16
 */
@Data
@ApiModel("大语言模型响应数据对象")
data class DefectSuggestionRespVO(
    @ApiModelProperty("大模型类型")
    var llmName: String? = null,

    @ApiModelProperty("修复建议内容")
    val content: String? = null
)
