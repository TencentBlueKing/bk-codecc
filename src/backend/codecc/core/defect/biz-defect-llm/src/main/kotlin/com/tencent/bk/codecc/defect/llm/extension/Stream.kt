package com.tencent.bk.codecc.defect.llm.extension

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.pojo.serializable.BkChatCompletionChunk
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.ComConstants
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import kotlin.text.String

private const val STREAM_PREFIX = "data:"
private const val STREAM_END_TOKEN = "$STREAM_PREFIX [DONE]"
const val CHUNK_SIZE = 1024 * 64

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
    val chunk = CHUNK_SIZE
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
    val logger = LoggerFactory.getLogger("Stream")
    val jsonLenient = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    val channel: ByteReadChannel = response.body()
    // 记录前一个消息的ID,用于填充结束消息块
    var lastId: String? = null
    try {
        while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
            var remain = ""
            val lineResult = withTimeoutOrNull(LLMConstants.TIMEOUT_SECONDS * 1000L) {
                readUTF8Line(channel, remain)
            } ?: break
            val lines = lineResult.first
            remain = lineResult.second
            lines?.forEach { line ->
                when {
                    line.startsWith(STREAM_END_TOKEN) -> {
                        emit(
                            BkChatCompletionChunk(
                                id = lastId ?: "",
                                choices = listOf(
                                    BkChatCompletionChunk.Choice(
                                        finishReason = "stop"
                                    )
                                )
                            ) as T
                        )
                        return
                    }

                    line.startsWith(STREAM_PREFIX) -> {
                        val jsonContent = line.removePrefix(STREAM_PREFIX)
                        // 先解析为通用JSON元素来获取id
                        val element = jsonLenient.parseToJsonElement(jsonContent)
                        // 安全地尝试获取id字段
                        element.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.let {
                            lastId = it
                        }
                        // 转换为目标类型
                        val value = jsonLenient.decodeFromJsonElement<T>(element)
                        emit(value)
                    }
                }
            }
        }
    } catch (e: Exception) {
        throw CodeCCException("streamEventsFromForOpenAI failed :${e.message}")
    } finally {
        channel.cancel()
        logger.info("channel is closed for read：${channel.isClosedForRead}, " +
                "currentCoroutineContext().isActive:${currentCoroutineContext().isActive}")
    }
}

internal suspend fun readUTF8Line(channel: ByteReadChannel, preRemain: String): Pair<List<String>?, String> {
    val buffers = mutableListOf<ByteArray>()
    val chunkSize = CHUNK_SIZE
    val accumulatedData = StringBuilder(preRemain)

    while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
        val buffer = ByteArray(chunkSize)
        val bytesRead = channel.readAvailable(buffer)
        val trimmed = buffer.dropLastWhile { it == 0.toByte() }.toByteArray()
        buffers.add(trimmed)
        if (bytesRead < 1024) {
            continue
        }
        val totalSize = buffers.sumOf { it.size }
        val combinedBytes = ByteArray(totalSize)
        var currentIndex = 0
        buffers.forEach { arr ->
            System.arraycopy(arr, 0, combinedBytes, currentIndex, arr.size)
            currentIndex += arr.size
        }
        // 转换为UTF-8字符串（注意处理不完整字符）
        val decodedStr = String(combinedBytes, Charsets.UTF_8)
        buffers.clear()
        // 添加到当前总字符串中
        accumulatedData.append(decodedStr)
        // 没有换行符，继续
        if (!decodedStr.contains('\n')) {
            continue
        }
        // 查找所有换行符
        val lines = accumulatedData.split('\n')
        val completeLines = lines.dropLast(1).map { it + '\n' }
        val remain = lines.last().toString()
        return Pair(completeLines, remain)
    }

    // 通道关闭时返回剩余数据（如果有）
    return if (accumulatedData.isNotEmpty()) {
        Pair(listOf(accumulatedData.toString()), ComConstants.EMPTY_STRING)
    } else {
        Pair(null, ComConstants.EMPTY_STRING)
    }
}
