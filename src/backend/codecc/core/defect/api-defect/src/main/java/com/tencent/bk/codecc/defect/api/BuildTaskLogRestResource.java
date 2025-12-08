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

import com.tencent.bk.codecc.defect.vo.GrayBuildNumAndTaskVO;
import com.tencent.bk.codecc.defect.vo.GrayTaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.UploadToolErrorTaskLogVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * 分析任务构建机接口
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Tag(name = "SERVICE_TASKLOG", description = "工具侧上报任务分析记录接口")
@Path("/build/tasklog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskLogRestResource
{

    @Operation(summary = "上报任务分析记录")
    @Path("/")
    @POST
    Result uploadTaskLog(
            @Parameter(description = "上传分析任务详情", required = true)
                    UploadTaskLogStepVO uploadTaskLogStepVO
    );

    @Operation(summary = "上报工具分析失败记录")
    @Path("/reportToolErrorTaskLog")
    @POST
    Result<Boolean> uploadToolErrorTaskLog(
        @Parameter(description = "上传工具分析失败详情", required = true)
        UploadToolErrorTaskLogVO uploadToolErrorTaskLogVO
    );


    @Operation(summary = "批量获取最新分析记录")
    @Path("/suggest/param")
    @PUT
    Result<Boolean> uploadDirStructSuggestParam(
            @Parameter(description = "上传参数建议值信息", required = true)
                    UploadTaskLogStepVO uploadTaskLogStepVO);

    @Operation(summary = "获取当前构建的分析记录")
    @Path("/taskId/{taskId}/toolName/{toolName}/buildId/{buildId}")
    @GET
    Result<TaskLogVO> getBuildTaskLog(
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

    @Operation(summary = "批量获取最新分析的代码库信息(上次执行时间和上次版本库信息)")
    @Path("/taskId/{taskId}/toolName/{toolName}/latest/repo")
    @PUT
    Result<TaskLogRepoInfoVO> getLastAnalyzeRepoInfo(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName);

    @Operation(summary = "根据构建号和taskId获取tasklog集合")
    @POST
    @Path("/getTaskLogByBuildNumAndTaskId")
    Result<List<GrayTaskLogRepoInfoVO>> getTaskLogInfoByBuildBumAndTaskId(GrayBuildNumAndTaskVO grayBuildNumAndTaskVO);

    @Operation(summary = "获取最近N次工具构建成功的分析记录")
    @Path("/taskId/{taskId}/toolName/{toolName}")
    @GET
    Result<List<TaskLogVO>> listBuildTaskLog(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
            long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
            String toolName,
            @Parameter(description = "最近N次")
            @QueryParam("range")
            int range
    );
}
