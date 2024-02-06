package com.tencent.bk.codecc.task.pojo

data class TriggerPipelineModel(
    val projectId: String,
    val pipelineId: String,
    val taskId: Long,
    val gongfengId: Int,
    val owner: String,
    val commitId: String? = null,
    val codeccBuildId: String? = null,
    val toolName: String? = null,
    /**
     * 迁移过度标识，标识本次触发使用闭源构建集群
     */
    val migrate: Boolean? = null,
    /**
     * 传递给构建集群分发的额外参数
     */
    val dispatchExtraInfo: Map<String, Any>? = null,
    /**
     * 传递给流水连运行的额外参数
     */
    val runtimeParam: List<CodeCCRuntimeParam>? = null,
)
