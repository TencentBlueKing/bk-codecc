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

package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 任务更新视图
 *
 * @version V1.0
 * @date 2019/5/23
 */
@Data
@Schema(description = "任务更新视图")
public class TaskUpdateVO
{

    @Schema(description = "任务主键id", required = true)
    private long taskId;

    @Schema(description = "任务中文名")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}", message = "输入的中文名称不符合命名规则")
    private String nameCn;

    @Schema(description = "代码语言")
    private Long codeLang;

    @Schema(description = "工具特定参数")
    private List<ToolConfigParamJsonVO> devopsToolParams;

    @Schema(description = "任务负责人/管理员")
    private List<String> taskOwner;

    @Schema(description = "任务成员")
    private List<String> taskMember;

    @Schema(description = "任务状态")
    private int status;

    @Schema(description = "任务停用时间")
    private String disableTime;


}
