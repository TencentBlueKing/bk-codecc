package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Deprecated
@Data
@EqualsAndHashCode(callSuper = true)
// NOCC:TypeName(设计如此:)
public class DefectQueryReqVO_Old extends DefectQueryReqVOBase {
    @Schema(description = "工具名，数据迁移后支持多选，逗号分割多个")
    protected String toolName;

    @Schema(description = "维度，数据迁移后支持多选，逗号分割多个")
    protected String dimension;
}
