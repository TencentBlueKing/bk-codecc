package com.tencent.bk.codecc.defect.pojo

data class OutputVO(
    var defects: List<Defect>
)

data class Defect(
    var filePath: String?,
    var checkerName: String?,
    val line: Int,
    val description: String
)
