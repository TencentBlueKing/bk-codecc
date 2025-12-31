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

package com.tencent.bk.codecc.defect.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.QueryTaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.task.vo.QueryLogRepVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.data.domain.PageImpl;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Tag(name = "TASK_LOG", description = "任务分析记录接口")
@Path("/user/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UserTaskLogRestResource
{

    @Operation(summary = "获取分析记录")
    @Path("/")
    @GET
    Result<QueryTaskLogVO> getTaskLogs(
            @Parameter(description = "工具名称", required = true)
            @QueryParam("toolName")
                    String toolName,
            @Parameter(description = "第几页", required = true)
            @QueryParam("page")
                    int page,
            @Parameter(description = "每页多少条", required = true)
            @QueryParam("pageSize")
                    int pageSize
    );

    @Operation(summary = "获取分析记录 V2 接口")
    @Path("/overview")
    @GET
    Result<PageImpl<TaskLogOverviewVO>> getTaskLogs(
            @Parameter(description = "任务ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @Parameter(description = "第几页")
            @QueryParam("page")
                    Integer page,
            @Parameter(description = "每页多少条")
            @QueryParam("pageSize")
                    Integer pageSize
    );

    @Operation(summary = "获取分析记录日志")
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}")
    @GET
    Result<QueryLogRepVO> getAnalysisLogs(
            @Parameter(description = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @Parameter(description = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @Parameter(description = "构建号ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "搜索关键字")
            @QueryParam("queryKeywords")
                    String queryKeywords,
            @Parameter(description = "对应elementId")
            @QueryParam("tag")
                    String tag,
            @Parameter(description = "单流水线对应多任务标识")
            @QueryParam("multiPipelineMark")
                    String multiPipelineMark
    );

    @Operation(summary = "获取更多日志")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/more")
    Result<QueryLogRepVO> getMoreLogs(
            @Parameter(description = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @Parameter(description = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "日志行数")
            @QueryParam("num")
                    Integer num,
            @Parameter(description = "是否正序输出")
            @QueryParam("fromStart")
                    Boolean fromStart,
            @Parameter(description = "起始行号", required = true)
            @QueryParam("start")
                    Long start,
            @Parameter(description = "结尾行号", required = true)
            @QueryParam("end")
                    Long end,
            @Parameter(description = "对应elementId")
            @QueryParam("tag")
                    String tag,
            @Parameter(description = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount,
            @Parameter(description = "单流水线对应多任务标识")
            @QueryParam("multiPipelineMark")
                    String multiPipelineMark
    );


    @Operation(summary = "下载日志接口")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    void downloadLogs(
            @Parameter(description = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @Parameter(description = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
                    String pipelineId,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "对应element ID")
            @QueryParam("tag")
                    String tag,
            @Parameter(description = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount,
            @Parameter(description = "单流水线对应多任务标识")
            @QueryParam("multiPipelineMark")
                    String multiPipelineMark
    );


    @Operation(summary = "获取某行后的日志")
    @GET
    @Path("/analysis/logs/{projectId}/{pipelineId}/{buildId}/after")
    Result<QueryLogRepVO> getAfterLogs(
            @Parameter(description = "用户ID")
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userId,
            @Parameter(description = "项目ID", required = true)
            @PathParam("projectId")
                    String projectId,
            @Parameter(description = "流水线ID", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
                    String buildId,
            @Parameter(description = "起始行号", required = true)
            @QueryParam("start")
                    Long start,
            @Parameter(description = "搜索关键字")
            @QueryParam("queryKeywords")
                    String queryKeywords,
            @Parameter(description = "对应elementId")
            @QueryParam("tag")
                    String tag,
            @Parameter(description = "执行次数")
            @QueryParam("executeCount")
                    Integer executeCount,
            @Parameter(description = "单流水线对应多任务标识")
            @QueryParam("multiPipelineMark")
                    String multiPipelineMark
    );


}
