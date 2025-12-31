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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.api;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_BUILD_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.CodeYmlRepoOwnerVO;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.RuntimeUpdateMetaVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.validation.Valid;
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


/**
 * 构建机任务接口
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Tag(name = "BUILD_TASK", description = "任务管理接口")
@Path("/build/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildTaskRestResource {

    @Operation(summary = "获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoById(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @Operation(summary = "获取任务信息")
    @Path("/streamName/{streamName}")
    @GET
    Result<TaskDetailVO> getTaskInfoByStreamName(
            @Parameter(description = "流名称（也即任务英文名）", required = true)
            @PathParam(value = "streamName")
            String streamName
    );

    @Operation(summary = "获取任务信息")
    @Path("/pipeline/{pipelineId}")
    @GET
    Result<PipelineTaskVO> getTaskInfoByPipelineId(
            @Parameter(description = "流水线id", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @Parameter(description = "流水线附加标识", required = true)
            @QueryParam(value = "multiPipelineMark")
            String multiPipelineMark,
            @Parameter(description = "用户id", required = true)
            @QueryParam(value = "userId")
            String userId
    );

    @Operation(summary = "从流水线注册任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerPipelineTask(
            @Parameter(description = "任务详细信息", required = true)
            TaskDetailVO taskDetailVO,
            @Parameter(description = "当前项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName);

    @Operation(summary = "修改任务信息")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            @Parameter(description = "任务修改信息", required = true)
            TaskDetailVO taskDetailVO,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @Operation(summary = "将自建任务 id 和异步执行该自建任务的流水线 id 建立联系")
    @Path("/add/relationship/pipelineId/{pipelineId}")
    @POST
    Result<Boolean> addRelationshipBetweenTaskAndPipeline(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "用户ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @Parameter(description = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId
    );

    @Operation(summary = "停用任务")
    @Path("/{taskId}")
    @DELETE
    Result<Boolean> stopTask(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId,
            @Parameter(description = "停用原因", required = true)
            @QueryParam("disabledReason")
            String disabledReason,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @Operation(summary = "检查任务是否存在")
    @Path("/exists/{taskId}")
    @GET
    Result<Boolean> checkTaskExists(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @Operation(summary = "保存定制化报告信息")
    @Path("/report")
    @POST
    Result<Boolean> updateTaskReportInfo(
            @Parameter(description = "任务id", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "通知信息", required = true)
            NotifyCustomVO notifyCustomVO);

    @Operation(summary = "修改任务扫描触发配置")
    @Path("/taskId/{taskId}/scanConfiguration")
    @POST
    Result<Boolean> updateScanConfiguration(
            @Parameter(description = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
            @Parameter(description = "定时分析信息", required = true)
            ScanConfigurationVO scanConfigurationVO
    );

    @Operation(summary = "添加路径屏蔽")
    @Path("/add/filter/path")
    @POST
    Result<Boolean> addFilterPath(
            @Parameter(description = "任务信息", required = true)
            @Valid
            FilterPathInputVO filterPathInput,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );


    @Operation(summary = "删除路径屏蔽")
    @Path("/del/filter")
    @DELETE
    Result<Boolean> deleteFilterPath(
            @Parameter(description = "删除路径", required = true)
            @QueryParam("path")
            String path,
            @Parameter(description = "路径类型", required = true)
            @QueryParam("pathType")
            String pathType,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );


    @Operation(summary = "路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId
    );

    @Operation(summary = "路径屏蔽列表")
    @Path("/filter/path/pipeline/{pipelineId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @Parameter(description = "流水线ID", required = true)
            @PathParam("pipelineId")
            String pipelineId,
            @Parameter(description = "标识", required = false)
            @QueryParam("mark")
            String mark
    );

    @Operation(summary = "更新code.yml的路径屏蔽")
    @Path("/code/yml/filter/update")
    @POST
    Result<Boolean> codeYmlFilterPath(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "当前用户", required = true)
            CodeYmlFilterPathVO codeYmlFilterPathVO
    );

    @Operation(summary = "更新code.yml的repoOwner屏蔽")
    @Path("/code/yml/repoOwner/update")
    @POST
    Result<Boolean> codeYmlRepoOwner(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "当前用户", required = true)
            CodeYmlRepoOwnerVO codeYmlRepoOwnerVO
    );

    @Operation(summary = "更新code.yml的repoOwner屏蔽")
    @Path("/runtime/update")
    @POST
    Result<Boolean> runtimeMetaUpdate(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            Long taskId,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "当前用户", required = true)
            RuntimeUpdateMetaVO runtimeUpdateMetaVO
    );

    @Operation(summary = "获取工具platform信息")
    @Path("/toolConfig/info")
    @GET
    Result<ToolConfigPlatformVO> getToolConfigInfo(
            @Parameter(description = "任务ID", required = true)
            @QueryParam("taskId")
            Long taskId,
            @Parameter(description = "工具名称", required = true)
            @QueryParam("toolName")
            String toolName
    );

    @Operation(summary = "发送开始任务信号")
    @Path("/startSignal/taskId/{taskId}/buildId/{buildId}")
    @GET
    Result<Boolean> sendStartTaskSignal(
            @Parameter(description = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @Parameter(description = "构件号id", required = true)
            @PathParam("buildId")
            String buildId,
            @Parameter(description = "超时时间(S)")
            @QueryParam("timeout")
            Integer timeout);

    @Operation(summary = "触发立即分析")
    @Path("/execute")
    @POST
    Result<Boolean> executeTask(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "是否首次触发")
            @QueryParam("isFirstTrigger")
            String isFirstTrigger,
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName);

    @Operation(summary = "添加路径白名单")
    @Path("/path")
    @POST
    Result<Boolean> addWhitePath(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "任务信息", required = true)
            List<String> pathList
    );


    @Operation(summary = "注册流水线回调")
    @Path("/registerPipelineCallback")
    @POST
    Result<Boolean> registerPipelineCallback(
            @Parameter(description = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName,
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
            long taskId,
            @Parameter(description = "构建ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
            String buildId
    );
}
