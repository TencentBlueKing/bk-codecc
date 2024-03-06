package com.tencent.devops.common.auth.api.pojo.external.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 资源成员详情
 */
data class AuthRoleUserDetail(

    @JsonProperty("display_name")
    val displayName: String,

    @JsonProperty("role_id")
    val roleId: String,

    /**
     * 中英文匹配,enum： AuthRoleType
     */
    @JsonProperty("role_name")
    val roleName: String,

    @JsonProperty("user_id_list")
    val userIdList: List<String>,
)
