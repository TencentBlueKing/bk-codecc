package com.tencent.bk.codecc.defect.llm.handle

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.pojo.BKAIDevRespBodyVO
import com.tencent.devops.common.api.codecc.util.JsonUtil
import org.slf4j.LoggerFactory

object BkAIDevResultHandle : LLMResultHandle {
    private val logger = LoggerFactory.getLogger(BkAIDevResultHandle::class.java)

    override fun parseDefectSuggestionsResult(result: String): String {
        var content = ""
        var respBodyVO: BKAIDevRespBodyVO? = null
        try {
            respBodyVO = JsonUtil.to(result, object : TypeReference<BKAIDevRespBodyVO>() {})
        } catch (e: Exception) {
            logger.warn("the response can't json to object BKAIDevRespBodyVO, $result")
            return ""
        }
        respBodyVO?.data?.result?.choices?.forEach { choice ->
            content = choice.message.content
        }
        return content
    }
}
