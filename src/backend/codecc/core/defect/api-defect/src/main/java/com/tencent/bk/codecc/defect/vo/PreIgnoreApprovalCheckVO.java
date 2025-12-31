package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "提前检查忽略审批返回对象")
@AllArgsConstructor
@NoArgsConstructor
public class PreIgnoreApprovalCheckVO {


    @Schema(description = "需要审批的告警总数")
    private Long count;

    @Schema(description = "示例告警列表")
    private List<LintDefectVO> defectList;
}
