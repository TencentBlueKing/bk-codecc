package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "忽略审核配置实体")
public class IgnoreApprovalConfigVO extends CommonVO {

    /**
     * 名字
     */
    @ApiModelProperty(value = "名字")
    private String name;

    /**
     * 问题维度
     */
    @ApiModelProperty(value = "问题维度")
    private List<String> dimensions;

    /**
     * 问题创建时间
     */
    @ApiModelProperty(value = "问题创建时间")
    private Long defectCreateTime;

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
     * 项目范围类型, SINGLE, 其他范围类型
     */
    @ApiModelProperty(value = "项目范围类型")
    private String projectScopeType;


    /**
     * 项目ID
     */
    @ApiModelProperty(value = "项目ID")
    private String projectId;

    /**
     * 限制项目ID
     */
    @ApiModelProperty(value = "限制项目ID")
    private List<String> limitedProjectIds;

    /**
     * 任务范围类型, ALL,INCLUDE,EXCLUDE
     */
    @ApiModelProperty(value = "任务范围类型")
    private String taskScopeType;

    /**
     * 任务范围INCLUDE,EXCLUDE下，选择的任务列表
     */
    @ApiModelProperty(value = "选择的任务列表")
    private List<Long> taskScopeList;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @ApiModelProperty(value = "审批人类型")
    private String approverType;

    /**
     * 自定义审批人列表
     */
    @ApiModelProperty(value = "自定义审批人列表")
    private List<String> customApprovers;

    /**
     * 是否可以编辑
     */
    @ApiModelProperty(value = "是否可以编辑")
    private Boolean edit;

    /**
     * 项目
     * ONLY_PROJECT_MANAGER
     * UNIFIED_CONFIG
     */
    @ApiModelProperty(value = "不允许编辑原因")
    private String disableEditReason;

}
