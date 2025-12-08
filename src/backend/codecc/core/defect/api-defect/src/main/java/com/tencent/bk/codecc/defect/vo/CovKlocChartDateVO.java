package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用于统计每一类缺陷状态的个数")
public class CovKlocChartDateVO
{
    @Schema(description = "日期")
    private String date;

    @Schema(description = "显示日期")
    private String tips;

    @Schema(description = "待修复告警趋势-待修复数量")
    private int unFixCount;

    @Schema(description = "每天新增-总数量")
    private int newCount;

    @Schema(description = "每天新增-待修复的数量[ newCount, reopen ]")
    private int existCount;

    @Schema(description = "每天关闭-总数量[ repairedCount, ignoreCount, excludedCount ]")
    private int closedCount;

    @Schema(description = "每天关闭-修复数量")
    private int repairedCount;

    @Schema(description = "每天关闭-忽略数量")
    private int ignoreCount;

    @Schema(description = "每天关闭-过滤屏蔽数量")
    private int excludedCount;

    public CovKlocChartDateVO() {
        this.repairedCount = 0;
        this.excludedCount = 0;
        this.ignoreCount = 0;
        this.newCount = 0;
        this.closedCount = 0;
        this.existCount = 0;
        this.unFixCount = 0;
    }

    public void increaseRepairedCount()
    {
        this.repairedCount++;
    }

    public void increaseExcludedCount()
    {
        this.excludedCount++;
    }

    public void increaseIgnoreCount()
    {
        this.ignoreCount++;
    }

    public void increaseNewCount()
    {
        this.newCount++;
    }

    public void increaseClosedCount()
    {
        this.closedCount++;
    }

    public void increaseExistCount()
    {
        this.existCount++;
    }

    public void increaseFixCount()
    {
        this.unFixCount++;
    }
}
