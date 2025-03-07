package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 查询测试报告的请求视图
 *
 * @date 2024/05/11
 */
@Data
@ApiModel("查询测试报告的请求视图")
public class QueryTestReportReqVO {
    @NotBlank(message = "当前版本号不能为空")
    @ApiModelProperty(value = "当前版本号", required = true)
    String currentVersion;
    @NotBlank(message = "对比版本号不能为空")
    @ApiModelProperty(value = "对比版本号", required = true)
    String comparisonVersion;
    @ApiModelProperty(value = "任务状态, 1-成功, 2-失败, 4-进行中, 多个状态取异或")
    Integer taskStatus;
    @ApiModelProperty(value = "耗时变化, 1-上升, 2-下降, 多个状态取异或")
    Integer costTimeDiff;
    @ApiModelProperty(value = "问题数变化, 1-上升, 2-下降, 多个状态取异或")
    Integer defectCountDiff;
    @ApiModelProperty(value = "开始时间(戳)")
    Long startTime;
    @ApiModelProperty(value = "结束时间(戳)")
    Long endTime;
}
