package com.tencent.bk.codecc.defect.llm.extension

import kotlinx.serialization.json.*

val jsonLenient = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

inline fun <reified T> streamRequestOf(serializable: T): JsonElement {
    val enableStream = "stream" to JsonPrimitive(true)
    val json = jsonLenient.encodeToJsonElement(serializable)
    val map = json.jsonObject.toMutableMap().also { it += enableStream }
    return JsonObject(map)
}
