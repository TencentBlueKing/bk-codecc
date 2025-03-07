package com.tencent.bk.codecc.defect.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import lombok.Data

/**
 * 大语言模型请求数据对象
 *
 * @version V1.0
 * @date 2023/11/16
 */
@Data
@ApiModel("大语言模型请求数据对象")
data class DefectSuggestionReqVO(
    @ApiModelProperty("大模型类型")
    var llmName: String? = null,

    @ApiModelProperty("工具名称")
    val toolName: String? = null,

    @ApiModelProperty("规则名称")
    val checker: String? = null,

    @ApiModelProperty("规则描述")
    val message: String? = null,

    @ApiModelProperty("代码内容")
    val content: String? = null
)
