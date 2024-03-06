/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.GetLatestBuildIdMapRequest;
import com.tencent.bk.codecc.task.vo.GetTaskStatusAndCreateFromResponse;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigRequest;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskStatisticVO;
import com.tencent.bk.codecc.task.vo.checkerset.UpdateCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.devops.common.api.CommonPageVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.StatisticTaskCodeLineToolVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * task interface
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Api(tags = {"SERVICE_TASK"}, description = "任务管理接口")
@Path("/service/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceTaskRestResource {

    @ApiOperation("获取任务信息")
    @Path("/taskInfo")
    @GET
    Result<TaskDetailVO> getTaskInfo(
            @ApiParam(value = "任务英文名", required = true)
            @QueryParam("nameEn")
            String nameEn);

    @ApiOperation("获取任务已接入工具列表")
    @Path("/tools")
    @GET
    Result<TaskBaseVO> getTaskToolList(
            @ApiParam(value = "任务ID", required = true)
            @QueryParam("taskId")
            long taskId);


    @ApiOperation("从流水线注册任务")
    @Path("/")
    @POST
    Result<TaskIdVO> registerPipelineTask(
            @ApiParam(value = "任务详细信息", required = true)
            TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前项目", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName);


    @ApiOperation("通过taskId获取任务信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoById(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );

    @ApiOperation("批量获取任务信息")
    @Path("/list")
    @POST
    Result<List<TaskBaseVO>> getTaskInfosByIds(
            @ApiParam(value = "任务ID清单", required = true)
            List<Long> taskIds);

    @ApiOperation("批量获取任务信息")
    @Path("/taskInfoByIdsWithDelete")
    @POST
    Result<List<TaskDetailVO>> getTaskDetailListByIdsWithDelete(
            @ApiParam(value = "任务ID清单", required = true)
            List<Long> taskIds);

    @ApiOperation("通过taskid查询任务信息，不包含工具信息")
    @Path("/taskInfoWithoutTools/{taskId}")
    @GET
    Result<TaskDetailVO> getTaskInfoWithoutToolsByTaskId(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId);

    @ApiOperation("修改任务信息")
    @Path("/")
    @PUT
    Result<Boolean> updateTask(
            @ApiParam(value = "任务修改信息", required = true)
            TaskDetailVO taskDetailVO,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @ApiOperation("停用任务")
    @Path("/{taskId}")
    @DELETE
    Result<Boolean> stopTask(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId,
            @ApiParam(value = "停用原因", required = true)
            @QueryParam("disabledReason")
            String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userName
    );

    @ApiOperation("停用任务")
    @Path("/pipeline/stop")
    @DELETE
    Result<Boolean> stopTaskByPipeline(
            @ApiParam(value = "流水线ID", required = true)
            @QueryParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "停用原因", required = true)
            @QueryParam("disabledReason")
            String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @QueryParam("userName")
            String userName
    );

    @ApiOperation("停用单个流水线任务")
    @Path("/pipeline/stopSingle")
    @DELETE
    Result<Boolean> stopSingleTaskByPipeline(
            @ApiParam(value = "流水线ID", required = true)
            @QueryParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "多任务标识", required = false)
            @QueryParam(value = "multiPipelineMark")
            String multiPipelineMark,
            @ApiParam(value = "停用原因", required = true)
            @QueryParam("disabledReason")
            String disabledReason,
            @ApiParam(value = "当前用户", required = true)
            @QueryParam("userName")
            String userName
    );

    @ApiOperation("检查任务是否存在")
    @Path("/exists/{taskId}")
    @GET
    Result<Boolean> checkTaskExists(
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId
    );


    @ApiOperation("获取所有的基础工具信息")
    @Path("/tool/meta")
    @GET
    Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache();


    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}")
    @GET
    Result<PipelineTaskVO> getPipelineTask(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "单流水线对应多任务标识")
            @QueryParam(value = "multiPipelineMark")
            String multiPipelineMark,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user
    );

    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/pipelines/{pipelineId}/allTask")
    @GET
    Result<List<PipelineTaskVO>> getPipelineAllTask(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user
    );

    @ApiOperation("通过流水线ID获取任务信息（仅返回TaskID）")
    @Path("/pipelines/{pipelineId}/allTaskId")
    @GET
    Result<List<Long>> getPipelineAllTaskId(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user
    );

    @ApiOperation("通过流水线ID获取任务信息")
    @Path("/id/pipeline/{pipelineId}")
    @GET
    Result<Long> getTaskIdByPipelineInfo(
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "单流水线多分支标识", required = false)
            @QueryParam(value = "multiPipelineMark")
            String multiPipelineMark);

    @ApiOperation("通过流水线ID批量获取任务ID")
    @Path("/queryTaskListByPipelineIds")
    @POST
    Result<Set<String>> queryTaskListByPipelineIds(
            @ApiParam(value = "流水线ID集合", required = true)
            Set<String> pipelineIds
    );

    @ApiOperation("获取任务清单")
    @Path("/tasks")
    @GET
    Result<TaskListVO> getTaskList(
            @ApiParam(value = "项目ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
            String projectId,
            @ApiParam(value = "当前用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user
    );


    @ApiOperation("根据bg id获取任务清单")
    @Path("/bgId/{bgId}")
    @GET
    Result<List<TaskBaseVO>> getTasksByBgId(
            @ApiParam(value = "事业群id", required = true)
            @PathParam("bgId")
            Integer bgId);


    @ApiOperation("获取任务信息清单列表")
    @Path("/detail/list")
    @POST
    Result<TaskListVO> getTaskDetailList(
            @ApiParam(value = "任务批量查询模型", required = true)
            QueryTaskListReqVO queryTaskListReqVO);


    @ApiOperation("根据作者获取对应任务信息列表")
    @Path("/myTasks")
    @POST
    Result<Page<TaskInfoVO>> getTasksByAuthor(
            @ApiParam(value = "查询作者名下的任务列表", required = true)
            QueryMyTasksReqVO reqVO);


    @ApiOperation("修改流水线CodeCC配置的规则集")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/tasks/{taskId}/checkerSets")
    @PUT
    Result<Boolean> updatePipelineTaskCheckerSets(
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String user,
            @ApiParam(value = "项目ID", required = true)
            @PathParam(value = "projectId")
            String projectId,
            @ApiParam(value = "流水线ID", required = true)
            @PathParam(value = "pipelineId")
            String pipelineId,
            @ApiParam(value = "任务ID", required = true)
            @PathParam(value = "taskId")
            Long taskId,
            @ApiParam(value = "修改规则集列表", required = true)
            UpdateCheckerSet2TaskReqVO updateCheckerSet2TaskReqVO
    );

    @ApiOperation("查询工蜂项目task")
    @Path("/getByCreateFrom/{taskType}")
    @POST
    Result<Page<Long>> getTaskInfoByCreateFrom(
            @ApiParam(value = "task类型", required = true)
            @PathParam(value = "taskType")
            String taskType,
            @ApiParam(value = "查询工蜂项目task", required = true)
            CommonPageVO reqVO
    );

    @ApiOperation("按事业群ID获取部门ID集合")
    @Path("/org/bgId/{bgId}")
    @GET
    Result<Set<Integer>> queryDeptIdByBgId(
            @ApiParam(value = "事业群ID", required = true)
            @PathParam(value = "bgId")
            Integer bgId
    );

    @ApiOperation("多条件批量获取任务详情列表")
    @Path("/batch/list")
    @POST
    Result<List<TaskDetailVO>> batchGetTaskList(
            @ApiParam(value = "任务批量查询模型", required = true)
            QueryTaskListReqVO queryTaskListReqVO);

    @ApiOperation("路径屏蔽列表")
    @Path("/filter/path/{taskId}")
    @GET
    Result<FilterPathOutVO> filterPath(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId
    );


    @ApiOperation("分页查询任务列表")
    @Path("/detail/page")
    @POST
    Result<Page<TaskDetailVO>> getTaskDetailPage(
            @ApiParam(value = "批量查询参数", required = true)
            QueryTaskListReqVO reqVO);

    @ApiOperation("分页查询任务列表")
    @Path("/author/taskId/{taskId}")
    @PUT
    Result<Boolean> authorTransfer(
            @ApiParam(value = "任务ID", required = true)
            @PathParam("taskId")
            Long taskId,
            @ApiParam(value = "作者转换信息", required = true)
            List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
            @ApiParam(value = "用户", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            String userId);

    @ApiOperation("按创建来源查询任务ID列表")
    @Path("/query/by/createFrom")
    @POST
    Result<List<Long>> queryTaskIdByCreateFrom(
            @ApiParam(value = "任务ID集合", required = true)
            List<String> createFrom
    );

    @ApiOperation("按创建来源查询任务ID列表,排除灰度任务")
    @Path("/query/createFrom/exclude/gray")
    @POST
    Result<List<Long>> queryTaskIdByCreateFromExcludeGray(
            @ApiParam(value = "来源", required = true)
            List<String> createFrom,
            @ApiParam(value = "应用code")
            @QueryParam("pageNum")
            Integer pageNum,
            @ApiParam(value = "应用code")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @ApiOperation("获取任务信息")
    @Path("/get")
    @GET
    Result<TaskDetailVO> getTaskInfoWithoutToolsByStreamName(
            @ApiParam(value = "任务英文名", required = true)
            @QueryParam("nameEn")
            String nameEn
    );

    @ApiOperation("获取任务的项目id Map")
    @Path("/projectId/map")
    @POST
    Result<Map<Long, String>> getProjectIdMapByTaskId(
            @ApiParam(value = "批量查询参数", required = true)
            QueryTaskListReqVO reqVO
    );

    @ApiOperation("获取指定已下架工具的任务Id")
    @Path("/tool/taskIdMap")
    @POST
    Result<Map<String, Set<Long>>> queryTaskIdByWithdrawTool(
            @ApiParam(value = "工具集合", required = true)
            Set<String> toolSet
    );

    @ApiOperation("获取任务总数")
    @Path("/task/count")
    @GET
    Result<Long> countTaskSize();

    @ApiOperation("获取任务总数")
    @Path("/task/page")
    @GET
    Result<List<TaskDetailVO>> getTaskIdByPage(
            @ApiParam(value = "第几页")
            @QueryParam("page")
            Integer page,
            @ApiParam(value = "页数")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @ApiOperation("定时任务统计任务数、代码行、工具数")
    @Path("/statistic/taskCodeLineTool")
    @POST
    Result<Boolean> statisticTaskCodeLineTool(
            @ApiParam(value = "统计任务数、代码行、工具数请求体", required = true)
            StatisticTaskCodeLineToolVO reqVO
    );

    @ApiOperation("获取项目id下的任务id")
    @Path("/projectId/{projectId}")
    @GET
    Result<List<TaskBaseVO>> getTaskListByProjectId(
            @ApiParam(value = "批量查询参数", required = true)
            @PathParam("projectId")
            String projectId
    );


    @ApiOperation("分页获取有效任务的projectId")
    @Path("/projectId/pageList")
    @POST
    Result<List<String>> queryProjectIdPage(
            @ApiParam(value = "任务创建来源", required = true)
            Set<String> createFrom,
            @ApiParam(value = "页码")
            @QueryParam("pageNum")
            Integer pageNum,
            @ApiParam(value = "每页数量")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @ApiOperation("按项目id分页获取有效任务id")
    @Path("/projectId/{projectId}/taskIdPageList")
    @GET
    Result<List<Long>> queryTaskIdPageByProjectId(
            @ApiParam(value = "项目id", required = true)
            @PathParam("projectId")
            String projectId,
            @ApiParam(value = "页码，从1开始算")
            @QueryParam("pageNum")
            Integer pageNum,
            @ApiParam(value = "每页数量")
            @QueryParam("pageSize")
            Integer pageSize
    );

    @ApiOperation("按项目id分页获取有效任务id")
    @Path("/queryTaskIdByProjectIdWithPermission")
    @GET
    Result<List<Long>> queryTaskIdByProjectIdWithPermission(
            @ApiParam(value = "项目Id", required = true)
            @QueryParam("projectId")
            String projectId,
            @ApiParam(value = "用户Id", required = true)
            @QueryParam("userId")
            String userId
    );

    @ApiOperation("根据任务Id以及维度获取工具名字")
    @Path("/getTaskInfoWithSortedToolConfig")
    @POST
    Result<TaskInfoWithSortedToolConfigResponse> getTaskInfoWithSortedToolConfig(
            TaskInfoWithSortedToolConfigRequest request
    );

    @ApiOperation("获取部分任务信息，数据迁移专用")
    @Path("/getTaskInfoForDataMigration")
    @GET
    Result<List<TaskBaseVO>> getTaskInfoForDataMigration(
            @ApiParam(value = "任务Id，锚点", required = true)
            @QueryParam("lastTaskId")
            Long lastTaskId,
            @ApiParam(value = "往后取多少条")
            @QueryParam("limit")
            Integer limit
    );

    @ApiOperation("特定资源列表")
    @Path("/instances/list")
    @POST
    Result<String> resourceList(
            @ApiParam(value = "回调信息", required = true) CallbackRequestDTO callBackInfo
    );

    @ApiOperation("获取最后一次构建Id")
    @Path("/getLatestBuildId")
    @GET
    Result<String> getLatestBuildId(
            @ApiParam(value = "任务Id", required = true)
            @QueryParam("taskId")
            Long taskId
    );

    @ApiOperation("获取最后一次构建Id")
    @Path("/latestBuildIdMap")
    @POST
    Result<Map<Long, String>> latestBuildIdMap(
            @ApiParam(value = "任务Id", required = true)
            GetLatestBuildIdMapRequest request
    );

    @ApiOperation("设置任务状态为冷")
    @Path("/setTaskToColdFlag")
    @POST
    Result<Boolean> setTaskToColdFlag(
            @ApiParam(value = "任务Id", required = true)
            @QueryParam("taskId")
            Long taskId
    );

    @ApiOperation("设置任务状态为正常")
    @Path("/setTaskToEnableFlag")
    @POST
    Result<Boolean> setTaskToEnableFlag(
            @ApiParam(value = "任务Id", required = true)
            @QueryParam("taskId")
            Long taskId
    );

    @ApiOperation("获取任务Id，冷热分离专用")
    @Path("/getTaskIdListForHotColdDataSeparation")
    @GET
    Result<List<Long>> getTaskIdListForHotColdDataSeparation(
            @ApiParam(value = "任务Id，锚点", required = true)
            @QueryParam("lastTaskId")
            Long lastTaskId,
            @ApiParam(value = "往后取多少条")
            @QueryParam("limit")
            Integer limit
    );

    @ApiOperation("获取任务的工具名称列表")
    @Path("/getTaskToolList")
    @GET
    Result<List<String>> getTaskToolNameList(
            @ApiParam(value = "任务Id", required = true)
            @QueryParam("taskId")
            Long taskId
    );

    @ApiOperation("获取任务状态")
    @Path("/getTaskStatus")
    @GET
    Result<GetTaskStatusAndCreateFromResponse> getTaskStatusAndCreateFrom(
            @ApiParam(value = "任务Id", required = true)
            @QueryParam("taskId")
            Long taskId
    );

    @ApiOperation("根据bgId和deptId对任务信息进行分组统计")
    @Path("/taskStatisticWithBgIdAndDeptId")
    @POST
    Result<List<TaskStatisticVO>> getTaskStatisticByIds(
            @ApiParam(value = "任务ID清单", required = true)
            List<Long> taskIds);


    @ApiOperation("根据bgId和deptId对任务信息进行分组统计")
    @Path("/taskIdNeProjectIdWithPage")
    @POST
    Result<List<Long>> getTaskIdNeProjectIdWithPage(
            @ApiParam(value = "过滤的项目id", required = true)
            @QueryParam("filterProjectId")
            String filterProjectId,
            @ApiParam(value = "页码")
            @QueryParam("pageNum")
            Integer pageNum,
            @ApiParam(value = "每页数量")
            @QueryParam("pageSize")
            Integer pageSize);
}
