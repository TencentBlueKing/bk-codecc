package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IgnoreTypeDefectStatResponse {

    @Schema(description = "ignoreTypeId")
    private Integer ignoreTypeId;

    /**
     * 任务数量
     */
    @Schema(description = "taskCount")
    private Long taskCount;

    /**
     * 告警数量
     */
    @Schema(description = "defect")
    private Long defect;

    /**
     * 风险函数数量
     */
    @Schema(description = "riskFunction")
    private Long riskFunction;
}
