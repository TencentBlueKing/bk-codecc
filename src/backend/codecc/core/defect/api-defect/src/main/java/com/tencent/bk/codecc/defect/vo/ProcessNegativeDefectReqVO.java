package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 误报告警的处理请求结构体
 *
 * @date 2024/01/22
 */
@Data
public class ProcessNegativeDefectReqVO {

    @Schema(description = "更新误报告警处理进展")
    private Integer processProgress;

    @Schema(description = "误报的处理原因类型")
    private Integer processReasonType;

    @Schema(description = "误报的处理原因")
    private String processReason;

    @Schema(description = "TAPD/Github Issue链接")
    private String issueLink;
}
