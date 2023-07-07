package com.tencent.bk.codecc.codeccjob.websocket

import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction

interface CustomizeWebSocketCheckProcessor {

    fun matchCheckCondition(taskCreateFrom: String) : Boolean

    fun check(user: String, taskId: String, projectId: String, actions: List<CodeCCAuthAction>) : Boolean
}