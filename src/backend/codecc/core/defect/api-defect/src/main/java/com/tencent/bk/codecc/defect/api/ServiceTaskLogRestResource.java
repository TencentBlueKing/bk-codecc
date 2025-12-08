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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.defect.vo.BatchLastAnalyzeReqVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.GetLastAnalysisResultsVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Tag(name = "SERVICE_TASKLOG", description = "工具侧上报任务分析记录接口")
@Path("/service/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTaskLogRestResource
{
    @Operation(summary = "停止正在运行的任务")
    @Path("/runningTask/pipelineId/{pipelineId}/streamName/{streamName}")
    @POST
    Result<Boolean> stopRunningTask(
            @Parameter(description = "流水线id", required = true)
            @PathParam("pipelineId")
                    String pipelineId,
            @Parameter(description = "任务名称", required = true)
            @PathParam("streamName")
                    String streamName,
            @Parameter(description = "工具清单", required = true)
                    Set<String> toolSet,
            @Parameter(description = "项目id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
                    String projectId,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName);


    @Operation(summary = "获取最新分析记录")
    @Path("/latest/toolName/{toolName}/taskId/{taskId}")
    @GET
    Result<TaskLogVO> getLatestTaskLog(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName
    );

    @Operation(summary = "获取最新分析记录")
    @Path("/toolName/{toolName}/taskId/{taskId}/buildId/{buildId}")
    @GET
    Result<TaskLogVO> getTaskLog(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
            long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "构建ID", required = true)
            @PathParam("buildId")
            String buildId
    );

    @Operation(summary = "平台侧获取任务所有有效工具的最近一次分析结果")
    @Path("/lastAnalysisResults")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getLastAnalysisResults(
            @Parameter(description = "获取最近一次分析结果的请求对象", required = true)
                    GetLastAnalysisResultsVO getLastAnalysisResultsVO);

    @Operation(summary = "平台侧获取任务所有有效工具的某一次分析结果")
    @Path("/analysisResults")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getAnalysisResults(
        @Parameter(description = "获取某一次分析结果的请求对象", required = true)
            GetLastAnalysisResultsVO getLastAnalysisResultsVO);

    @Operation(summary = "根据BUILD_ID平台侧获取任务所有有效工具的某一次分析结果")
    @Path("/analysisResult")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getAnalysisResult(
            @Parameter(description = "获取某一次分析结果的请求对象", required = true)
            GetLastAnalysisResultsVO getLastAnalysisResultsVO);

    @Operation(summary = "获取最近统计信息")
    @Path("/lastStatisticResult")
    @POST
    Result<BaseLastAnalysisResultVO> getLastStatisticResult(
            @Parameter(description = "获取最近统计信息的请求对象", required = true)
                    ToolLastAnalysisResultVO toolLastAnalysisResultVO);


    @Operation(summary = "批量获取最新分析记录")
    @Path("/latest/batch/taskId/{taskId}")
    @POST
    Result<List<ToolLastAnalysisResultVO>> getBatchLatestTaskLog(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    long taskId,
            @Parameter(description = "工具名称清单", required = true)
                    Set<String> toolSet);

    @Operation(summary = "任务维度批量获取最新分析记录")
    @Path("/latest/batchTask")
    @POST
    Result<Map<String, List<ToolLastAnalysisResultVO>>> getBatchTaskLatestTaskLog(
            @Parameter(description = "任务id及工具集映射参数", required = true)
                    List<TaskDetailVO> taskDetailVOList);

    @Operation(summary = "批量获取最新分析记录")
    @Path("/suggest/param")
    @PUT
    Result<Boolean> uploadDirStructSuggestParam(
            @Parameter(description = "上传参数建议值信息", required = true)
                    UploadTaskLogStepVO uploadTaskLogStepVO);


    @Operation(summary = "批量获取最新分析记录")
    @Path("/pipeline")
    @PUT
    Result<Boolean> refreshTaskLogByPipeline(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "工具集合", required = true)
                    Set<String> toolNames);

    @Operation(summary = "批量获取最新分析记录")
    @Path("/latest/repo")
    @PUT
    Result<Map<String, TaskLogRepoInfoVO>> getLastAnalyzeRepoInfo(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId);

    @Operation(summary = "批量获取最新分析记录")
    @Path("/batchGet/taskLogs")
    @POST
    Result<Map<Long, Integer>> batchGetAnalyzeFlag(
            @Parameter(description = "请求参数", required = true)
            BatchLastAnalyzeReqVO batchLastAnalyzeReqVO
    );

}
