package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authType", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeCCAccountAuthInfo::class, name = "account"),
    JsonSubTypes.Type(value = CodeCCTokenAuthInfo::class, name = "token"),
    JsonSubTypes.Type(value = CodeCCCertificateAuthInfo::class, name = "certificate")
)
abstract class CodeCCAuthInfo(
    open val authType: String,
    open val privateToken: String?,
    open val commitId: String?,
    open val tag: String?
)
