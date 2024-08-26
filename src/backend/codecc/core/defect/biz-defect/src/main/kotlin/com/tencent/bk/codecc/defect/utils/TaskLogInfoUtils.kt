package com.tencent.bk.codecc.defect.utils

import com.tencent.bk.codecc.defect.model.TaskLogEntity

object TaskLogInfoUtils {

    private const val FAST_INC_SCAN_EN = "FAST INCREMENT SCAN"
    private const val FAST_INC_SCAN_ZH = "超快增量扫描"

    fun isFastIncScan(taskLog: TaskLogEntity): Boolean {
        val stepArrays = taskLog.stepArray
        // 过滤掉包含“超快增量”的TaskLog
        return if (stepArrays.isNullOrEmpty()) {
            true
        } else {
            stepArrays.none { step -> FAST_INC_SCAN_EN == step.msg || FAST_INC_SCAN_ZH == step.msg }
        }
    }
}
