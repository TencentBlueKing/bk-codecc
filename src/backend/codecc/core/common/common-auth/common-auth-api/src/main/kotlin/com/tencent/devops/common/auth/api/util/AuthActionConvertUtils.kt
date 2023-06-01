package com.tencent.devops.common.auth.api.util

import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction

object AuthActionConvertUtils {


    fun covert(actions: List<CodeCCAuthAction>): List<PipelineAuthAction> {
        if (actions.isEmpty()) {
            return emptyList()
        }
        val pipelineActions = mutableSetOf<PipelineAuthAction>()
        actions.forEach {
            pipelineActions.addAll(covert(it))
        }
        return pipelineActions.toList()
    }

    private fun covert(action: CodeCCAuthAction?): List<PipelineAuthAction> {
        if (action == null) {
            return emptyList()
        }
        return when (action) {
            CodeCCAuthAction.TASK_MANAGE ->
                listOf<PipelineAuthAction>(
                    PipelineAuthAction.EDIT
                )
            CodeCCAuthAction.ANALYZE ->
                listOf<PipelineAuthAction>(
                    PipelineAuthAction.EXECUTE
                )
            CodeCCAuthAction.DEFECT_MANAGE ->
                listOf<PipelineAuthAction>(
                    PipelineAuthAction.EXECUTE
                )
            else ->
                listOf<PipelineAuthAction>(
                    PipelineAuthAction.VIEW
                )
        }
    }

}