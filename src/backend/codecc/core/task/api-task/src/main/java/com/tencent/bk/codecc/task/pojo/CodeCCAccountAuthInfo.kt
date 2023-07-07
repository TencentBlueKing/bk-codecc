package com.tencent.bk.codecc.task.pojo

data class CodeCCAccountAuthInfo(
    val userName: String,
    val passWord: String,
    override val privateToken: String? = null,
    override val commitId: String? = null,
    override val tag: String? = null
) : CodeCCAuthInfo("account", privateToken, commitId, tag)
