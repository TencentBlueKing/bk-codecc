package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 测试报告前端返回视图
 *
 * @date 2024/05/11
 */
@Data
@Schema(description = "测试任务报告前端返回视图")
public class TestTaskReportVO {
    @Schema(description = "项目名")
    private String projectName;
    @Schema(description = "项目id")
    private String projectId;
    @Schema(description = "任务名")
    private String nameCn;
    private Long taskId;
    @Schema(description = "代码库")
    private String repo;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "当前版本任务耗时")
    private Long currentCostTime;
    @Schema(description = "对比版本任务耗时")
    private Long comparisonCostTime;
    @Schema(description = "耗时变化, 当前版本 - 对比版本")
    private Long diffCostTime;
    @Schema(description = "当前版本问题数")
    private Integer currentDefectCount;
    @Schema(description = "对比版本问题数")
    private Integer comparisonDefectCount;
    @Schema(description = "问题数变化, 当前版本 - 对比版本")
    private Integer diffDefectCount;
    @Schema(description = "执行时间")
    private Long executeDate;
}
