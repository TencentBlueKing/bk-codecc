package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IgnoreTypeDefectStatResponse {

    @ApiModelProperty("ignoreTypeId")
    private Integer ignoreTypeId;

    /**
     * 任务数量
     */
    @ApiModelProperty("taskCount")
    private Long taskCount;

    /**
     * 告警数量
     */
    @ApiModelProperty("defect")
    private Long defect;

    /**
     * 风险函数数量
     */
    @ApiModelProperty("riskFunction")
    private Long riskFunction;
}
