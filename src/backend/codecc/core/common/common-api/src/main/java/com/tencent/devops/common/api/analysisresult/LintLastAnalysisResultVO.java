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

package com.tencent.devops.common.api.analysisresult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Lint类工具最近一次分析结果
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "lint类工具最近一次分析结果")
public class LintLastAnalysisResultVO extends BaseLastAnalysisResultVO
{
    @Schema(description = "文件总数")
    private Integer fileCount;

    @Schema(description = "文件变化数")
    private Integer fileChange;

    @Schema(description = "新告警总数")
    private Integer newDefectCount;

    @Schema(description = "历史告警总数")
    private Integer historyDefectCount;


    @Schema(description = "新增严重告警总数")
    private Integer totalNewSerious;

    @Schema(description = "新增一般告警总数")
    private Integer totalNewNormal;

    @Schema(description = "新增提示告警总数")
    private Integer totalNewPrompt;

    @Schema(description = "所有严重警告总数")
    private Integer totalSerious;

    @Schema(description = "所有一般警告总数")
    private Integer totalNormal;

    @Schema(description = "所有提示警告总数")
    private Integer totalPrompt;
    
    @Schema(description = "新增告警的处理人统计")
    private List<NotRepairedAuthorVO> authorStatistic;
    
    @Schema(description = "存量告警的处理人统计")
    private List<NotRepairedAuthorVO> existAuthorStatistic;
}
