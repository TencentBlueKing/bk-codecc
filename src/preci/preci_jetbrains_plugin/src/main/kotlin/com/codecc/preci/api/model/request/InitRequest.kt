package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 项目初始化请求
 *
 * 用于初始化项目扫描配置，准备代码扫描所需的配置和环境
 *
 * @property currentPath 当前工作目录路径，用于自动推断项目根目录
 * @property rootPath 指定的项目根目录路径，如果不指定则自动推断
 *
 * @since 1.0
 */
@Serializable
data class InitRequest(
    @SerialName("currentPath")
    val currentPath: String,
    
    @SerialName("rootPath")
    val rootPath: String? = null
)

