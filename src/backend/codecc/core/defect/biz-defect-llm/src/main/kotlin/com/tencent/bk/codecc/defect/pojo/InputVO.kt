package com.tencent.bk.codecc.defect.pojo

data class InputVO(
    val logLevel: String?,
    val scanPath: String,
    val language: Long,
    val whitePathList: List<String>,
    val scanType: String,
    val skipPaths: List<String>,
    val incrementalFiles: List<String>,
    val openCheckers: List<Checker>,
    val llmAPI: LLMApi,
)

data class Checker(
    val checkerName: String,
    val severity: Int,
    val checkerOptions: List<CheckerOption>
)

data class CheckerOption(
    val checkerOptionName: String,
    val checkerOptionValue: String
)

data class LLMApi(
    val name: String,
    val apiKey: String,
)
