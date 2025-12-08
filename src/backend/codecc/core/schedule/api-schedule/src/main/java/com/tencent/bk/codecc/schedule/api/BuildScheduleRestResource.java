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

package com.tencent.bk.codecc.schedule.api;

import com.tencent.bk.codecc.schedule.vo.TailLogRspVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;

/**
 * 分析服务调度接口
 *
 * @version V2.0
 * @date 2019/09/28
 */
@Tag(name = "SERVICE_DISPATCH", description = "分析服务调度接口")
@Path("/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildScheduleRestResource {
    @Operation(summary = "推入分析任务")
    @Path("/push/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}")
    @GET
    Result<Boolean> push(
            @Parameter(description = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "构建ID")
            @QueryParam("createFrom")
                    String createFrom,
            @Parameter(description = "任务所属蓝盾项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId);

    @Operation(summary = "实时获取日志")
    @Path("/log/streamName/{streamName}/toolName/{toolName}/buildId/{buildId}/beginLine/{beginLine}")
    @GET
    Result<TailLogRspVO> tailLog(
            @Parameter(description = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "开始行", required = true)
            @PathParam("beginLine")
                    long beginLine);
}
