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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 代码重复率的扫描统计结果
 *
 * @version V2.5
 * @date 2017/11/7
 */
@Data
@ApiModel("代码重复率的扫描统计结果")
public class DUPCScanSummaryVO
{
    @ApiModelProperty(value = "重复文件数", required = true)
    @JsonProperty("dup_file_count")
    private long dupFileCount;

    @ApiModelProperty(value = "总文件数", required = true)
    @JsonProperty("total_file_count")
    private long totalFileCount;

    @ApiModelProperty(value = "分析耗时", required = true)
    @JsonProperty("processing_time")
    private long processingTime;

    @ApiModelProperty(value = "significant有意义的行，即非注释行", required = true)
    @JsonProperty("significantline_count")
    private long significantlineCount;

    @ApiModelProperty(value = "重复块数", required = true)
    @JsonProperty("dup_block_count")
    private long dupBlockCount;

    @ApiModelProperty(value = "原始代码行数，即包括注释行", required = true)
    @JsonProperty("rawline_count")
    private long rawlineCount;

    @ApiModelProperty(value = "重复代码行数", required = true)
    @JsonProperty("dup_line_count")
    private long dupLineCount;
}
