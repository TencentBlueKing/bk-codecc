/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.model.statistic;

import com.tencent.bk.codecc.defect.model.DUPCNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.DUPCScanSummaryEntity;
import com.tencent.bk.codecc.defect.model.DupcChartTrendEntity;
import com.tencent.bk.codecc.defect.model.statistic.StatisticEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import org.springframework.data.mongodb.core.mapping.Sharded;

/**
 * 每次分析结束的统计数据
 *
 * @version V2.4
 * @date 2017/10/28
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_dupc_statistic")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}", background = true),
        @CompoundIndex(name = "task_id_1_build_id_1", def = "{'task_id': 1, 'build_id': 1}", background = true),
        @CompoundIndex(name = "task_id_1_tool_name_1_time_1", def = "{'task_id': 1, 'tool_name': 1, 'time': 1}",
                background = true)
})
@Sharded(shardKey = "task_id")
public class DUPCStatisticEntity extends StatisticEntity {
    /**
     * 本次分析前的遗留告警数
     */
    @Field("last_defect_count")
    private Integer lastDefectCount;

    /**
     * 本次分析的代码重复率
     */
    @Field("dup_rate")
    private Float dupRate;

    /**
     * 本次分析前的代码重复率
     */
    @Field("last_dup_rate")
    private Float lastDupRate;

    /**
     * 本次分析的代码重复率变化值
     */
    @Field("dup_rate_change")
    private Float dupRateChange;

    /**
     * 代码重复率工具的扫描统计结果
     */
    @Field("dupc_scan_summary")
    private DUPCScanSummaryEntity dupcScanSummary;

    /**
     * 极高风险文件数量
     */
    @Field("super_high_count")
    private Integer superHighCount;

    /**
     * 高风险文件数量
     */
    @Field("high_count")
    private Integer highCount;

    /**
     * 中风险文件数量
     */
    @Field("medium_count")
    private Integer mediumCount;

    /**
     * 平均圈复杂度趋势
     */
    @Field("dupc_chart")
    private List<DupcChartTrendEntity> dupcChart;

    /**
     * 新增极高风险告警数
     */
    @Field("new_super_high_count")
    private int newSuperHighCount;

    /**
     * 新增高风险告警数
     */
    @Field("new_high_count")
    private int newHighCount;

    /**
     * 新增中风险告警数
     */
    @Field("new_medium_count")
    private int newMediumCount;

    /**
     * 新告警处理人统计
     */
    @Field("new_author_statistic")
    private List<DUPCNotRepairedAuthorEntity> newAuthorStatistic;

    /**
     * 存量告警处理人统计
     */
    @Field("exist_author_statistic")
    private List<DUPCNotRepairedAuthorEntity> existAuthorStatistic;
}
