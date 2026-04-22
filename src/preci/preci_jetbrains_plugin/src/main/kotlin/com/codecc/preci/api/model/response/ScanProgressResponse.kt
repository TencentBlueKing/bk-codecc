package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 扫描进度查询响应
 *
 * @property projectRoot 当前扫描的项目根目录
 * @property toolStatuses 各检查工具的状态映射，key 为工具名，value 为状态（running/done）
 * @property status 整体扫描状态：running-扫描进行中，done-扫描已完成，空字符串-无扫描任务
 *
 * @since 1.0
 */
@Serializable
data class ScanProgressResponse(
    @SerialName("projectRoot")
    val projectRoot: String,
    
    @SerialName("toolStatuses")
    val toolStatuses: Map<String, String>,
    
    @SerialName("status")
    val status: String
)

