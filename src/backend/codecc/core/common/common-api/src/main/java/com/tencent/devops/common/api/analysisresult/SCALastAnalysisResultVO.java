package com.tencent.devops.common.api.analysisresult;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SCA类工具最近一次分析结果
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("SCA类工具最近一次分析结果")
public class SCALastAnalysisResultVO extends BaseLastAnalysisResultVO {

    @ApiModelProperty("组件数量")
    private Long packageCount;

    @ApiModelProperty("漏洞数量")
    private Long newVulCount;

    @ApiModelProperty("高风险漏洞数量")
    private Long newHighVulCount;

    @ApiModelProperty("中风险漏洞数量")
    private Long newMediumVulCount;

    @ApiModelProperty("低风险漏洞数量")
    private Long newLowVulCount;

    @ApiModelProperty("证书数量")
    private Long licenseCount;
}
