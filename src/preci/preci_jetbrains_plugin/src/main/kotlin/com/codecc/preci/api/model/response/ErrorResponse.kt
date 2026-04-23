package com.codecc.preci.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 统一错误响应
 *
 * 当 API 调用失败时（HTTP 4xx/5xx），服务端返回的错误信息
 *
 * **错误信息格式：** `[错误码] 错误消息`
 * 例如：`[100005] access token 无效, 请重新登录`
 *
 * @property error 错误信息描述（包含错误码和错误消息）
 *
 * @since 1.0
 */
@Serializable
data class ErrorResponse(
    @SerialName("error")
    val error: String
) {
    /**
     * 从错误信息中提取错误码
     *
     * 错误信息格式为 `[错误码] 错误消息`，例如 `[100005] access token 无效, 请重新登录`
     *
     * @return 错误码，如果无法解析则返回 null
     */
    fun extractErrorCode(): Int? {
        val regex = Regex("""\[(\d+)\]""")
        val matchResult = regex.find(error)
        return matchResult?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * 从错误信息中提取纯错误消息（不包含错误码）
     *
     * @return 纯错误消息
     */
    fun extractErrorMessage(): String {
        val regex = Regex("""\[\d+\]\s*(.*)""")
        val matchResult = regex.find(error)
        return matchResult?.groupValues?.get(1)?.trim() ?: error
    }
}

