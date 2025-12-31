package com.tencent.bk.codecc.defect.pojo.serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Creates a completion for the chat message.
 */
@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
class ChatCompletionRequestV2(
    @SerialName("model") var model: String? = null,
    @SerialName("messages") val messages: List<SerializableChat>,
    @SerialName("stream") var stream: Boolean = true,
    @SerialName("temperature") public val temperature: Double? = null,
    @SerialName("n") public val n: Int? = null,
    @SerialName("max_tokens") public val maxTokens: Int? = null,
    @SerialName("user") public val user: String? = null
)

/**
 * The messages to generate chat completions for.
 */
fun chatCompletionRequestV2(block: ChatCompletionRequestBuilderV2.() -> Unit): ChatCompletionRequestV2 =
    ChatCompletionRequestBuilderV2().apply(block).build()

/**
 * Creates a completion for the chat message.
 */
@LLMDsl
class ChatCompletionRequestBuilderV2 {
    var model: String? = null
    var messages: List<SerializableChat>? = null
    var stream: Boolean = false
    var temperature: Double? = null
    var n: Int? = null
    var maxTokens: Int? = null
    var user: String? = null

    fun messages(block: ChatMessagesBuilderV2.() -> Unit) {
        messages = ChatMessagesBuilderV2().apply(block).messages
    }

    /**
     * Builder of [ChatCompletionRequestV2] instances.
     */
    fun build(): ChatCompletionRequestV2 = ChatCompletionRequestV2(
        model = requireNotNull(model) { "model is required" },
        messages = requireNotNull(messages) { "messages is required" },
        stream = stream,
        temperature = temperature,
        n = n,
        maxTokens = maxTokens,
        user = user
    )
}

/**
 * Creates a list of [message].
 */
@LLMDsl
class ChatMessagesBuilderV2 {
    internal val messages = mutableListOf<SerializableChat>()

    fun message(block: SerializableChatBuilder.() -> Unit) {
        messages += SerializableChatBuilder().apply(block).build()
    }
}
