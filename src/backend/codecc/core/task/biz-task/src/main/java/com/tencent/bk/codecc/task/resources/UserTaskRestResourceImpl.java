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

package com.tencent.bk.codecc.task.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.api.UserTaskRestResource;
import com.tencent.bk.codecc.task.enums.EmailType;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.FilterPathOutVO;
import com.tencent.bk.codecc.task.vo.ListTaskNameCnRequest;
import com.tencent.bk.codecc.task.vo.ListTaskNameCnResponse;
import com.tencent.bk.codecc.task.vo.ListTaskToolDimensionRequest;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.TreeNodeTaskVO;
import com.tencent.bk.codecc.task.vo.path.CodeYmlFilterPathVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.devops.common.api.RtxNotifyVO;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.condition.CommunityCondition;
import com.tencent.devops.common.web.security.AuthMethod;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;

import java.util.List;

/**
 * 任务清单资源实现
 *
 * @version V1.0
 * @date 2019/4/23
 */
@RestResource
@Slf4j
@Conditional(CommunityCondition.class)
public class UserTaskRestResourceImpl implements UserTaskRestResource {
    @Autowired
    private TaskService taskService;

    @Autowired
    @Qualifier("devopsTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PathFilterService pathFilterService;

    @Autowired
    private EmailNotifyService emailNotifyService;
    @Autowired
    private Client client;

    @Override
    public Result<TaskListVO> getTaskList(String projectId, String user, TaskSortType taskSortType, TaskListReqVO taskListReqVO) {
        return new Result<>(taskService.getTaskList(projectId, user, taskSortType, taskListReqVO));
    }

    @Override
    public Result<Boolean> testEx() {
        return null;
    }

    @Override
    public Result<Boolean> triggerNotify(Long taskId, Integer type) {
        if (type.equals(1)) {
            emailNotifyService.sendReport(new EmailNotifyModel(taskId, null, EmailType.INSTANT));
        } else {
            if (type.equals(2)) {
                emailNotifyService.sendRtx(new RtxNotifyModel(taskId, true, null));
            } else {
                if (type.equals(3)) {
                    emailNotifyService.sendRtx(new RtxNotifyModel(taskId, false, null));
                }
            }
        }

        return new Result<>(true);
    }

    @Override
    public Result<TaskListVO> getTaskBaseList(String projectId, String user) {
        return new Result<>(taskService.getTaskBaseList(projectId, user));
    }

    @Override
    public Result<TaskBaseVO> getTaskInfo() {
        return new Result<>(taskService.getTaskInfo());
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.REPORT_VIEW})
    public Result<TaskDetailVO> getTask(Long taskId) {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public Result<TaskOverviewVO> getTaskOverview(Long taskId, String buildNum, String orderBy) {
        return new Result<>(taskService.getTaskOverview(taskId, buildNum, orderBy));
    }


    @Override
    public Result<TaskIdVO> registerDevopsTask(TaskDetailVO taskDetailVO, String projectId, String userName) {
        taskDetailVO.setProjectId(projectId);
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    //    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO) {
        return new Result<>(taskService.updateScanConfiguration(taskId, user, scanConfigurationVO));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String projectId, String userName) {
        return new Result<>(taskService.updateTask(taskUpdateVO, taskId, userName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> addFilterPath(FilterPathInputVO filterPathInput, String userName) {
        return new Result<>(pathFilterService.addFilterPaths(filterPathInput, userName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> deleteFilterPath(String path, String pathType, Long taskId, String userName) {
        return new Result<>(pathFilterService.deleteFilterPath(path, pathType, taskId, userName));
    }

    @Override
    public Result<FilterPathOutVO> filterPath(Long taskId) {
        return new Result<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public Result<TreeNodeTaskVO> filterPathTree(Long taskId) {
        return new Result<>(pathFilterService.filterPathTree(taskId));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> startTask(Long taskId, String userName) {
        return new Result<>(taskService.startTask(taskId, userName));
    }

    @Override
    public Result<TaskStatusVO> getTaskStatus(Long taskId) {
        return new Result<>(taskService.getTaskStatus(taskId));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName) {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public Result<TaskCodeLibraryVO> getCodeLibrary(Long taskId) {
        return new Result<>(taskService.getCodeLibrary(taskId));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) throws JsonProcessingException {
        return new Result<>(taskService.updateCodeLibrary(taskId, userName, taskDetailVO));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.ANALYZE})
    public Result<Boolean> executeTask(long taskId, String isFirstTrigger,
                                                                          String userName) {
        return new Result<>(taskService.manualExecuteTask(taskId, isFirstTrigger, userName));
    }

    @Override
    public Result<TaskMemberVO> getTaskUsers(long taskId, String projectId) {
        return new Result<>(taskService.getTaskUsers(taskId, projectId));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateTaskReportInfo(Long taskId, NotifyCustomVO notifyCustomVO) {
        taskService.updateReportInfo(taskId, notifyCustomVO);
        return new Result<>(true);
    }


    @Override
    public Result<Boolean> updateTopUserInfo(String user, Long taskId, Boolean topFlag) {
        return new Result<>(taskService.updateTopUserInfo(taskId, user, topFlag));
    }

    @Override
    public Result<Boolean> syncKafkaTaskInfo(String dataType, String washTime) {
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> manualTriggerPipeline(List<Long> taskIdList) {
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> updateTaskOwnerAndMember(TaskOwnerAndMemberVO vo, Long taskId) {
        taskService.updateTaskOwnerAndMember(vo, taskId);

        return new Result<>(true);
    }

    @Override
    public Result<CodeYmlFilterPathVO> listCodeYmlFilterPath(Long taskId) {
        return new Result<>(pathFilterService.listCodeYmlFilterPath(taskId));
    }

    @Override
    public Result<Boolean> triggerBkPluginScoring() {
        return new Result<>(true);
    }

    @Override
    public Result<List<MetadataVO>> listTaskToolDimension(Long taskId) {
        return new Result<>(taskService.listTaskToolDimension(Lists.newArrayList(taskId), ""));
    }

    @Override
    @I18NResponse
    public Result<List<MetadataVO>> listTaskToolDimension(String projectId, ListTaskToolDimensionRequest request) {
        return new Result<>(taskService.listTaskToolDimension(request.getTaskIdList(), projectId));
    }

    @Override
    public Result<Boolean> codeCommentSendRtx(RtxNotifyVO rtxNotifyVO) {
        return new Result<>(true);
    }

    @Override
    public Result<List<TaskBaseVO>> listTaskBase(String userId, String projectId) {
        return new Result<>(taskService.listTaskBase(userId, projectId));
    }

    @Override
    public Result<ListTaskNameCnResponse> listTaskNameCn(ListTaskNameCnRequest request) {
        Map<Long, String> data = taskService.listTaskNameCn(request.getTaskIdList());

        return new Result<>(new ListTaskNameCnResponse(data));
    }

    @Override
    public Result<Boolean> multiTaskVisitable(String projectId) {
        return new Result<>(taskService.multiTaskVisitable(projectId));
    }

    @Override
    public Result<Boolean> isRbacPermission(String projectId, Boolean needRefresh) {
        return new Result<>(false);
    }
}
