package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "消息传递，告警忽略")
public class DefectIgnoreApprovalVO {

    /**
     * 审核ID
     */
    @ApiModelProperty(value = "审核ID")
    private String approvalId;

    /**
     * 审核ID
     */
    @ApiModelProperty(value = "审核ID")
    private String defectMatchId;

    /**
     * 审核配置ID
     */
    @ApiModelProperty(value = "审核配置ID")
    private String approvalConfigId;

    /**
     * 忽略类型
     */
    @ApiModelProperty(value = "忽略类型")
    private Integer ignoreTypeId;

    /**
     * 忽略类型名称
     */
    @ApiModelProperty(value = "忽略类型名称")
    private String ignoreTypeName;

    /**
     * 忽略原因
     */
    @ApiModelProperty(value = "忽略原因")
    private String ignoreReason;

    /**
     * 项目ID
     */
    @ApiModelProperty(value = "项目ID")
    private String projectId;

    /**
     * 忽略人
     */
    @ApiModelProperty(value = "忽略人")
    private String ignoreAuthor;


    /**
     * 任务ID列表
     */
    @ApiModelProperty(value = "任务ID列表")
    private Set<Long> taskIds;

    /**
     * 告警数量
     */
    @ApiModelProperty(value = "告警数量")
    private Long defectCount;

    /**
     * 是否为重试提交
     */
    @ApiModelProperty(value = "是否为重试提交")
    private Boolean retryCommit;
}
