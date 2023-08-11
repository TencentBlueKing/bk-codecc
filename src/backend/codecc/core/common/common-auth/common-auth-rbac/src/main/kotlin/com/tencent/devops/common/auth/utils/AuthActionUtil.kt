package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.auth.api.pojo.TaskAuthAction
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction

object AuthActionUtil {

    /**
     * 旧版、新版操作id映射
     */
    private val ActionMapping = mapOf(
        CodeCCAuthAction.ANALYZE.actionName to listOf(TaskAuthAction.ANALYZE.actionId),
        CodeCCAuthAction.DEFECT_MANAGE.actionName to listOf(TaskAuthAction.MANAGE_DEFECT.actionId),
        CodeCCAuthAction.DEFECT_VIEW.actionName to listOf(TaskAuthAction.VIEW_DEFECT.actionId),
        CodeCCAuthAction.REPORT_VIEW.actionName to listOf(TaskAuthAction.VIEW_REPORT.actionId),
        CodeCCAuthAction.TASK_MANAGE.actionName to
            listOf(TaskAuthAction.SETTING.actionId, TaskAuthAction.MANAGE.actionId)
    )

    /**
     * 获取权限操作id
     */
    fun getRbacAction(codeCCAuthActionName: String): List<String> {
        return ActionMapping[codeCCAuthActionName] ?: listOf(codeCCAuthActionName)
    }

    /**
     * 新版pipeline操作id映射
     */
    private val pipelineActionMapping = mapOf(
        PipelineAuthAction.DELETE.actionName to "pipeline_${PipelineAuthAction.DELETE.actionName}",
        PipelineAuthAction.DOWNLOAD.actionName to "pipeline_${PipelineAuthAction.DOWNLOAD.actionName}",
        PipelineAuthAction.EDIT.actionName to "pipeline_${PipelineAuthAction.EDIT.actionName}",
        PipelineAuthAction.EXECUTE.actionName to "pipeline_${PipelineAuthAction.EXECUTE.actionName}",
        PipelineAuthAction.LIST.actionName to "pipeline_${PipelineAuthAction.LIST.actionName}",
        PipelineAuthAction.SHARE.actionName to "pipeline_${PipelineAuthAction.SHARE.actionName}",
        PipelineAuthAction.VIEW.actionName to "pipeline_${PipelineAuthAction.VIEW.actionName}"
    )

    /**
     * 获取流水线新版操作id
     */
    fun getPipelineAction(pipelineAuthActionName: String): String {
        return pipelineActionMapping[pipelineAuthActionName] ?: pipelineAuthActionName
    }
}
