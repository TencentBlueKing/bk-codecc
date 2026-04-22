package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 触发流水线构建请求
 *
 * `POST /pipeline/build/start` 接口的请求体。
 *
 * @property rootPath 项目根目录路径
 *
 * @since 1.0
 */
@Serializable
data class StartBuildRequest(
    @SerialName("rootPath")
    val rootPath: String
)
