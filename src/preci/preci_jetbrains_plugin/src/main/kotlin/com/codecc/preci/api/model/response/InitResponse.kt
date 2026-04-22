package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 项目初始化响应
 *
 * @property rootPath 初始化成功后确定的项目根目录路径
 * @property tools 需要下载/重载的工具名称列表，后续需逐个调用 `/task/reload/tool/{toolName}` 完成下载
 *
 * @since 1.0
 */
@Serializable
data class InitResponse(
    @SerialName("rootPath")
    val rootPath: String,

    @SerialName("tools")
    val tools: List<String> = emptyList(),

    @SerialName("hasUpdate")
    val hasUpdate: Boolean = false,

    @SerialName("currentVersion")
    val currentVersion: String = "",

    @SerialName("latestVersion")
    val latestVersion: String = ""
)

