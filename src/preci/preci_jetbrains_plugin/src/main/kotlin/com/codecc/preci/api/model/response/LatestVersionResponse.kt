package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 获取最新版本响应
 *
 * @property latestVersion 线上最新版本号（如 v2.1.0）
 *
 * @since 1.0
 */
@Serializable
data class LatestVersionResponse(
    @SerialName("latestVersion")
    val latestVersion: String
)

