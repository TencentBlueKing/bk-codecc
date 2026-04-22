package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 规则集选择响应
 *
 * @property projectRoot 项目根目录路径
 * @property checkerSets 成功选择的规则集 ID 列表
 *
 * @since 1.0
 */
@Serializable
data class CheckerSetSelectResponse(
    @SerialName("projectRoot")
    val projectRoot: String,
    
    @SerialName("checkerSets")
    val checkerSets: List<String>
)

