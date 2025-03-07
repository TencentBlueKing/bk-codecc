package com.tencent.bk.codecc.defect.pojo.serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BkChatCompletionChunk(
    @SerialName("id")
    val id: String? = null,

    @SerialName("object")
    val objectName: String? = null,

    @SerialName("created")
    val created: Long? = null,

    @SerialName("model")
    val model: String? = null,

    @SerialName("choices")
    val choices: List<Choice>? = null,

    @SerialName("usage")
    val usage: Usage? = null
) {
    @Serializable
    data class Choice(
        @SerialName("index")
        val index: Int? = null,

        @SerialName("delta")
        val delta: Delta? = null,

        @SerialName("finish_reason")
        val finishReason: String? = null
    )

    // 流式响应专用数据结构
    @Serializable
    data class Delta(
        @SerialName("role")
        val role: String? = null,

        @SerialName("content")
        val content: String? = null,

        @SerialName("reasoning_content")
        val reasoningContent: String? = null
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens")
        val promptTokens: Int? = null,

        @SerialName("completion_tokens")
        val completionTokens: Int? = null,

        @SerialName("total_tokens")
        val totalTokens: Int? = null
    )
}
