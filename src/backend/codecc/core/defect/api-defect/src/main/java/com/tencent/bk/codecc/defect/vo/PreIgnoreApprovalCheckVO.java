package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@ApiModel("提前检查忽略审批返回对象")
@AllArgsConstructor
@NoArgsConstructor
public class PreIgnoreApprovalCheckVO {


    @ApiModelProperty("需要审批的告警总数")
    private Long count;

    @ApiModelProperty("示例告警列表")
    private List<LintDefectVO> defectList;
}
