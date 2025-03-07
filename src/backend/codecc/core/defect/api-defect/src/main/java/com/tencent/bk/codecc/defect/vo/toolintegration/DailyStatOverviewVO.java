package com.tencent.bk.codecc.defect.vo.toolintegration;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("工具集成-按天统计概览")
public class DailyStatOverviewVO extends DailyTrendChartVO {

    @ApiModelProperty("创建时间均是统计归属日期当天date的告警数")
    private long newIssueCount;

    @ApiModelProperty("当天总耗时（注意单位：秒）用于计算平均耗时，单位为：秒/千行代码 = 总耗时 /（总代码行数 / 1000）")
    private long elapseTime;

    @ApiModelProperty("总代码行")
    private long codeLine;

    @ApiModelProperty("工具误报原因忽略的问题数，误报率 = 工具误报原因忽略的问题数 /（待修复+已修复+忽略问题数）")
    private long misreportIgnoreCount;

    @ApiModelProperty("待确认误报数")
    private long unconfirmedCount;
}
