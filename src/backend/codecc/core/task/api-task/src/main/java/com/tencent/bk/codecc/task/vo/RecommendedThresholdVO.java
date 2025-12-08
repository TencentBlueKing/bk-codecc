package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建议阈值前端视图
 *
 * @date 2024/04/29
 */
@Data
@Schema(description = "建议阈值前端视图")
@AllArgsConstructor
@NoArgsConstructor
public class RecommendedThresholdVO {

    @Schema(description = "已执行任务数 (>=)")
    Integer taskNum;
    @Schema(description = "执行成功率 (>=) (单位: %)")
    Double successRate;
    @Schema(description = "执行耗时均值 (<=) (单位: s/kloc)")
    Double averCostTime;
    @Schema(description = "扫出问题密度 (>)")
    Double defectDensity;

}
