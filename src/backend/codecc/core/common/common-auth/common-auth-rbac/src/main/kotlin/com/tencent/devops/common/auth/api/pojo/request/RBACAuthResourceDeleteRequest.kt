package com.tencent.devops.common.auth.api.pojo.request

data class RBACAuthResourceDeleteRequest(
        val resourceCode: String,
        val resourceType: String
)
