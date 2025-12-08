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

import java.util.List;
import java.util.Set;

/**
 * lint类文件告警详情
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@Schema(description = "lint类文件告警详情")
public class LintFileVO
{
    @Schema(description = "任务id")
    private long taskId;

    @Schema(description = "工具名称")
    private String toolName;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件更新时间")
    private long fileUpdateTime;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
     */
    @Schema(description = "发现该告警的最近分析版本号")
    private String analysisVersion;

    /**
     * 状态：NEW(1), FIXED(2), PATH_MASK(8)
     */
    @Schema(description = "状态", allowableValues = "{1,2,8}")
    private int status;

    @Schema(description = "第一次检查出告警的时间")
    private long createTime;

    @Schema(description = "文件被修复的时间")
    private long fixedTime;

    @Schema(description = "告警被修复的时间")
    private long excludeTime;

    @Schema(description = "本文件的告警总数，方便用于统计")
    private int defectCount;

    @Schema(description = "本文件的新告警数，方便用于统计")
    private int newCount;

    @Schema(description = "本文件的历史告警数，方便用于统计")
    private int historyCount;

    @Schema(description = "代码库路径")
    private String url;

    @Schema(description = "代码仓库id")
    private String repoId;

    @Schema(description = "版本号")
    private String revision;

    @Schema(description = "分支名称")
    private String branch;

    @Schema(description = "相对路径")
    private String relPath;

    @Schema(description = "代码库子模块")
    private String subModule;

    @Schema(description = "作者清单")
    private Set<List<String>> authorList;

    @Schema(description = "规则清单")
    private Set<String> checkerList;

    @Schema(description = "严重程度列表")
    private Set<Integer> severityList;

    /**
     * 文件所有告警的严重程度之和，用于排序
     */
    private int severity;

    /**
     * 告警清单
     */
    @Schema(description = "告警清单")
    private List<LintDefectVO> defectList;


}
