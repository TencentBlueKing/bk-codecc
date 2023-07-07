package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
// NOCC:TypeName(设计如此:)
public class DefectQueryReqVO_Old extends DefectQueryReqVOBase {
    @ApiModelProperty("工具名，数据迁移后支持多选，逗号分割多个")
    protected String toolName;

    @ApiModelProperty("维度，数据迁移后支持多选，逗号分割多个")
    protected String dimension;
}
