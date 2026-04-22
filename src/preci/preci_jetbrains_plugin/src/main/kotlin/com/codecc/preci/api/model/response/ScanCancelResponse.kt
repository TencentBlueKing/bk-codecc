package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 取消扫描响应
 *
 * @property projectRoot 被取消扫描任务的项目根目录
 *
 * @since 1.0
 */
@Serializable
data class ScanCancelResponse(
    @SerialName("projectRoot")
    val projectRoot: String
)

