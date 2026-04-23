package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 代码缺陷信息
 *
 * @property toolName 检查工具名称（如 golangci-lint）
 * @property checkerName 检查规则名称（如 errcheck、gosec）
 * @property description 问题描述信息
 * @property filePath 问题所在文件的完整路径
 * @property line 问题所在行号
 * @property severity 严重程度：1=严重、2=一般、4=提示
 *
 * @since 1.0
 */
@Serializable
data class Defect(
    @SerialName("toolName")
    val toolName: String,

    @SerialName("checkerName")
    val checkerName: String,

    @SerialName("description")
    val description: String,

    @SerialName("filePath")
    val filePath: String,

    @SerialName("line")
    val line: Int,

    @SerialName("severity")
    val severity: Long = 4  // 默认为提示级别
) {
    /**
     * 获取严重程度的中文描述
     *
     * @return 严重程度的中文文本
     */
    fun getSeverityText(): String {
        return when (severity) {
            1L -> "严重"
            2L -> "一般"
            4L -> "提示"
            else -> "提示"
        }
    }
}

/**
 * 扫描结果查询响应
 *
 * @property defects 缺陷列表，如果服务器返回 null 则默认为空列表
 *
 * @since 1.0
 */
@Serializable
data class ScanResultResponse(
    @SerialName("defects")
    val defects: List<Defect>? = null
) {
    /**
     * 获取缺陷列表
     *
     * 如果服务器返回的 defects 为 null，则返回空列表
     */
    fun getDefectList(): List<Defect> = defects ?: emptyList()
}

