package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 扫描结果查询请求
 *
 * 用于查询扫描结果（代码缺陷列表）
 *
 * @property path 查询的路径前缀，用于过滤结果（通常为项目根目录）
 *
 * @since 1.0
 */
@Serializable
data class ScanResultRequest(
    @SerialName("path")
    val path: String
)

