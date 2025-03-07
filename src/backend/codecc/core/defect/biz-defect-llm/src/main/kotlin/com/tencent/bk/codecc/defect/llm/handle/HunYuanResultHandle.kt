package com.tencent.bk.codecc.defect.llm.handle

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.pojo.HunYuanRespBodyVO
import com.tencent.devops.common.api.codecc.util.JsonUtil
import org.slf4j.LoggerFactory

object HunYuanResultHandle : LLMResultHandle {
    private val logger = LoggerFactory.getLogger(HunYuanResultHandle::class.java)

    override fun parseDefectSuggestionsResult(result: String): String {
        var content = ""
        var respBodyVO: HunYuanRespBodyVO? = null
        try {
            respBodyVO = JsonUtil.to(result, object : TypeReference<HunYuanRespBodyVO>() {})
        } catch (e: Exception) {
            logger.warn("the response can't json to object HunYuanRespBodyVO, $result")
            return ""
        }
        respBodyVO.choices.forEach { choice ->
            content = choice.message.content
        }
        return content
    }
}
