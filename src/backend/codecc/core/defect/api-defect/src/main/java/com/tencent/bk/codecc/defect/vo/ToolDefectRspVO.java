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

import com.tencent.bk.codecc.defect.vo.openapi.DefectDetailExtVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 工具告警响应视图
 *
 * @version V1.0
 * @date 2019/11/25
 */

@Data
@Schema(description = "工具告警响应视图")
public class ToolDefectRspVO
{
    @Schema(description = "任务主键")
    private long taskId;

    @Schema(description = "任务英文名")
    private String nameEn;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "严重规则数")
    private Integer seriousCheckerCount;

    @Schema(description = "正常规则数")
    private Integer normalCheckerCount;

    @Schema(description = "提示规则数")
    private Integer promptCheckerCount;

    @Schema(description = "新增文件数")
    private Integer newDefectCount;

    @Schema(description = "历史文件数")
    private Integer historyDefectCount;

    @Schema(description = "规则总数")
    private Integer totalCheckerCount;

    @Schema(description = "首次分析时间")
    private Long firstAnalysisSuccessTime;

    @Schema(description = "lint类告警清单")
    private Page<LintDefectVO> lintDefectList;

    @Deprecated
    @Schema(description = "lint类文件清单")
    private Page<LintFileVO> lintFileList;

    @Schema(description = "Cov类文件清单")
    private Page<DefectDetailExtVO> defectList;

    /**
     * 风险系数极高的个数
     */
    @Schema(description = "风险系数极高的个数")
    private Integer superHighCount;

    @Schema(description = "风险系数高的个数")
    private Integer highCount;

    @Schema(description = "风险系数中的个数")
    private Integer mediumCount;

    @Schema(description = "风险系数低的个数")
    private Integer lowCount;

    @Schema(description = "告警总数")
    private Integer totalCount;

    @Schema(description = "缺陷列表")
    private Page<CCNDefectVO> ccnDefectList;

    @Schema(description = "重复率缺陷列表")
    private Page<DUPCDefectVO> dupcDefectList;

    @Schema(description = "任务详情Map")
    private Map<Long, TaskDetailVO> taskDetailVoMap;
}
