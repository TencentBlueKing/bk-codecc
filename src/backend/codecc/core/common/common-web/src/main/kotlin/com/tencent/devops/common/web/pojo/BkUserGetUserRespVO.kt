package com.tencent.devops.common.web.pojo

data class BkUserGetUserRespVO(
    val bk_username: String? = null,
    val display_name: String? = null,
    val tenant_id: String?,
    val time_zone: String?,
    val language: String?,
    val status: String?
)
