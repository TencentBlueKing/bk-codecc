package com.tencent.bk.codecc.defect.vo

data class ApiChatVO(

    /**
     * LLM问答开始user角色定义
     */
    val userChatPrompt: String,

    /**
     * LLM问答开始assistant角色确定
     */
    val assistantChatPrompt: String,

    /**
     * LLM问答内容
     */
    var content: String,

    /**
     * 流模式响应
     */
    val stream: Boolean = false
)
