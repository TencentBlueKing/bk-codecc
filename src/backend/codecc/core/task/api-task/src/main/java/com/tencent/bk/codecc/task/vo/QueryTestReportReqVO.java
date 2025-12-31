package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 查询测试报告的请求视图
 *
 * @date 2024/05/11
 */
@Data
@Schema(description = "查询测试报告的请求视图")
public class QueryTestReportReqVO {
    @NotBlank(message = "当前版本号不能为空")
    @Schema(description = "当前版本号", required = true)
    String currentVersion;
    @NotBlank(message = "对比版本号不能为空")
    @Schema(description = "对比版本号", required = true)
    String comparisonVersion;
    @Schema(description = "任务状态, 1-成功, 2-失败, 4-进行中, 多个状态取异或")
    Integer taskStatus;
    @Schema(description = "耗时变化, 1-上升, 2-下降, 多个状态取异或")
    Integer costTimeDiff;
    @Schema(description = "问题数变化, 1-上升, 2-下降, 多个状态取异或")
    Integer defectCountDiff;
    @Schema(description = "开始时间(戳)")
    Long startTime;
    @Schema(description = "结束时间(戳)")
    Long endTime;
}
