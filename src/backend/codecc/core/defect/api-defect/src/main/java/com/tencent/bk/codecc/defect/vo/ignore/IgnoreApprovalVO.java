package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "忽略审核实体")
public class IgnoreApprovalVO extends CommonVO {

    /**
     * 审核配置ID
     */
    @ApiModelProperty(value = "审核配置ID")
    private String approvalConfigId;

    /**
     * 问题维度
     */
    @ApiModelProperty(value = "问题维度")
    private List<String> dimensions;

    /**
     * 问题级别
     */
    @ApiModelProperty(value = "问题级别")
    private List<Integer> severities;

    /**
     * 忽略类型
     */
    @ApiModelProperty(value = "忽略类型")
    private List<Integer> ignoreTypeIds;

    /**
     * 项目ID
     */
    @ApiModelProperty(value = "项目ID")
    private String projectId;


    /**
     * 任务ID列表
     */
    @ApiModelProperty(value = "任务ID列表")
    private List<Long> taskIds;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @ApiModelProperty(value = "审批人")
    private String approverType;

    /**
     * 审批人列表 - 真实审批人列表
     */
    @ApiModelProperty(value = "审批人列表 - 真实审批人列表")
    private List<String> approvers;


    /**
     * 审批单号
     */
    @ApiModelProperty(value = "审批单号")
    private String itsmSn;


    /**
     * 审批详情URL
     */
    @ApiModelProperty(value = "审批详情URL")
    private String itsmUrl;

    /**
     * 审批状态
     */
    @ApiModelProperty(value = "审批状态")
    private Integer status;

}
