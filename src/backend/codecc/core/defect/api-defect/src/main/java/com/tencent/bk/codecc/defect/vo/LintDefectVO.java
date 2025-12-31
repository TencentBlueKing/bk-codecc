/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * lint类告警视图
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@Schema(description = "lint类告警视图")
public class LintDefectVO {
    private Long taskId;

    @Schema(description = "所属文件的主键id")
    private String entityId;

    @Schema(description = "告警ID")
    private String id;

    @Schema(description = "所属文件名")
    private String fileName;

    @Schema(description = "告警行号")
    private int lineNum;

    @Schema(description = "告警作者")
    private List<String> author;

    @Schema(description = "告警规则")
    private String checker;

    @Schema(description = "告警规则名")
    private String checkerName;

    @Schema(description = "严重程度")
    private int severity;

    @Schema(description = "告警描述")
    private String message;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @Schema(description = "告警类型")
    private int defectType;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @Schema(description = "告警状态")
    private int status;

    /**
     * 告警行的变更时间，用于跟新旧告警的判断时间做对比
     */
    @Schema(description = "告警行的变更时间")
    private long lineUpdateTime;

    @Schema(description = "pinpoint的hash值")
    private String pinpointHash;

    @Schema(description = "文件的md5值")
    private String fileMd5;

    @Schema(description = "文件相对路径")
    private String relPath;

    @Schema(description = "文件全路径")
    private String filePath;

    @Schema(description = "告警规则详情")
    private String checkerDetail;

    @Schema(description = "告警创建时间")
    private Long createTime;

    @Schema(description = "告警修复时间")
    private Long fixedTime;

    @Schema(description = "告警忽略时间")
    private Long ignoreTime;

    @Schema(description = "告警忽略原因类型")
    private Integer ignoreReasonType;

    @Schema(description = "告警忽略原因")
    private String ignoreReason;

    @Schema(description = "告警忽略操作人")
    private String ignoreAuthor;

    @Schema(description = "告警屏蔽时间")
    private Long excludeTime;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @Schema(description = "告警是否被标记为已修改的标志")
    private Integer mark;

    @Schema(description = "告警被标记为已修改的时间")
    private Long markTime;

    @Schema(description = "标记了，但是再次扫描没有修复")
    private Boolean markButNoFixed;

    @Schema(description = "创建时的构建号")
    private String createBuildNumber;

    @Schema(description = "修复时的构建号")
    private String fixedBuildNumber;

    @Schema(description = "告警规则类型")
    private String checkerType;

    @Schema(description = "告警详情链接")
    private String defectDetailUrl;

    @Schema(description = "告警评论")
    private CodeCommentVO codeComment;

    @Schema(description = "是否有告警评论")
    private Boolean hasCodeComment;

    @Schema(description = "代码库路径")
    private String url;

    @Schema(description = "分支名称")
    private String branch;

    @Schema(description = "版本号")
    private String revision;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "是否注释忽略")
    private Boolean ignoreCommentDefect;

    @Schema(description = "所属任务名称")
    private String taskNameCn;

    @Schema(description = "提交人")
    private String commitAuthor;

    @Schema(description = "忽略审批ID")
    private String ignoreApprovalId;

    @Schema(description = "忽略审批状态 - 0 进行中  1 成功  2失败 - 没有审批记录时为空")
    private Integer ignoreApprovalStatus;

    @Schema(description = "忽略审批链接")
    private String ignoreApprovalUrl;

    @Schema(description = "忽略审批人类型")
    private List<String> ignoreApproverTypes;

    @Schema(description = "忽略审批 - 自定义审批人")
    private List<String> customIgnoreApprovers;
}
