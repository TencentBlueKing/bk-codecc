package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 规则集选择请求
 *
 * 用于选择要使用的代码检查规则集
 *
 * @property projectRootDir 项目根目录路径
 * @property checkerSets 要选择的规则集 ID 列表，支持多选
 *
 * @since 1.0
 */
@Serializable
data class CheckerSetSelectRequest(
    @SerialName("projectRootDir")
    val projectRootDir: String? = null,
    
    @SerialName("checkerSets")
    val checkerSets: List<String>
)

