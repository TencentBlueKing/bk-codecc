package com.tencent.bk.codecc.codeccjob.pojo

/**
 * CodeCC上报的消息内容
 *
 * @date 2022/5/26
 */
data class BkMetricsMessage(

    /**
     * 统计日期，格式yyyy-MM-dd
     */
    val statisticsTime: String,

    /**
     * 项目ID
     */
    val projectId: String,

    /**
     * 代码库扫描平均分，精确二位小数
     */
    val repoCodeccAvgScore: Double,

    /**
     * 已解决缺陷数量
     */
    val resolvedDefectNum: Int
)
