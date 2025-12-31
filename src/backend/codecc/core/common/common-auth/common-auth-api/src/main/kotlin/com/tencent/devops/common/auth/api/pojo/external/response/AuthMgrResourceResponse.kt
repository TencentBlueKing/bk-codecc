package com.tencent.devops.common.auth.api.pojo.external.response;

import io.swagger.v3.oas.annotations.media.Schema
data class AuthMgrResourceResponse(
    val policy: List<AuthTaskPolicy>,
    val role: List<AuthTaskRole>
)