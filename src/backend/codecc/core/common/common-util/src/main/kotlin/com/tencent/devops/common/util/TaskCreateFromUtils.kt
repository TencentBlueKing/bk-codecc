package com.tencent.devops.common.util

import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom

object TaskCreateFromUtils {

    /**
     * 区分GongfengScan来源
     */
    fun getGongfengScanTaskRealCreateFrom(projectId: String?): BsTaskCreateFrom {
        return if (!projectId.isNullOrBlank() && projectId.startsWith(ComConstants.CUSTOMPROJ_ID_PREFIX)) {
            BsTaskCreateFrom.API_TRIGGER
        } else {
            BsTaskCreateFrom.GONGFENG_SCAN
        }
    }

    fun getTaskRealCreateFrom(projectId: String?, taskCreateFrom: String?): BsTaskCreateFrom {
        if (taskCreateFrom.isNullOrBlank() || BsTaskCreateFrom.getByValue(taskCreateFrom) == null) {
            return BsTaskCreateFrom.BS_PIPELINE
        }
        return getTaskRealCreateFrom(projectId, BsTaskCreateFrom.getByValue(taskCreateFrom))
    }

    fun getTaskRealCreateFrom(projectId: String?, taskCreateFrom: BsTaskCreateFrom): BsTaskCreateFrom {
        if (taskCreateFrom != BsTaskCreateFrom.GONGFENG_SCAN) {
            return taskCreateFrom
        }
        return getGongfengScanTaskRealCreateFrom(projectId)
    }
}
