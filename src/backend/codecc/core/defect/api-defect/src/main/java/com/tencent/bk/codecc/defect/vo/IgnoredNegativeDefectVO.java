package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 以误报为理由忽略的告警的展示视图
 *
 * @date 2024/01/16
 */
@Data
public class IgnoredNegativeDefectVO {

    @Schema(description = "误报告警 Id")
    private String entityId;

    @Schema(description = "该误报告警所对应的 LintDefectV2Entity 的 id")
    private String defectId;

    @Schema(description = "任务名")
    private String taskNameCn;

    @Schema(description = "项目")
    private String projectName;

    @Schema(description = "代码库 url")
    private String url;

    @Schema(description = "文件链接")
    private String fileLink;

    @Schema(description = "告警行号")
    private int lineNum;

    @Schema(description = "告警所在文件路径")
    private String filePath;

    @Schema(description = "告警所在文件名")
    private String fileName;

    @Schema(description = "规则名")
    private String checker;

    @Schema(description = "规则发布者")
    private String publisher;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    @Schema(description = "级别")
    private Integer severity;

    @Schema(description = "告警忽略人")
    private String ignoreAuthor;

    @Schema(description = "告警忽略时间")
    private Long ignoreTime;

    @Schema(description = "告警忽略原因类型")
    private Integer ignoreReasonType;

    @Schema(description = "告警忽略原因")
    private String ignoreReason;

    @Schema(description = "处理进展")
    private Integer processProgress;

    @Schema(description = "误报的处理原因类型")
    private Integer processReasonType;

    @Schema(description = "误报的处理原因")
    private String processReason;

    @Schema(description = "规则描述")
    private String message;

    @Schema(description = "TAPD/Github Issue链接")
    private String issueLink;
}
