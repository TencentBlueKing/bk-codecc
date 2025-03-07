package com.tencent.bk.codecc.defect.pojo

data class FileFilterConditionsVO(
    val suffixList: List<String>,
    val skipPathList: List<String>,
    val whitePathList: List<String>
)
