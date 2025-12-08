package com.tencent.bk.codecc.defect.vo.toolintegration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工具集成-按天统计概览")
public class DailyStatOverviewVO extends DailyTrendChartVO {

    @Schema(description = "创建时间均是统计归属日期当天date的告警数")
    private long newIssueCount;

    @Schema(description = "当天总耗时（注意单位：秒）用于计算平均耗时，单位为：秒/千行代码 = 总耗时 /（总代码行数 / 1000）")
    private long elapseTime;

    @Schema(description = "总代码行")
    private long codeLine;

    @Schema(description = "工具误报原因忽略的问题数，误报率 = 工具误报原因忽略的问题数 /（待修复+已修复+忽略问题数）")
    private long misreportIgnoreCount;

    @Schema(description = "待确认误报数")
    private long unconfirmedCount;

    @Schema(description = "当天新增这款工具的任务数")
    private long taskIdCount;
}
