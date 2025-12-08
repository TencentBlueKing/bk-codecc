package com.tencent.bk.codecc.defect.vo

import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

/**
 * 大语言模型请求数据对象
 *
 * @version V1.0
 * @date 2023/11/16
 */
@Data
@Schema(description = "大语言模型请求数据对象")
data class DefectSuggestionReqVO(
    @get:Schema(description = "大模型类型")
    var llmName: String? = null,

    @get:Schema(description = "工具名称")
    val toolName: String? = null,

    @get:Schema(description = "规则名称")
    val checker: String? = null,

    @get:Schema(description = "规则描述")
    val message: String? = null,

    @get:Schema(description = "代码内容")
    val content: String? = null
)
