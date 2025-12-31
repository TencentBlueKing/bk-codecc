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

package com.tencent.bk.codecc.task.api;

import com.tencent.bk.codecc.task.vo.GrayReportVO;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 灰度工具项目调用接口
 *
 * @version V2.0
 * @date 2020/12/29
 */
@Tag(name = "GRAY_TOOL_PROJECT", description = "灰度工具项目调用接口")
@Path("/build/gray/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildGrayToolProjectRestResource {
    @Operation(summary = "灰度项目数据更新")
    @Path("/pool/toolName/{toolName}/stage/{stage}")
    @POST
    Result<Boolean> createGrayTaskPool(
            @Parameter(description = "工具名", required = true) @PathParam("toolName") String toolName,
            @Parameter(description = "语言", required = false) @QueryParam("langCode") String langCode,
            @Parameter(description = "阶段", required = true) @PathParam("stage") String stage,
            @Parameter(description = "当前用户", required = true) @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) String user);

    @Operation(summary = "触发灰度池项目")
    @Path("/pool/trigger/toolName/{toolName}/taskNum")
    @GET
    Result<TriggerGrayToolVO> triggerGrayTaskPool(
            @Parameter(description = "工具名", required = true) @PathParam("toolName") String toolName,
            @Parameter(description = "执行任务数", required = false) @QueryParam("taskNum") String taskNum,
            @Parameter(description = "语言", required = false) @QueryParam("langCode") String langCode
    );

    @Operation(summary = "查询灰度报告")
    @Path("/report/toolName/{toolName}/codeccBuildId/{codeccBuildId}")
    @GET
    Result<GrayToolReportVO> findGrayReportByToolNameAndCodeCCBuildId(
        @Parameter(description = "工具名", required = true)
        @PathParam("toolName")
            String toolName,
        @Parameter(description = "codecc构建id", required = true)
        @PathParam("codeccBuildId")
            String codeccBuildId);

    @Operation(summary = "根据构建号查询灰度报告")
    @Path("/report/byBuildNum")
    @POST
    Result<GrayToolReportVO> findGaryReportByToolNameAndCodeCCBuildIdAndBuildNum(
        @Parameter(description = "报告信息", required = true)
        GrayReportVO grayReportVO
    );

    @Operation(summary = "查询任务清单")
    @Path("/task/list/toolName/{toolName}")
    @GET
    Result<Set<Long>> findTaskListByToolName(
        @Parameter(description = "工具名", required = true)
        @PathParam("toolName")
            String toolName);
}
