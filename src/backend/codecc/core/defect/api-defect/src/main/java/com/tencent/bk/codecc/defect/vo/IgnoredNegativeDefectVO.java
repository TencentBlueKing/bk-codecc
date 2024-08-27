package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 以误报为理由忽略的告警的展示视图
 *
 * @date 2024/01/16
 */
@Data
public class IgnoredNegativeDefectVO {

    @ApiModelProperty(value = "误报告警 Id")
    private String entityId;

    @ApiModelProperty(value = "该误报告警所对应的 LintDefectV2Entity 的 id")
    private String defectId;

    @ApiModelProperty(value = "任务名")
    private String taskNameCn;

    @ApiModelProperty(value = "项目")
    private String projectName;

    @ApiModelProperty(value = "代码库 url")
    private String url;

    @ApiModelProperty(value = "文件链接")
    private String fileLink;

    @ApiModelProperty(value = "告警行号")
    private int lineNum;

    @ApiModelProperty(value = "告警所在文件路径")
    private String filePath;

    @ApiModelProperty(value = "告警所在文件名")
    private String fileName;

    @ApiModelProperty(value = "规则名")
    private String checker;

    @ApiModelProperty(value = "规则发布者")
    private String publisher;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    @ApiModelProperty(value = "级别")
    private Integer severity;

    @ApiModelProperty("告警忽略人")
    private String ignoreAuthor;

    @ApiModelProperty("告警忽略时间")
    private Long ignoreTime;

    @ApiModelProperty("告警忽略原因类型")
    private Integer ignoreReasonType;

    @ApiModelProperty("告警忽略原因")
    private String ignoreReason;

    @ApiModelProperty("处理进展")
    private Integer processProgress;

    @ApiModelProperty("误报的处理原因类型")
    private Integer processReasonType;

    @ApiModelProperty("误报的处理原因")
    private String processReason;

    @ApiModelProperty("规则描述")
    private String message;

    @ApiModelProperty("TAPD/Github Issue链接")
    private String issueLink;
}
