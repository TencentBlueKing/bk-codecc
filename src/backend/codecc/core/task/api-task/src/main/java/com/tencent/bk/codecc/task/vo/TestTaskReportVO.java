package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 测试报告前端返回视图
 *
 * @date 2024/05/11
 */
@Data
@ApiModel("测试任务报告前端返回视图")
public class TestTaskReportVO {
    @ApiModelProperty(value = "项目名")
    private String projectName;
    @ApiModelProperty(value = "项目id")
    private String projectId;
    @ApiModelProperty(value = "任务名")
    private String nameCn;
    private Long taskId;
    @ApiModelProperty(value = "代码库")
    private String repo;
    @ApiModelProperty(value = "状态")
    private Integer status;
    @ApiModelProperty(value = "当前版本任务耗时")
    private Long currentCostTime;
    @ApiModelProperty(value = "对比版本任务耗时")
    private Long comparisonCostTime;
    @ApiModelProperty(value = "耗时变化, 当前版本 - 对比版本")
    private Long diffCostTime;
    @ApiModelProperty(value = "当前版本问题数")
    private Integer currentDefectCount;
    @ApiModelProperty(value = "对比版本问题数")
    private Integer comparisonDefectCount;
    @ApiModelProperty(value = "问题数变化, 当前版本 - 对比版本")
    private Integer diffDefectCount;
    @ApiModelProperty(value = "执行时间")
    private Long executeDate;
}
