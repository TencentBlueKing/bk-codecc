package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建议阈值前端视图
 *
 * @date 2024/04/29
 */
@Data
@ApiModel("建议阈值前端视图")
@AllArgsConstructor
@NoArgsConstructor
public class RecommendedThresholdVO {

    @ApiModelProperty("已执行任务数 (>=)")
    Integer taskNum;
    @ApiModelProperty("执行成功率 (>=) (单位: %)")
    Double successRate;
    @ApiModelProperty("执行耗时均值 (<=) (单位: s/kloc)")
    Double averCostTime;
    @ApiModelProperty("扫出问题密度 (>)")
    Double defectDensity;

}
