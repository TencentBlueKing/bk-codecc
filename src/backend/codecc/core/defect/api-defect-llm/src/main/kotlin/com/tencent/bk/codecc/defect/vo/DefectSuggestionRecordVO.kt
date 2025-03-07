package com.tencent.bk.codecc.defect.vo

import lombok.Data

@Data
data class DefectSuggestionRecordVO(

    /**
     * 工具扫描开始时间
     */
    var createdAfterDate: Long? = null,

    /**
     * 告警主Id
     */
    var defectId: String? = null,

    /**
     * AI模型类型
     */
    val llmName: String? = null,

    /**
     * 项目Id
     */
    val projectId: String? = null,

    /**
     * 任务Id
     */
    val taskId: Long? = null,

    /**
     * 用户名
     */
    val userId: String? = null,

    /**
     * 流响应
     */
    val stream: Boolean? = null,

    /**
     * 修复建议内容
     */
    val content: String? = null
)
