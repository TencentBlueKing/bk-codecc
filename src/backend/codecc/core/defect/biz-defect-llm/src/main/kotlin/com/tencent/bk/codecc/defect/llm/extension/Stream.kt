package com.tencent.bk.codecc.defect.llm.extension

import com.tencent.bk.codecc.defect.llm.http.JsonLenient
import io.ktor.client.call.*
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.text.String

private const val STREAM_PREFIX = "data: "
private const val STREAM_END_TOKEN = "$STREAM_PREFIX [DONE]"

/**
 * Get data as [Server-Sent
  * Events](https://developer.mozilla.org/en-US/docs/Web/API
  * /Server-sent_events/Using_server-sent_events#Event_stream_format).
 */
internal suspend inline fun <reified T> FlowCollector<T>.streamEventsFrom(response: HttpResponse) {

    val jsonLenient = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    val channel: ByteReadChannel = response.body()
    val chunk = 1024 * 64
    var appendResult = ""
    val buffers = mutableListOf<ByteArray>()
    while (!channel.isClosedForRead) {
        val buffer = ByteArray(chunk)
        val size = channel.readAvailable(buffer)
        val trimmed = buffer.dropLastWhile { it == 0.toByte() }.toByteArray()
        buffers.add(trimmed)
        if (size < 1024) {
            continue
        }
        val totalSize = buffers.sumOf { it.size }
        var combinedBuffer = ByteArray(totalSize)
        var currentIndex = 0
        buffers.forEach { arr ->
            System.arraycopy(arr, 0, combinedBuffer, currentIndex, arr.size)
            currentIndex += arr.size
        }
        appendResult = String(combinedBuffer, Charsets.ISO_8859_1).trimEnd('\u0000')
        buffers.clear()
        if (appendResult.contains("}{")) {
            val resultList = appendResult
                .replace("}{", "}#codecc#{")
                .split("#codecc#")
            resultList.forEachIndexed { index, result ->
                try {
                    val encodeToUtf8 = String(result.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                    jsonLenient.parseToJsonElement(encodeToUtf8)
                    emit(encodeToUtf8 as T)
                } catch (exception: Exception) {
                    buffers.add(result.toByteArray(Charsets.ISO_8859_1))
                }
            }
        } else {
            try {
                val encodeToUtf8 = String(appendResult.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                jsonLenient.parseToJsonElement(encodeToUtf8)
                emit(encodeToUtf8 as T)
            } catch (exception: Exception) {
                buffers.add(appendResult.toByteArray(Charsets.ISO_8859_1))
            }
        }
    }
}

internal suspend inline fun <reified T> FlowCollector<T>.streamEventsFromForOpenAI(response: HttpResponse) {
    val channel: ByteReadChannel = response.body()
    try {
        while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: continue
            val jsonContent = line.removePrefix(STREAM_PREFIX)
            when {
                line.startsWith(STREAM_END_TOKEN) -> break
                jsonContent == "[DONE]" -> break
                jsonContent.isBlank() -> continue
                else -> {
                    try {
                        val value = JsonLenient.decodeFromString<T>(jsonContent)
                        emit(value)
                    } catch (e: Exception) {
                        println("JSON解析失败: ${e.message}\n原始内容: $jsonContent")
                    }
                }
            }
        }
    } finally {
        channel.cancel()
    }
}
