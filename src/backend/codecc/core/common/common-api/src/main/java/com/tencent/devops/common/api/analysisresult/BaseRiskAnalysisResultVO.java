package com.tencent.devops.common.api.analysisresult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风险等级类分析基类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseRiskAnalysisResultVO extends BaseLastAnalysisResultVO {
    /**
     * 新增极高风险告警数
     */
    @Schema(description = "新增极高风险告警数")
    private int newSuperHighCount;

    /**
     * 新增高风险告警数
     */
    @Schema(description = "新增高风险告警数")
    private int newHighCount;

    /**
     * 新增中风险告警数
     */
    @Schema(description = "新增中风险告警数")
    private int newMediumCount;


    /**
     * 所有极高风险告警数
     */
    @Schema(description = "所有极高风险告警数")
    private Integer superHighCount;

    /**
     * 所有高风险告警数
     */
    @Schema(description = "所有高风险告警数")
    private Integer highCount;

    /**
     * 所有中高风险告警数
     */
    @Schema(description = "所有中高风险告警数")
    private Integer mediumCount;
}
