package com.tencent.bk.codecc.task.pojo

class CodeCCCertificateAuthInfo(
        val certificateId: String,
        override val privateToken: String? = null,
        override val commitId: String? = null,
        override val tag: String? = null
) : CodeCCAuthInfo("certificate", privateToken, commitId, tag)