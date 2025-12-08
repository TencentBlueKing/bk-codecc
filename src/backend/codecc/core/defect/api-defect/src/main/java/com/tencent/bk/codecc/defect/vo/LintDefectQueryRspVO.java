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

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * lint类告警查询返回视图
 *
 * @version V1.0
 * @date 2019/5/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "lint类告警查询返回视图")
public class LintDefectQueryRspVO extends CommonDefectQueryRspVO
{
    @Schema(description = "严重规则数")
    private int seriousCount;

    @Schema(description = "正常规则数")
    private int normalCount;

    @Schema(description = "提示规则数")
    private int promptCount;

    @Schema(description = "待修复告警数")
    private int existCount;

    @Schema(description = "已修复告警数")
    private int fixCount;

    @Schema(description = "已忽略告警数")
    private int ignoreCount;

    @Schema(description = "新增文件数")
    private int newCount;

    @Schema(description = "历史文件数")
    private int historyCount;

    @Schema(description = "符合条件的告警总数")
    private int totalCount;

    /**
     * 按文件聚类时使用
     */
    @Schema(description = "lint类文件清单")
    private Page<LintFileVO> fileList;

    /**
     * 按问题聚类时使用
     */
    @Schema(description = "告警清单")
    private Page<LintDefectVO> defectList;
}
