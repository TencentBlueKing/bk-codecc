package com.tencent.bk.codecc.defect.llm.api

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.pojo.Input
import com.tencent.bk.codecc.defect.pojo.OpenAIData
import com.tencent.bk.codecc.defect.pojo.Chat
import com.tencent.bk.codecc.defect.pojo.OpenAIConfig
import com.tencent.bk.codecc.defect.pojo.BKAIDevOpenAIReqBodyVO
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.devops.common.codecc.util.JsonUtil
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object ChatGPTApi : LLMApi {

    private val logger = LoggerFactory.getLogger(ChatGPTApi::class.java)

    override fun defectSuggestionApi(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): String {
        val url = "${com.tencent.bk.codecc.defect.constant.LLMConstants.OPENAI_API_URL}"
        val bkAIDevOpenAIReqBodyVO = BKAIDevOpenAIReqBodyVO(
            data = OpenAIData(
                inputs = listOf<Input>(
                    Input(
                        messages = listOf<Chat>(
                            Chat(
                                role = com.tencent.bk.codecc.defect.constant.LLMConstants.CHAT_ROLE_USER,
                                content = apiChatVO.userChatPrompt
                            ),
                            Chat(
                                role = com.tencent.bk.codecc.defect.constant.LLMConstants.CHAT_ROLE_ASSISTANT,
                                content = apiChatVO.assistantChatPrompt
                            ),
                            Chat(
                                role = com.tencent.bk.codecc.defect.constant.LLMConstants.CHAT_ROLE_USER,
                                content = apiChatVO.content
                            )
                        ), model = com.tencent.bk.codecc.defect.constant.LLMConstants.OPENAI_MODEL
                    )
                )
            ), config = OpenAIConfig(
                action = "create"
            )
        )
        logger.debug("bkAIDevOpenAIReqBodyVO: ${JsonUtil.toJson(bkAIDevOpenAIReqBodyVO)}")

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Bkapi-Authorization", JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), ""))
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), JsonUtil.toJson(bkAIDevOpenAIReqBodyVO)))
            .build()

        val client = OkHttpClient().newBuilder().readTimeout(LLMConstants.TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS).build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseContent = response.body!!.string()
                logger.debug("responseContent: $responseContent")
                responseContent
            }
        } catch (e: Exception) {
            logger.error("trigger $url api error: ${e.message}")
            ""
        }
    }

    override fun defectSuggestionApiStream(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): Flow<String> {
        TODO("Not yet implemented")
    }
}
