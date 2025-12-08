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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
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
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.condition.CommunityCondition;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;

/**
 * 服务间任务管理接口
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Slf4j
@RestResource
@Conditional(CommunityCondition.class)
public class ServiceTaskRestResourceImpl implements ServiceTaskRestResource {

    @Autowired
    private TaskService taskService;

    @Autowired
    @Qualifier("pipelineTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private PathFilterService pathFilterService;

    @Override
    public Result<TaskDetailVO> getTaskInfo(String nameEn) {
        return new Result<>(taskService.getTaskInfoByStreamName(nameEn));
    }

    @Override
    public Result<TaskBaseVO> getTaskToolList(long taskId) {
        return new Result<>(taskService.getTaskToolList(taskId));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoById(Long taskId) {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public Result<List<TaskBaseVO>> getTaskInfosByIds(List<Long> taskIds) {
        return new Result<>(taskService.getTasksByIds(taskIds));
    }

    @Override
    public Result<List<TaskDetailVO>> getTaskDetailListByIdsWithDelete(QueryTaskListReqVO reqVO) {
        return new Result<>(taskService.getTaskDetailListByIdsWithDelete(reqVO));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoWithoutToolsByTaskId(Long taskId) {
        return new Result<>(taskService.getTaskInfoWithoutToolsByTaskId(taskId));
    }

    @Override
    public Result<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName) {
        log.info("upadte pipeline task request body: {}, username: {}",
                JsonUtil.INSTANCE.toJson(taskDetailVO), userName);
        return new Result<>(taskRegisterService.updateTask(taskDetailVO, userName));
    }

    @Override
    public Result<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName) {
        taskDetailVO.setProjectId(projectId);
        log.info("registerPipelineTask request body: {}", JsonUtil.INSTANCE.toJson(taskDetailVO));
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName) {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public Result<Boolean> stopTaskByPipeline(String pipelineId, String disabledReason, String userName) {
        return new Result<>(taskService.stopTask(pipelineId, disabledReason, userName));
    }


    @Override
    public Result<Boolean> stopSingleTaskByPipeline(String pipelineId, String multiPipelineMark,
            String disabledReason, String userName, String asyncTaskId) {
        return new Result<>(taskService.stopSinglePipelineTask(pipelineId, multiPipelineMark, disabledReason,
                userName, asyncTaskId));
    }

    @Override
    public Result<Boolean> checkTaskExists(Long taskId) {
        return new Result<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public Result<Map<String, ToolMetaBaseVO>> getToolMetaListFromCache() {
        return new Result<>(taskService.getToolMetaListFromCache());
    }

    @Override
    public Result<PipelineTaskVO> getPipelineTask(String pipelineId, String multiPipelineMark, String user) {
        return new Result<>(taskService.getTaskInfoByPipelineId(pipelineId, multiPipelineMark, user));
    }

    @Override
    public Result<List<PipelineTaskVO>> getPipelineAllTask(String pipelineId, String user) {
        return new Result<>(taskService.getTaskInfoByPipelineId(pipelineId, user));
    }

    @Override
    public Result<List<Long>> getPipelineAllTaskId(String pipelineId, String user) {
        return new Result<>(taskService.getTaskIdsByPipelineId(pipelineId, user));
    }

    @Override
    public Result<Long> getTaskIdByPipelineInfo(String pipelineId, String multiPipelineMark) {
        return new Result<>(taskService.getTaskIdByPipelineInfo(pipelineId, multiPipelineMark));
    }

    @Override
    public Result<Set<String>> queryTaskListByPipelineIds(Set<String> pipelineIds) {
        return new Result<>(authTaskService.queryTaskListByPipelineIds(pipelineIds));
    }


    @Override
    public Result<TaskListVO> getTaskList(String projectId, String user) {
        return new Result<>(taskService.getTaskList(projectId, user, TaskSortType.CREATE_DATE, null));
    }

    @Override
    public Result<List<TaskBaseVO>> getTasksByBgId(Integer bgId) {
        return new Result<>(taskService.getTasksByBgId(bgId));
    }

    @Override
    public Result<TaskListVO> getTaskDetailList(QueryTaskListReqVO taskListReqVO) {
        return new Result<>(taskService.getTaskDetailList(taskListReqVO));
    }

    @Override
    public Result<Page<TaskInfoVO>> getTasksByAuthor(QueryMyTasksReqVO reqVO) {
        return new Result<>(taskService.getTasksByAuthor(reqVO));
    }

    /**
     * 即将废弃 - 逻辑先注释
     *
     * @param user
     * @param projectId
     * @param pipelineId
     * @param taskId
     * @param updateCheckerSet2TaskReqVO
     * @return
     */
    @Override
    @Deprecated
    public Result<Boolean> updatePipelineTaskCheckerSets(String user, String projectId, String pipelineId, Long taskId,
            UpdateCheckerSet2TaskReqVO updateCheckerSet2TaskReqVO) {
        return new Result<>(pipelineService.updateCheckerSets(user, projectId, pipelineId, taskId,
                updateCheckerSet2TaskReqVO.getToolCheckerSets()));
    }

    @Override
    public Result<Set<Integer>> queryDeptIdByBgId(Integer bgId) {
        Set<Integer> deptIdSet = taskService.queryDeptIdByBgId(bgId);
        return new Result<>(deptIdSet);
    }

    @Override
    public Result<List<TaskDetailVO>> batchGetTaskList(QueryTaskListReqVO queryTaskListReqVO) {
        return new Result<>(taskService.getTaskInfoList(queryTaskListReqVO));
    }


    @Override
    public Result<FilterPathOutVO> filterPath(Long taskId) {
        return new Result<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public Result<Page<TaskDetailVO>> getTaskDetailPage(QueryTaskListReqVO reqVO) {
        return new Result<>(taskService.getTaskDetailPage(reqVO));
    }

    @Override
    public Result<Boolean> authorTransfer(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
            String userId) {
        taskService.authorTransferForApi(taskId, transferAuthorPairs, userId);
        return new Result<>(true);
    }

    @Override
    public Result<List<Long>> queryTaskIdByCreateFrom(List<String> createFrom) {
        return new Result<>(taskService.queryTaskIdByCreateFrom(createFrom));
    }

    @Override
    public Result<List<Long>> queryTaskIdByCreateFromExcludeGray(List<String> createFrom, Long lastTaskId,
            Integer pageSize) {
        return new Result<>(Collections.emptyList());
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoWithoutToolsByStreamName(String nameEn) {
        return new Result<>(taskService.getTaskInfoWithoutToolsByStreamName(nameEn));
    }

    @Override
    public Result<Map<Long, String>> getProjectIdMapByTaskId(QueryTaskListReqVO reqVO) {
        return new Result<>(taskService.getProjectIdMapByTaskId(reqVO));
    }

    @Override
    public Result<Map<String, Set<Long>>> queryTaskIdByWithdrawTool(Set<String> toolSet) {
        return new Result<>(taskService.queryTaskIdByWithdrawTool(toolSet));
    }

    @Override
    public Result<Long> countTaskSize() {
        return new Result<>(taskService.countTaskSize());
    }

    @Override
    public Result<List<TaskDetailVO>> getTaskIdByPage(Integer page, Integer pageSize) {
        return new Result<>(taskService.getTaskIdByPage(page, pageSize));
    }

    @Override
    public Result<Boolean> statisticTaskCodeLineTool(StatisticTaskCodeLineToolVO reqVO) {
        return new Result<>(true);
    }

    @Override
    public Result<List<TaskBaseVO>> getTaskListByProjectId(String projectId) {
        return new Result<>(taskService.queryTaskListByProjectId(projectId));
    }

    @Override
    public Result<List<Long>> getTaskIdsByProjectId(String projectId) {
        return new Result<>(taskService.queryTasIdByProjectId(projectId));
    }

    @Override
    public Result<List<Long>> getAllTaskIdsByProjectId(String projectId) {
        return new Result<>(taskService.queryAllTaskIdByProjectId(projectId));
    }

    @Override
    public Result<List<String>> getAllPipelineIdsByProjectId(String projectId) {
        return new Result<>(taskService.queryAllPipelineIdByProjectId(projectId));
    }

    @Override
    public Result<List<String>> queryProjectIdPage(Set<String> createFrom, Integer pageNum, Integer pageSize) {
        return new Result<>(taskService.queryProjectIdPage(createFrom, pageNum, pageSize));
    }

    @Override
    public Result<List<Long>> queryTaskIdPageByProjectId(String projectId, Integer pageNum, Integer pageSize) {
        return new Result<>(taskService.queryTaskIdPageByProjectId(projectId, pageNum, pageSize));
    }

    @Override
    public Result<List<Long>> queryTaskIdByProjectIdWithPermission(String projectId, String userId) {
        return new Result<>(taskService.queryTaskIdByProjectIdWithPermission(projectId, userId));
    }

    @Override
    public Result<TaskInfoWithSortedToolConfigResponse> getTaskInfoWithSortedToolConfig(
            TaskInfoWithSortedToolConfigRequest request
    ) {
        return new Result<>(taskService.getTaskInfoWithSortedToolConfig(request));
    }

    @Override
    public Result<List<TaskBaseVO>> getTaskInfoForDataMigration(Long lastTaskId, Integer limit) {
        return new Result<>(taskService.getTaskIdAndCreateFromWithPage(lastTaskId, limit));
    }

    @Override
    public Result<String> resourceList(CallbackRequestDTO callBackInfo) {
        return Result.success(taskService.getInstanceByResource(callBackInfo));
    }

    @Override
    public Result<String> getLatestBuildId(Long taskId) {
        return Result.success(taskService.getLatestBuildId(taskId));
    }

    @Override
    public Result<Map<Long, String>> latestBuildIdMap(GetLatestBuildIdMapRequest request) {
        return Result.success(taskService.getLatestBuildIdMap(request.getTaskIdList()));
    }

    @Override
    public Result<Boolean> setTaskToColdFlag(Long taskId) {
        taskService.setTaskToColdFlag(taskId);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setTaskToEnableFlag(Long taskId) {
        taskService.setTaskToEnableFlag(taskId);
        return new Result<>(true);
    }

    @Override
    public Result<List<Long>> getTaskIdListForHotColdDataSeparation(Long lastTaskId, Integer limit) {
        return new Result<>(taskService.getTaskIdListForHotColdDataSeparation(lastTaskId, limit));
    }

    @Override
    public Result<List<String>> getTaskToolNameList(Long taskId) {
        return new Result<>(taskService.getTaskToolNameList(taskId));
    }

    @Override
    public Result<GetTaskStatusAndCreateFromResponse> getTaskStatusAndCreateFrom(Long taskId) {
        return new Result<>(taskService.getTaskStatusAndCreateFrom(taskId));
    }

    @Override
    public Result<List<TaskStatisticVO>> getTaskStatisticByIds(List<Long> taskIds) {
        return new Result<>(taskService.getTaskStatisticByIds(taskIds));
    }

    @Override
    public Result<List<Long>> getTaskIdNeProjectIdWithPage(String filterProjectId, Integer pageNum,
            Integer pageSize) {
        return new Result<>(taskService.getTaskIdNeProjectIdWithPage(filterProjectId, pageNum, pageSize));
    }

    @Override
    public Result<Boolean> stopDisableProjectTask(String projectId) {
        taskService.stopDisableProjectTask(projectId);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> startEnableProjectTask(String projectId) {
        taskService.startEnableProjectTask(projectId);
        return new Result<>(true);
    }

    @Override
    public Result<Page<Long>> getTaskInfoByCreateFrom(String taskType, CommonPageVO reqVO) {
        return new Result<>(null);
    }
}
