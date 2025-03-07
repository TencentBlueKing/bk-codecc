package com.tencent.bk.codecc.openapi.v2.vo

import com.tencent.bk.codecc.openapi.v2.constant.ProjectEventType

/**
 * 项目事件通知
 */
data class ProjectCallbackEvent(
    val event: ProjectEventType,
    val data: ProjectCallbackEventData
)
