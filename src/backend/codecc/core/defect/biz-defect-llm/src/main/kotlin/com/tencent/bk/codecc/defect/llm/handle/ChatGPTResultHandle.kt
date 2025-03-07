package com.tencent.bk.codecc.defect.llm.handle

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.pojo.BKAIDevOpenAIRespBodyVO
import com.tencent.devops.common.api.codecc.util.JsonUtil
import org.slf4j.LoggerFactory

object ChatGPTResultHandle : LLMResultHandle {
    private val logger = LoggerFactory.getLogger(ChatGPTResultHandle::class.java)

    override fun parseDefectSuggestionsResult(result: String): String {
        var content = ""
        var respBodyVO: BKAIDevOpenAIRespBodyVO? = null
        try {
            respBodyVO = JsonUtil.to(result, object : TypeReference<BKAIDevOpenAIRespBodyVO>() {})
        } catch (e: Exception) {
            logger.warn("the response can't json to object BKAIDevOpenAIRespBodyVO, $result")
            return ""
        }

        respBodyVO?.data?.first()?.result?.choices?.forEach { choice ->
            content = choice.message.content
        }
        return content
    }
}
