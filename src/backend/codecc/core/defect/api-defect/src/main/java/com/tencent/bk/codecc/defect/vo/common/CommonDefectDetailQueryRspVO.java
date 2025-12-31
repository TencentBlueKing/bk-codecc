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

package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公用告警详情查询返回视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@Schema(description = "公用告警详情查询返回视图")
public class CommonDefectDetailQueryRspVO
{
    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件内容")
    private String fileContent;

    @Schema(description = "相对路径")
    private String relativePath;

    @Schema(description = "截取的代码片段的起始行")
    private int trimBeginLine;

    @Schema(description = "版本号")
    private String revision;

    @Schema(description = "同分支的最后一次构建号")
    private String lastBuildNumOfSameBranch;

    @Schema(description = "该告警在同分支的最后一次构建中是否已被修复")
    private Boolean defectIsFixedOnLastBuildNumOfSameBranch;

    @Schema(description = "AI告警修复建议")
    private String defectSuggestions;
}
