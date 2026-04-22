package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 远程缺陷信息
 *
 * CodeCC 平台上的缺陷信息，由 `POST /codecc/defect/list` 接口返回。
 * 与本地扫描的 [Defect] 不同，远程缺陷包含更多元数据（如责任人、状态等）。
 *
 * @property fileName 文件相对路径
 * @property filePath 文件绝对路径（基于请求中的 projectRoot 转换）
 * @property lineNum 问题所在行号
 * @property author 问题责任人列表
 * @property checker 检查规则名称
 * @property severity 严重级别：1=严重, 2=一般, 4=提示, 8=建议
 * @property message 问题描述信息
 * @property status 缺陷状态：1=待修复, 2=已修复, 4=已忽略
 * @property toolName 检查工具名称
 *
 * @since 1.0
 */
@Serializable
data class RemoteDefect(
    @SerialName("fileName")
    val fileName: String = "",

    @SerialName("filePath")
    val filePath: String = "",

    @SerialName("lineNum")
    val lineNum: Int = 0,

    @SerialName("author")
    val author: List<String>? = null,

    @SerialName("checker")
    val checker: String = "",

    @SerialName("severity")
    val severity: Int = 4,

    @SerialName("message")
    val message: String = "",

    @SerialName("status")
    val status: Int = 1,

    @SerialName("toolName")
    val toolName: String = ""
) {
    /**
     * 获取严重程度的中文描述
     *
     * @return 严重程度中文文本
     */
    fun getSeverityText(): String {
        return when (severity) {
            1 -> "严重"
            2 -> "一般"
            4 -> "提示"
            8 -> "建议"
            else -> "未知"
        }
    }

    /**
     * 获取状态的中文描述
     *
     * @return 状态中文文本
     */
    fun getStatusText(): String {
        return when (status) {
            1 -> "待修复"
            2 -> "已修复"
            4 -> "已忽略"
            else -> "未知"
        }
    }

    /**
     * 获取责任人列表的展示文本
     *
     * @return 以逗号分隔的责任人名称
     */
    fun getAuthorText(): String {
        return author?.joinToString(", ") ?: ""
    }

    /**
     * 转换为本地缺陷模型 [Defect]
     *
     * 将远程缺陷转换为本地缺陷格式，以便复用本地扫描结果的展示逻辑。
     *
     * @return 本地缺陷模型
     */
    fun toLocalDefect(): Defect {
        return Defect(
            toolName = toolName,
            checkerName = checker,
            description = message,
            filePath = filePath,
            line = lineNum,
            severity = severity.toLong()
        )
    }
}

/**
 * 远程缺陷列表响应
 *
 * `POST /codecc/defect/list` 接口的响应体，包含缺陷列表和统计信息。
 *
 * @property seriousCount 严重级别缺陷数量
 * @property normalCount 一般级别缺陷数量
 * @property promptCount 提示级别缺陷数量
 * @property totalCount 缺陷总数
 * @property existCount 待修复（存量）缺陷数量
 * @property fixCount 已修复缺陷数量
 * @property ignoreCount 已忽略缺陷数量
 * @property defects 缺陷列表
 *
 * @since 1.0
 */
@Serializable
data class RemoteDefectListResponse(
    @SerialName("seriousCount")
    val seriousCount: Int = 0,

    @SerialName("normalCount")
    val normalCount: Int = 0,

    @SerialName("promptCount")
    val promptCount: Int = 0,

    @SerialName("totalCount")
    val totalCount: Int = 0,

    @SerialName("existCount")
    val existCount: Int = 0,

    @SerialName("fixCount")
    val fixCount: Int = 0,

    @SerialName("ignoreCount")
    val ignoreCount: Int = 0,

    @SerialName("defects")
    val defects: List<RemoteDefect>? = null
) {
    /**
     * 获取缺陷列表（null 安全）
     *
     * @return 缺陷列表，若服务器返回 null 则返回空列表
     */
    fun getDefectList(): List<RemoteDefect> = defects ?: emptyList()

    /**
     * 获取统计摘要文本
     *
     * @return 格式化的统计摘要，如 "共 18 个问题（严重: 5, 一般: 10, 提示: 3）"
     */
    fun getSummaryText(): String {
        return "共 $totalCount 个问题（严重: $seriousCount, 一般: $normalCount, 提示: $promptCount）"
    }
}
