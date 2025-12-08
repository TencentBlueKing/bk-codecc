package com.tencent.bk.codecc.defect.llm.api

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.llm.chat.LLMOpenAIChatApi
import com.tencent.bk.codecc.defect.llm.http.OpenAIHost
import com.tencent.bk.codecc.defect.llm.http.HttpClientConfig
import com.tencent.bk.codecc.defect.llm.http.HttpTransport
import com.tencent.bk.codecc.defect.llm.http.LoggingConfig
import com.tencent.bk.codecc.defect.llm.http.createHttpClient
import com.tencent.bk.codecc.defect.llm.log.LogLevel
import com.tencent.bk.codecc.defect.llm.log.Timeout
import com.tencent.bk.codecc.defect.pojo.serializable.BkChatCompletionChunk
import com.tencent.bk.codecc.defect.pojo.serializable.chatCompletionRequestV2
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

object LLMOpenAIApi {
    private val logger = LoggerFactory.getLogger(LLMOpenAIApi::class.java)

    /**
     * 非流式对话接口
     * @return String
     */
    fun getChatCompletion(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO, llmModel: String): String {
        val url = "${apiAuthVO.host}${apiAuthVO.path}"
        val requestBody = chatCompletionRequestV2{
            model = llmModel
            messages {
                // 非deepseek模型需要指定用户提示
                if (!llmModel.contains("deepseek", ignoreCase = true)) {
                    message {
                        role = LLMConstants.CHAT_ROLE_USER
                        content = apiChatVO.userChatPrompt
                    }
                    message {
                        role = LLMConstants.CHAT_ROLE_ASSISTANT
                        content = apiChatVO.assistantChatPrompt
                    }
                }
                message{
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.content
                }

            }
            stream = false
        }

        val client = OkHttpClient().newBuilder()
            .readTimeout(LLMConstants.TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "X-Bkapi-Authorization", JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), EMPTY_STRING)
            )
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), JsonUtil.toJson(requestBody)))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("API request failed with code: ${response.code}")
                    EMPTY_STRING
                }
                val responseBody = response.body!!.string()
                responseBody
            }
        } catch (e: Exception) {
            logger.error("trigger $url api error: ${e.message}")
            ""
        }
    }

    /**
     * 流式对话接口
     * @return Flow<String> 持续输出内容流
     */
    fun getChatCompletions(
        apiAuthVO: ApiAuthVO,
        apiChatVO: ApiChatVO,
        llmModel: String
    ): Flow<String> {
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
        val requestBody = chatCompletionRequestV2 {
            model = llmModel
            messages {
                // 非deepseek模型需要指定用户提示
                if (!llmModel.contains("deepseek", ignoreCase = true)) {
                    message {
                        role = LLMConstants.CHAT_ROLE_USER
                        content = apiChatVO.userChatPrompt
                    }
                    message {
                        role = LLMConstants.CHAT_ROLE_ASSISTANT
                        content = apiChatVO.assistantChatPrompt
                    }
                }
                message {
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.content
                }

            }
            stream = true
        }

        val llmConfig = HttpClientConfig(
            token = EMPTY_STRING,
            logging = LoggingConfig(logLevel = LogLevel.All),
            host = OpenAIHost(apiAuthVO.host.toString()),
            timeout = Timeout(socket = 1.minutes),
        )
        val httpTransport = HttpTransport(
            createHttpClient(llmConfig)
        )
        val chatApi = LLMOpenAIChatApi(httpTransport, apiAuthVO.path?: LLMConstants.LLM_GATEWAY_URL_APPSPACE)
        val authorStr = JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), EMPTY_STRING)

        return flow {
            chatApi.chatCompletions(requestBody, authorStr).buffer(Channel.UNLIMITED).collect { chunkRsp ->
                emit(json.encodeToString(BkChatCompletionChunk.serializer(), chunkRsp))
            }
        }
    }
}
