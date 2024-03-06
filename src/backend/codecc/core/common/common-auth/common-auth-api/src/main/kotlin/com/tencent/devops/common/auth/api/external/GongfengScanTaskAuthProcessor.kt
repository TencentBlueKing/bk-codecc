package com.tencent.devops.common.auth.api.external

import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import javax.ws.rs.container.ContainerRequestContext

interface GongfengScanTaskAuthProcessor {
    fun isPassAuth(requestContext: ContainerRequestContext, actions: List<CodeCCAuthAction>): Boolean
}