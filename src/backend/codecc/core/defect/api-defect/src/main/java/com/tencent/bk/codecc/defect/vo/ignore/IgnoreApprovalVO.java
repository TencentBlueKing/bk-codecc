package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "忽略审核实体")
public class IgnoreApprovalVO extends CommonVO {

    /**
     * 审核配置ID
     */
    @Schema(description = "审核配置ID")
    private String approvalConfigId;

    /**
     * 问题维度
     */
    @Schema(description = "问题维度")
    private List<String> dimensions;

    /**
     * 问题级别
     */
    @Schema(description = "问题级别")
    private List<Integer> severities;

    /**
     * 忽略类型
     */
    @Schema(description = "忽略类型")
    private List<Integer> ignoreTypeIds;

    /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    private String projectId;


    /**
     * 任务ID列表
     */
    @Schema(description = "任务ID列表")
    private List<Long> taskIds;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @Schema(description = "审批人")
    private String approverType;

    /**
     * 审批人列表 - 真实审批人列表
     */
    @Schema(description = "审批人列表 - 真实审批人列表")
    private List<String> approvers;


    /**
     * 审批单号
     */
    @Schema(description = "审批单号")
    private String itsmSn;


    /**
     * 审批详情URL
     */
    @Schema(description = "审批详情URL")
    private String itsmUrl;

    /**
     * 审批状态
     */
    @Schema(description = "审批状态")
    private Integer status;

}
