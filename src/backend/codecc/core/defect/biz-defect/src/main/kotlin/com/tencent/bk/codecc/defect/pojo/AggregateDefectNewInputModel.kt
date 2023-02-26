package com.tencent.bk.codecc.defect.pojo

data class AggregateDefectNewInputModel<out T>(
    val filePathSet: Set<String>?,
    val relPathSet : Set<String>?,
    val filterPaths: Set<String>?,
    val whitePaths: Set<String>?,
    val defectList: List<T>
)
