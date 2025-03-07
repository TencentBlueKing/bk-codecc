package com.tencent.bk.codecc.defect.llm.handle

interface LLMResultHandle {
    fun parseDefectSuggestionsResult(result: String): String
}
