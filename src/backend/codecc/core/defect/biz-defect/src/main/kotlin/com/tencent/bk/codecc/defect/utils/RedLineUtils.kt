package com.tencent.bk.codecc.defect.utils

import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.constant.ComConstants

object RedLineUtils {

    fun checkIfTaskEnableRedLine(task: TaskDetailVO): Boolean {
        return if (!ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(task.createFrom)) {
            return task.projectId.startsWith(ComConstants.GITHUB_PROJECT_PREFIX)
        } else {
            true
        }
    }
}
