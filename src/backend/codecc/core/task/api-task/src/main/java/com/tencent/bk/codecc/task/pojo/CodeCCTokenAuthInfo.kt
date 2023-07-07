package com.tencent.bk.codecc.task.pojo

class CodeCCTokenAuthInfo(
    val accessToken: String,
    override val privateToken: String? = null,
    override val commitId: String? = null,
    override val tag: String? = null
) : CodeCCAuthInfo("token", privateToken, commitId, tag)
