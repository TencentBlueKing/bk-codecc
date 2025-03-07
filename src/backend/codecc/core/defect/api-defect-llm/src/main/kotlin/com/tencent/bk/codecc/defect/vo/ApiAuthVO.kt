package com.tencent.bk.codecc.defect.vo

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiAuthVO(
    @JsonProperty("bk_app_code")
    val bkAppCode: String? = null,

    @JsonProperty("bk_app_secret")
    val bkAppSecret: String? = null,

    @JsonProperty("host")
    val host: String? = null,

    @JsonProperty("bk_ticket")
    val bkTicket: String? = null,

    @JsonProperty("apiKey")
    val apiKey: String? = null
)
