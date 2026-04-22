package com.codecc.preci.api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 代码扫描请求
 *
 * 用于执行代码扫描，支持全量扫描、目标扫描和增量扫描
 *
 * @property scanType 扫描类型：0-全量扫描，100-目标扫描，102-pre-commit增量扫描，103-pre-push增量扫描
 * @property paths 目标扫描时指定的路径列表，多个路径用数组传递
 * @property rootDir 项目根目录路径
 *
 * @since 1.0
 */
@Serializable
data class ScanRequest(
    @SerialName("scanType")
    val scanType: Int,
    
    @SerialName("paths")
    val paths: List<String>? = null,
    
    @SerialName("rootDir")
    val rootDir: String? = null
)

