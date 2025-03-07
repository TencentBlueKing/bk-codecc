package com.tencent.bk.codecc.defect.llm.api

import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.pojo.HunYuanReqBodyVO
import com.tencent.bk.codecc.defect.pojo.Chat
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.devops.common.codecc.util.JsonUtil
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object HunYuanApi : LLMApi {
    private val logger = LoggerFactory.getLogger(HunYuanApi::class.java)

    override fun defectSuggestionApi(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): String {

        val url = LLMConstants.HUNYUAN_API_URL
        var hunYuanBodyVO = HunYuanReqBodyVO(
            model = LLMConstants.HUNYUAN_MODEL,
            stream = false,
            messages = listOf<Chat>(
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

        logger.debug("hunYuanBodyVO: ${JsonUtil.toJson(hunYuanBodyVO)}")

        val client = OkHttpClient().newBuilder().readTimeout(LLMConstants.TIMEOUT_SECONDS.toLong(),
            TimeUnit.SECONDS).build()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${apiAuthVO.apiKey}")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), JsonUtil.toJson(hunYuanBodyVO)))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val responseContent = response.body!!.string()
                logger.debug("responseContent: $responseContent")
                return responseContent
            }
        } catch (e: Exception) {
            logger.error("trigger $url api error: ${e.message}")
            return ""
        }
    }

    override fun defectSuggestionApiStream(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): Flow<String> {
        TODO("Not yet implemented")
    }
}
