package com.tencent.bk.codecc.defect.vo.ignore;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "忽略审核配置实体")
public class IgnoreApprovalConfigVO extends CommonVO {

    /**
     * 名字
     */
    @Schema(description = "名字")
    private String name;

    /**
     * 问题维度
     */
    @Schema(description = "问题维度")
    private List<String> dimensions;

    /**
     * 问题创建时间
     */
    @Schema(description = "问题创建时间")
    private Long defectCreateTime;

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
     * 项目范围类型, SINGLE, 其他范围类型
     */
    @Schema(description = "项目范围类型")
    private String projectScopeType;


    /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    private String projectId;

    /**
     * 限制项目ID
     */
    @Schema(description = "限制项目ID")
    private List<String> limitedProjectIds;

    /**
     * 任务范围类型, ALL,INCLUDE,EXCLUDE
     */
    @Schema(description = "任务范围类型")
    private String taskScopeType;

    /**
     * 任务范围INCLUDE,EXCLUDE下，选择的任务列表
     */
    @Schema(description = "选择的任务列表")
    private List<Long> taskScopeList;

    /**
     * 审批人,
     * 项目管理员
     * 任务管理员
     * 规则发布者
     * 忽略人LEADER
     * 自定义
     */
    @Schema(description = "审批人类型")
    private List<String> approverTypes;

    /**
     * 自定义审批人列表
     */
    @Schema(description = "自定义审批人列表")
    private List<String> customApprovers;

    /**
     * 是否可以编辑
     */
    @Schema(description = "是否可以编辑")
    private Boolean edit;

    /**
     * 项目
     * ONLY_PROJECT_MANAGER
     * UNIFIED_CONFIG
     */
    @Schema(description = "不允许编辑原因")
    private String disableEditReason;

}
