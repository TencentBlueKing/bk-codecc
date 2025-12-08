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

import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoWithMetadataVO;
import com.tencent.bk.codecc.task.vo.ToolTaskInfoVO;
import com.tencent.bk.codecc.task.vo.checkerset.ClearTaskCheckerSetReqVO;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineBuildInfoVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Tag(name = "SERVICE_TOOL", description = "工具管理接口")
@Path("/service/tool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceToolRestResource {
    @Operation(summary = "更新工具分析步骤及状态")
    @Path("/")
    @PUT
    Result updateToolStepStatus(
            @Parameter(description = "需要更新的工具基本信息", required = true)
                    ToolConfigBaseVO toolConfigBaseVO
    );


    @Operation(summary = "根据任务id获取工具信息")
    @Path("/tool/{toolName}")
    @GET
    Result<ToolConfigInfoVO> getToolByTaskIdAndName(
            @Parameter(description = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName);


    @Operation(summary = "根据任务id获取带名称的工具信息")
    @Path("/tool/name/{toolName}")
    @GET
    Result<ToolConfigInfoWithMetadataVO> getToolWithMetadataByTaskIdAndName(
            @Parameter(description = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    long taskId,
            @Parameter(description = "工具名称", required = true)
            @PathParam("toolName")
                    String toolName);


    @Operation(summary = "获取工具顺序")
    @Path("/order")
    @GET
    Result<String> findToolOrder();

    @Operation(summary = "更新流水线工具配置")
    @Path("/pipeline/tools")
    @PUT
    Result<Boolean> updatePipelineTool(
            @Parameter(description = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String userName,
            @Parameter(description = "工具信息", required = true)
                    List<String> toolList);

    @Operation(summary = "清除任务和工具关联的规则集")
    @Path("/tasks/{taskId}/checkerSets/relationships")
    @DELETE
    Result<Boolean> clearCheckerSet(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "清除规则集ID请求体", required = true)
                    ClearTaskCheckerSetReqVO clearTaskCheckerSetReqVO);

    @Operation(summary = "设置任务和工具关联的规则集")
    @Path("/tasks/{taskId}/checkerSets/relationships")
    @POST
    Result<Boolean> addCheckerSet2Task(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "设置规则集ID请求体", required = true)
                    UpdateCheckerSet2TaskReqVO addCheckerSet2TasklReqVO);

    @Operation(summary = "获取分析配置信息")
    @Path("/config/streamName/{streamName}/toolType/{toolName}")
    @POST
    Result<AnalyzeConfigInfoVO> getAnalyzeConfig(
            @Parameter(description = "任务英文名", required = true)
            @PathParam("streamName")
                    String streamName,
            @Parameter(description = "工具名", required = true)
            @PathParam("toolName")
                    String toolName,
            @Parameter(description = "上传分析任务详情", required = true)
                    PipelineBuildInfoVO pipelineBuildInfoVO
    );

    @Operation(summary = "获取分析配置信息")
    @Path("/tasks/{taskId}/toolConfiguration")
    @POST
    Result<Boolean> updateTools(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId,
            @Parameter(description = "用户名", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
                    String user,
            @Parameter(description = "更新工具配置请求体", required = true)
                    BatchRegisterVO batchRegisterVO
    );

    @Operation(summary = "批量查询工具配置信息")
    @Path("/batch/toolConfig/list")
    @POST
    Result<List<ToolConfigInfoVO>> batchGetToolConfigList(
            @Parameter(description = "任务批量查询模型", required = true)
                    QueryTaskListReqVO queryTaskListReqVO
    );

    @Operation(summary = "获取失败工具的任务id")
    @Path("/failedTaskIds")
    @POST
    Result<Map<Integer, List<Long>>> getToolFailedTaskIds(
            @Parameter(description = "工具名称")
            @QueryParam("toolName")
            String toolName,
            @Parameter(description = "创建来源")
            @QueryParam("createFrom")
            Set<String> createFrom,
            @Parameter(description = "查看详情传的日期")
            @QueryParam("detailTime")
            String detailTime
    );

    @Operation(summary = "获取工具新增停用任务统计")
    @Path("/addAndStop")
    @POST
    Result<ToolTaskInfoVO> getToolInfoConfigByToolName(
            @Parameter(description = "工具名称")
            @QueryParam("toolName")
            String toolName,
            @Parameter(description = "查看详情传的日期")
            @QueryParam("detailTime")
            String detailTime
    );

    @Operation(summary = "根据工具查询任务信息分页")
    @Path("/toolTaskIdList")
    @POST
    Result<List<Long>> getTaskInfoByToolNameAndTaskId(
            @Parameter(description = "任务id列表")
            List<Long> taskIdList,
            @Parameter(description = "工具名称")
            @QueryParam("toolName")
            String toolName
    );

}
