package com.tencent.bk.codecc.defect.llm.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.llm.chat.LLMChat
import com.tencent.bk.codecc.defect.llm.chat.LLMChatApi
import com.tencent.bk.codecc.defect.llm.http.HttpClientConfig
import com.tencent.bk.codecc.defect.llm.http.HttpTransport
import com.tencent.bk.codecc.defect.llm.http.LoggingConfig
import com.tencent.bk.codecc.defect.llm.http.createHttpClient
import com.tencent.bk.codecc.defect.llm.log.LogLevel
import com.tencent.bk.codecc.defect.llm.log.Timeout
import com.tencent.bk.codecc.defect.pojo.BKAIDevReqBodyVO
import com.tencent.bk.codecc.defect.pojo.BKAIDevRespBodyVO
import com.tencent.bk.codecc.defect.pojo.Chat
import com.tencent.bk.codecc.defect.pojo.serializable.SerializableChat
import com.tencent.bk.codecc.defect.pojo.serializable.SerializableExecuteKwargs
import com.tencent.bk.codecc.defect.pojo.serializable.chatCompletionRequest
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

/**
 * 蓝鲸BK AIOPs接口调用实现
 */
object BkAIDevApi : LLMApi {
    private val logger = LoggerFactory.getLogger(BkAIDevApi::class.java)
    var sessionId: String = EMPTY_STRING

    val llmConfig: HttpClientConfig = HttpClientConfig(
        token = EMPTY_STRING,
        logging = LoggingConfig(logLevel = LogLevel.All),
        timeout = Timeout(socket = 1.minutes),
    )

    fun transport(config: HttpClientConfig? = null): HttpTransport {
        return HttpTransport(
            createHttpClient(
                config ?: llmConfig
            )
        )
    }

    private fun createSession(apiAuthVO: ApiAuthVO): String {
        if (sessionId.isNotEmpty()) {
            return sessionId
        }
        sessionId = "s-" + UUID.randomUUID().toString().replace("-", EMPTY_STRING)
        val url = "${apiAuthVO.host}${LLMConstants.BKAIDEV_API_URL}"

        val bkAIDevReqBodyVO = BKAIDevReqBodyVO(
            sessionId = sessionId,
            sessionHistory = listOf<Chat>(),
            chatPrompts = listOf<Chat>(),
            appCollection = LLMConstants.BKAIDEV_APP_COLLECTION,
            llm = LLMConstants.BKAIDEV_LLM_MODEL
        )

        logger.debug("create session bkAIDevReqBodyVO: {}", bkAIDevReqBodyVO)

        val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader(
                    "X-Bkapi-Authorization", JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), EMPTY_STRING)
                )
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), JsonUtil.toJson(bkAIDevReqBodyVO)))
                .build()

        val client = OkHttpClient().newBuilder().readTimeout(
            LLMConstants.TIMEOUT_SECONDS.toLong(),
            TimeUnit.SECONDS
        ).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return EMPTY_STRING
                }
                val responseContent = response.body!!.string()
                val respBodyVO = try {
                    JsonUtil.to(responseContent, object : TypeReference<BKAIDevRespBodyVO>() {})
                } catch (e: Exception) {
                    logger.warn(
                        "the createSession url response can't json to object BKAIDevRespBodyVO, $responseContent"
                    )
                    null
                } ?: return EMPTY_STRING
                if (!respBodyVO.result && respBodyVO.message != "ok") {
                    logger.error("the createSession url response can't create session id right, $responseContent ")
                    return EMPTY_STRING
                }
                return respBodyVO.data!!.sessionId
            }
        } catch (e: Exception) {
            logger.error("trigger $url api error: ${e.message}")
            return EMPTY_STRING
        }
    }

    private fun chatSessionStream(
        apiAuthVO: ApiAuthVO,
        session: String,
        apiChatVO: ApiChatVO
    ): Flow<String> {

        val request = chatCompletionRequest {
            host = apiAuthVO.host
            sessionId = session
            stream = apiChatVO.stream
            sessionHistory = listOf<SerializableChat>()
            executeKwargs = SerializableExecuteKwargs(
                stream = apiChatVO.stream,
                streamTimeOut = LLMConstants.TIMEOUT_SECONDS
            )
            appCollection = LLMConstants.BKAIDEV_APP_COLLECTION
            llm = LLMConstants.BKAIDEV_LLM_MODEL
            chatPrompts {
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.userChatPrompt
                }
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_ASSISTANT
                    content = apiChatVO.assistantChatPrompt
                }
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.content
                }
            }
        }

        val http = transport()
        val chat: LLMChat = LLMChatApi(http)
        val authorStr = JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), EMPTY_STRING)

        return flow {
            chat.chatCompletions(request, authorStr).collect {
                it.split("\n").onEach { result ->
                    emit(result)
                }
            }
        }
    }

    private fun chatSession(
        apiAuthVO: ApiAuthVO,
        sessionId: String,
        apiChatVO: ApiChatVO
    ): String {
        val url = "${apiAuthVO.host}${LLMConstants.BKAIDEV_API_URL}$sessionId/"
        val bkAIDevReqBodyVO = BKAIDevReqBodyVO(
            sessionId = sessionId,
            stream = true,
            sessionHistory = listOf<Chat>(),
            appCollection = LLMConstants.BKAIDEV_APP_COLLECTION,
            llm = LLMConstants.BKAIDEV_LLM_MODEL,
            chatPrompts = listOf<Chat>(
                Chat(
                    role = LLMConstants.CHAT_ROLE_USER,
                    content = apiChatVO.userChatPrompt
                ),
                Chat(
                    role = LLMConstants.CHAT_ROLE_ASSISTANT,
                    content = apiChatVO.assistantChatPrompt
                ),
                Chat(
                    role = LLMConstants.CHAT_ROLE_USER,
                    content = apiChatVO.content
                )
            )
        )

        logger.debug("chat session bkAIDevReqBodyVO: ${JsonUtil.toJson(bkAIDevReqBodyVO)}")

        val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader(
                    "X-Bkapi-Authorization", JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), EMPTY_STRING)
                )
                .patch(RequestBody.create("application/json".toMediaTypeOrNull(), JsonUtil.toJson(bkAIDevReqBodyVO)))
                .build()

        val client = OkHttpClient().newBuilder().readTimeout(
            LLMConstants.TIMEOUT_SECONDS.toLong(),
            TimeUnit.SECONDS
        ).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseContent = response.body!!.string()
                    logger.debug("responseContent: $responseContent")
                    responseContent
                } else {
                    EMPTY_STRING
                }
            }
        } catch (e: Exception) {
            logger.error("trigger $url api error: ${e.message}")
            EMPTY_STRING
        }
    }

    override fun defectSuggestionApi(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): String {
        val sessionId = createSession(apiAuthVO)
        if (sessionId.isNullOrEmpty()) {
            logger.error("can't create the session id ")
            return EMPTY_STRING
        }
        return chatSession(apiAuthVO, sessionId, apiChatVO)
    }

    override fun defectSuggestionApiStream(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): Flow<String> {
        val sessionId = createSession(apiAuthVO)
        if (sessionId.isEmpty()) {
            logger.error("can't create the session id ")
        }

        return flow {
            chatSessionStream(apiAuthVO, sessionId, apiChatVO).collect {
                emit(it)
            }
        }
    }

}
