package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 误报告警的处理请求结构体
 *
 * @date 2024/01/22
 */
@Data
public class ProcessNegativeDefectReqVO {

    @ApiModelProperty(value = "更新误报告警处理进展")
    private Integer processProgress;

    @ApiModelProperty(value = "误报的处理原因类型")
    private Integer processReasonType;

    @ApiModelProperty(value = "误报的处理原因")
    private String processReason;

    @ApiModelProperty(value = "TAPD/Github Issue链接")
    private String issueLink;
}
