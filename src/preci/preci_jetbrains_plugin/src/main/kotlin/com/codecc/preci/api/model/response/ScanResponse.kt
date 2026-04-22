package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 代码扫描启动响应
 *
 * @property message 扫描启动的提示消息
 * @property tools 本次扫描使用的检查工具列表（如 ["golangci-lint", "gosec"]）
 * @property scanFileNum 本次扫描涉及的文件数量
 *
 * @since 1.0
 */
@Serializable
data class ScanResponse(
    @SerialName("message")
    val message: String,
    
    @SerialName("tools")
    val tools: List<String>,
    
    @SerialName("scanFileNum")
    val scanFileNum: Int
)

