package com.tencent.bk.codecc.defect.pojo.serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class SerializableChat(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@LLMDsl
public class SerializableChatBuilder {

    /**
     * The role of the author of this message.
     */
    public var role: String? = null

    /**
     * The contents of the message.
     */
    public var content: String? = null

    /**
     * The name of the author of this message.
     * [name] is required if the role is `[ChatRole.Function],
     * and it should be the name of the function whose response is
     * in the [content]. It May contain a-z, A-Z, 0-9, and underscores, with a maximum length of 64 characters.
     */
    public var name: String? = null

    public fun build(): SerializableChat {
        return SerializableChat(
            role = role.toString(),
            content = content.toString()
        )
    }
    }
