package com.tencent.bk.codecc.defect.llm.api

import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import kotlinx.coroutines.flow.Flow

object ChatGLM2Api : LLMApi {

    override fun defectSuggestionApi(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): String {
        TODO("Not yet implemented")
    }

    override fun defectSuggestionApiStream(apiAuthVO: ApiAuthVO, apiChatVO: ApiChatVO): Flow<String> {
        TODO("Not yet implemented")
    }
}
