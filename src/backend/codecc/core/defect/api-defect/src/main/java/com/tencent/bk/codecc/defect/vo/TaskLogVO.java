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
import lombok.Data;

import java.util.List;

/**
 * 任务分析记录信息
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Data
@Schema(description = "任务分析记录信息")
public class TaskLogVO
{
    @Schema(description = "流名称", required = true)
    private String streamName;

    @Schema(description = "任务主键id", required = true)
    private long taskId;

    @Schema(description = "工具名称", required = true)
    private String toolName;

    @Schema(description = "当前步骤", required = true)
    private int currStep;

    @Schema(description = "步骤状态")
    private int flag;

    @Schema(description = "开始时间")
    private long startTime;

    @Schema(description = "结束时间")
    private long endTime;

    @Schema(description = "耗时")
    private long elapseTime;

    @Schema(description = "流水线id")
    private String pipelineId;

    @Schema(description = "构建id")
    private String buildId;

    @Schema(description = "构建号")
    private String buildNum;

    @Schema(description = "触发来源")
    private String triggerFrom;

    @Schema(description = "步骤列表", required = true)
    private List<TaskUnit> stepArray;

    @Data
    public static class TaskUnit
    {
        @Schema(description = "当前步骤", required = true)
        private int stepNum;

        @Schema(description = "步骤状态", required = true)
        private int flag;

        @Schema(description = "开始时间", required = true)
        private long startTime;

        @Schema(description = "结束时间")
        private long endTime;

        @Schema(description = "步骤信息")
        private String msg;

        @Schema(description = "步骤耗时")
        private long elapseTime;

        @Schema(description = "GOML目录结构是否符合规范,true/false")
        private boolean dirStructSuggestParam;

        @Schema(description = "GOML编译是否成功，true（成功）/false（失败）")
        private boolean compileResult;
    }
}
