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

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 圈复杂度缺陷视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "圈复杂度缺陷视图")
public class CCNDefectVO extends CommonVO {

    @Schema(description = "告警ID")
    private String id;

    /**
     * 签名，用于唯一认证
     */
    @Schema(description = "签名")
    private String funcSignature;
    /**
     * 任务id
     */
    @Schema(description = "任务id")
    private long taskId;

    /**
     * 方法名
     */
    @Schema(description = "方法名")
    private String functionName;

    /**
     * 方法的完整名称
     */
    @Schema(description = "方法的完整名称")
    private String longName;

    /**
     * 圈复杂度
     */
    @Schema(description = "圈复杂度")
    private int ccn;

    /**
     * 方法最后更新时间
     */
    @Schema(description = "方法最后更新时间")
    private Long latestDateTime;

    /**
     * 方法最后更新作者
     */
    @Schema(description = "方法最后更新作者")
    private String author;

    /**
     * 方法开始行号
     */
    @Schema(description = "方法开始行号")
    private Integer startLines;

    /**
     * 方法结束行号
     */
    @Schema(description = "方法结束行号")
    private Integer endLines;

    /**
     * 方法总行数
     */
    @Schema(description = "方法总行数")
    private Integer totalLines;

    /**
     * 包含圈复杂度计算节点的行号
     */
    @Schema(description = "包含圈复杂度计算节点的行号")
    private String conditionLines;

    /**
     * 告警方法的状态：new，closed，excluded，ignore
     */
    @Schema(description = "告警方法的状态：new，closed，excluded，ignore", allowableValues = "{new, closed, excluded, ignore}")
    private int status;

    /**
     * 风险系数，极高-1, 高-2，中-4，低-8
     * 该参数不入库，因为风险系数是可配置的
     */
    @Schema(description = "风险系数，极高-1, 高-2，中-4，低-8", allowableValues = "{1,2,4,8}")
    private int riskFactor;

    /**
     * 告警创建时间
     */
    @Schema(description = "告警创建时间")
    private Long createTime;

    /**
     * 告警修复时间
     */
    @Schema(description = "告警修复时间")
    private Long fixedTime;

    /**
     * 告警忽略时间
     */
    @Schema(description = "告警忽略时间")
    private Long ignoreTime;

    /**
     * 告警忽略原因类型
     */
    @Schema(description = "告警忽略原因类型")
    private Integer ignoreReasonType;

    /**
     * 告警忽略原因
     */
    @Schema(description = "告警忽略原因")
    private String ignoreReason;

    /**
     * 告警忽略操作人
     */
    @Schema(description = "告警忽略操作人")
    private String ignoreAuthor;

    /**
     * 告警屏蔽时间
     */
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

    /**
     * 文件相对路径
     */
    @Schema(description = "文件相对路径")
    private String relPath;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 代码仓库地址
     */
    @Schema(description = "代码仓库地址")
    private String url;

    /**
     * 仓库id
     */
    @Schema(description = "仓库id")
    private String repoId;

    /**
     * 文件版本号
     */
    @Schema(description = "文件版本号")
    private String revision;

    /**
     * 分支名
     */
    @Schema(description = "分支名")
    private String branch;

    /**
     * Git子模块
     */
    @Schema(description = "Git子模块")
    private String subModule;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
     */
    @Schema(description = "发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复")
    private String analysisVersion;


    /**
     * 告警创建构件号
     */
    @Schema(description = "告警创建构建号")
    private String createBuildNumber;

    /**
     * 修复时的构建号
     */
    @Schema(description = "告警修复构建号")
    private String fixedBuildNumber;

    /**
     * 告警评论
     */
    @Schema(description = "告警评论")
    private CodeCommentVO codeComment;

    /**
     * 是否注释忽略
     */
    @Schema(description = "是否注释忽略")
    private Boolean ignoreCommentDefect;

    @Schema(description = "所属任务名称")
    private String taskNameCn;

    @Schema(description = "提交人")
    private String commitAuthor;
}
