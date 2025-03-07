package com.tencent.bk.codecc.defect.pojo

data class BKAIDevOpenAIReqBodyVO(
    val data: OpenAIData,
    val config: OpenAIConfig
)

data class OpenAIData(
    val inputs: List<Input>
)

data class Input(
    val messages: List<Chat>,
    val model: String
)

data class OpenAIConfig(
    val action: String
)
