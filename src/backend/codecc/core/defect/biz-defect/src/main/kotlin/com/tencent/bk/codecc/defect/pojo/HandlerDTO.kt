package com.tencent.bk.codecc.defect.pojo

import com.tencent.devops.common.constant.ComConstants

data class HandlerDTO(
    var taskId: Long = 0L,
    var toolName: String = "",
    val pattern: String,
    var buildId: String = "",
    var isFullScan: Boolean = false,
    var scanStatus: ComConstants.ScanStatus
) {
    constructor(pattern: String) : this(
        0L,
        "",
        pattern,
        "",
        false,
        ComConstants.ScanStatus.SUCCESS
    )
}
