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

package com.tencent.bk.codecc.defect.vo.redline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 圈复杂度、重复率红线告警
 *
 * @version V1.0
 * @date 2019/7/4
 */
@Data
@Schema(description = "圈复杂度红线告警")
public class RLCcnAndDupcDefectVO
{

    @Schema(description = "平均圈复杂度/平均代码重复率")
    private Double average;

    @Schema(description = "单函数圈复杂度最大值")
    private Long singleFuncMax;

    @Schema(description = "极高风险函数数量/极高风险文件数")
    private Long extreme;

    @Schema(description = "高风险函数数量/高风险文件数")
    private Long high;

    @Schema(description = "中风险函数数量/中风险文件数率")
    private Long middle;

    @Schema(description = "单文件代码重复率最大值")
    private Double singleFileMax;

    @Schema(description = "新增单函数圈复杂度最大值")
    private Long newSingleFuncMax;

    @Schema(description = "新增风险函数数量")
    private Long newFuncCount;

    @Schema(description = "新风险函数超标复杂度总数")
    private Long newFuncBeyondThresholdSum;

    @Schema(description = "历史风险函数超标复杂度总数")
    private Long historyFuncBeyondThresholdSum;

    /**
     * 以圈复杂度阈值为20为例，有风险函数4个，圈复杂度分布为21、26、31、32
     * C30 = （31-30）+（32-30）= 3
     * C25 = （31-25）+（32-25）+（26-25）= 16+7+1 = 14
     * C20 = （31-20）+（32-20）+（26-20）+（21-20）= 11+12+6+1= 30
     * C15 无（因为15已经小于圈复杂度阈值20了，此时计算出来肯定不准确）
     * C10 无（因为10已经小于圈复杂度阈值20了，此时计算出来肯定不准确）
     * C5 无（因为5已经小于圈复杂度阈值20了，此时计算出来肯定不准确）
     */
    @Schema(description = "圈复杂度超标率(阈值=30)")
    private Long c30;
    
    @Schema(description = "圈复杂度超标率(阈值=25)")
    private Long c25;
    
    @Schema(description = "圈复杂度超标率(阈值=20)")
    private Long c20;
    
    @Schema(description = "圈复杂度超标率(阈值=15)")
    private Long c15;
    
    @Schema(description = "圈复杂度超标率(阈值=10)")
    private Long c10;
    
    @Schema(description = "圈复杂度超标率(阈值=5)")
    private Long c5;

    public RLCcnAndDupcDefectVO average(Double average) {
        this.average = average;
        return this;
    }

    public RLCcnAndDupcDefectVO singleFuncMax(Long singleFuncMax) {
        this.singleFuncMax = singleFuncMax;
        return this;
    }

    public RLCcnAndDupcDefectVO extreme(Long extreme) {
        this.extreme = extreme;
        return this;
    }

    public RLCcnAndDupcDefectVO high(Long high) {
        this.high = high;
        return this;
    }

    public RLCcnAndDupcDefectVO middle(Long middle) {
        this.middle = middle;
        return this;
    }

    public RLCcnAndDupcDefectVO singleFileMax(Double singleFileMax) {
        this.singleFileMax = singleFileMax;
        return this;
    }

    public RLCcnAndDupcDefectVO newSingleFuncMax(Long newSingleFuncMax) {
        this.newSingleFuncMax = newSingleFuncMax;
        return this;
    }

    public RLCcnAndDupcDefectVO newFuncCount(Long newFuncCount) {
        this.newFuncCount = newFuncCount;
        return this;
    }

    public RLCcnAndDupcDefectVO newFuncBeyondThresholdSum(Long newFuncBeyondThresholdSum) {
        this.newFuncBeyondThresholdSum = newFuncBeyondThresholdSum;
        return this;
    }

    public RLCcnAndDupcDefectVO historyFuncBeyondThresholdSum(Long historyFuncBeyondThresholdSum) {
        this.historyFuncBeyondThresholdSum = historyFuncBeyondThresholdSum;
        return this;
    }

    public RLCcnAndDupcDefectVO c30(Long c30) {
        this.c30 = c30;
        return this;
    }

    public RLCcnAndDupcDefectVO c25(Long c25) {
        this.c25 = c25;
        return this;
    }

    public RLCcnAndDupcDefectVO c20(Long c20) {
        this.c20 = c20;
        return this;
    }

    public RLCcnAndDupcDefectVO c15(Long c15) {
        this.c15 = c15;
        return this;
    }

    public RLCcnAndDupcDefectVO c10(Long c10) {
        this.c10 = c10;
        return this;
    }

    public RLCcnAndDupcDefectVO c5(Long c5) {
        this.c5 = c5;
        return this;
    }
}
