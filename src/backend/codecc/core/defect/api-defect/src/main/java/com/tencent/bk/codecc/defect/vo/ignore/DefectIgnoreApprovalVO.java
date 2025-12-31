package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息传递，告警忽略")
public class DefectIgnoreApprovalVO {

    /**
     * 审核ID
     */
    @Schema(description = "审核ID")
    private String approvalId;

    /**
     * 审核ID
     */
    @Schema(description = "审核ID")
    private String defectMatchId;

    /**
     * 审核配置ID
     */
    @Schema(description = "审核配置ID")
    private String approvalConfigId;

    /**
     * 忽略类型
     */
    @Schema(description = "忽略类型")
    private Integer ignoreTypeId;

    /**
     * 忽略类型名称
     */
    @Schema(description = "忽略类型名称")
    private String ignoreTypeName;

    /**
     * 忽略原因
     */
    @Schema(description = "忽略原因")
    private String ignoreReason;

    /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    private String projectId;

    /**
     * 忽略人
     */
    @Schema(description = "忽略人")
    private String ignoreAuthor;


    /**
     * 任务ID列表
     */
    @Schema(description = "任务ID列表")
    private Set<Long> taskIds;

    /**
     * 告警数量
     */
    @Schema(description = "告警数量")
    private Long defectCount;

    /**
     * 是否为重试提交
     */
    @Schema(description = "是否为重试提交")
    private Boolean retryCommit;
}
