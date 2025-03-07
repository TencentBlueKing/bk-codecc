package com.tencent.bk.codecc.defect.pojo.serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class SerializableExecuteKwargs(
    @SerialName("stream") val stream: Boolean = false,
    @SerialName("stream_timeout") val streamTimeOut: Int
)
