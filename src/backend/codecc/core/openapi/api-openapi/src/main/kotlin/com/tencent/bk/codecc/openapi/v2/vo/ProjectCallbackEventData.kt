package com.tencent.bk.codecc.openapi.v2.vo

/**
 * 项目事件通知
 */
data class ProjectCallbackEventData(
    val userId: String? = null,
    val projectId: String,
    val projectEnglishName: String? = null,
    val enabled: Boolean? = null
)
