package com.tencent.bk.codecc.defect.vo

import io.swagger.v3.oas.annotations.media.Schema
import lombok.Data

/**
 * 大语言模型响应数据对象
 *
 * @version V1.0
 * @date 2023/11/16
 */
@Data
@Schema(description = "大语言模型响应数据对象")
data class DefectSuggestionRespVO(
    @get:Schema(description = "大模型类型")
    var llmName: String? = null,

    @get:Schema(description = "修复建议内容")
    val content: String? = null
)
