package com.tencent.bk.codecc.defect.llm.api

import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import kotlinx.coroutines.flow.Flow

interface LLMApi {
    fun defectSuggestionApi(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): String

    fun defectSuggestionApiStream(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): Flow<String>
}
