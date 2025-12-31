package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.OrgInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "BG安全审批人实体")
public class BgSecurityApprovalVO extends CommonVO {


    /**
     * 项目范围类型, SINGLE, 多项目范围类型
     */
    @Schema(description = "项目范围类型")
    private String projectScopeType;


    /**
     * 组织信息
     */
    @Schema(description = "组织信息")
    private OrgInfoVO orgInfo;

    /**
     * 审批人列表
     */
    @Schema(description = "审批人列表")
    private List<String> approvers;
}


