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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.BuildTaskRestResource;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.PathFilterService;
import com.tencent.bk.codecc.task.service.PipelineCallbackRegisterService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
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
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 构建机任务接口实现
 *
 * @version V1.0
 * @date 2019/7/21
 */
@Slf4j
@RestResource
public class BuildTaskRestResourceImpl implements BuildTaskRestResource {

    @Autowired
    private TaskService taskService;

    @Autowired
    @Qualifier("pipelineTaskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PathFilterService pathFilterService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private PipelineCallbackRegisterService pipelineCallbackRegisterService;

    @Override
    public Result<TaskDetailVO> getTaskInfoById(Long taskId) {
        return new Result<>(taskService.getTaskInfoById(taskId));
    }

    @Override
    public Result<TaskDetailVO> getTaskInfoByStreamName(String streamName) {
        return new Result<>(taskService.getTaskInfoByStreamName(streamName));
    }

    @Override
    public Result<PipelineTaskVO> getTaskInfoByPipelineId(String pipelineId, String multiPipelineMark, String userId) {
        return new Result<>(taskService.getTaskInfoByPipelineId(pipelineId, multiPipelineMark, userId));
    }

    @Override
    public Result<TaskIdVO> registerPipelineTask(TaskDetailVO taskDetailVO, String projectId, String userName) {
        taskDetailVO.setProjectId(projectId);
        log.info("registerPipelineTask request body: {}", JsonUtil.INSTANCE.toJson(taskDetailVO));
        return new Result<>(taskRegisterService.registerTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> updateTask(TaskDetailVO taskDetailVO, String userName) {
        log.info("upadte pipeline task request body: {}, username: {}", JsonUtil.INSTANCE.toJson(taskDetailVO),
                userName);
        return new Result<>(taskRegisterService.updateTask(taskDetailVO, userName));
    }

    @Override
    public Result<Boolean> stopTask(Long taskId, String disabledReason, String userName) {
        return new Result<>(taskService.stopTask(taskId, disabledReason, userName));
    }

    @Override
    public Result<Boolean> checkTaskExists(Long taskId) {
        return new Result<>(taskService.checkTaskExists(taskId));
    }

    @Override
    public Result<Boolean> addFilterPath(FilterPathInputVO filterPathInput, String userName) {
        return new Result<>(pathFilterService.addFilterPaths(filterPathInput, userName));
    }

    @Override
    public Result<Boolean> deleteFilterPath(String path, String pathType, Long taskId, String userName) {
        return new Result<>(pathFilterService.deleteFilterPath(path, pathType, taskId, userName));
    }

    @Override
    public Result<FilterPathOutVO> filterPath(Long taskId) {
        return new Result<>(pathFilterService.getFilterPath(taskId));
    }

    @Override
    public Result<Boolean> codeYmlFilterPath(Long taskId, String userName, CodeYmlFilterPathVO codeYmlFilterPathVO) {
        return new Result<>(pathFilterService.codeYmlFilterPath(taskId, userName, codeYmlFilterPathVO));
    }

    @Override
    public Result<Boolean> codeYmlRepoOwner(Long taskId, String userName, CodeYmlRepoOwnerVO codeYmlRepoOwnerVO) {
        return new Result<>(taskService.updateTaskRepoOwner(taskId, codeYmlRepoOwnerVO.getRepoOwners(), userName));
    }

    @Override
    public Result<Boolean> runtimeMetaUpdate(Long taskId, String userName, RuntimeUpdateMetaVO runtimeUpdateMetaVO) {
        return new Result<>(taskService.updateRuntimeInfo(taskId, runtimeUpdateMetaVO, userName));
    }

    @Override
    public Result<Boolean> updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO) {
        return new Result<>(taskService.updateScanConfiguration(taskId, user, scanConfigurationVO));
    }

    @Override
    public Result<Boolean> updateTaskReportInfo(Long taskId, NotifyCustomVO notifyCustomVO) {
        taskService.updateReportInfo(taskId, notifyCustomVO);
        return new Result<>(true);
    }

    @Override
    public Result<ToolConfigPlatformVO> getToolConfigInfo(Long taskId, String toolName) {
        return new Result<>(toolService.getToolConfigPlatformInfo(taskId, toolName));
    }

    @Override
    public Result<Boolean> sendStartTaskSignal(Long taskId, String buildId, Integer timeout) {
        return new Result<>(taskService.sendStartTaskSignal(taskId, buildId, timeout));
    }

    @Override
    public Result<Boolean> executeTask(long taskId, String isFirstTrigger,
            String userName) {
        return new Result<>(taskService.manualExecuteTask(taskId, isFirstTrigger, userName));
    }

    @Override
    public Result<Boolean> addWhitePath(String userName, long taskId, List<String> pathList) {
        return new Result<>(taskService.addWhitePath(taskId, pathList));
    }

    @Override
    public Result<Boolean> registerPipelineCallback(String userName, long taskId, String buildId) {
        TaskInfoEntity taskInfoEntity = taskService.getTaskById(taskId);
        if (taskInfoEntity == null) {
            return new Result<>(false);
        }
        if (!pipelineCallbackRegisterService.checkIfRegisterBuildEndCallBack(taskInfoEntity.getPipelineId())) {
            pipelineCallbackRegisterService.registerBuildEndCallback(taskInfoEntity.getProjectId(),
                    taskInfoEntity.getPipelineId(), userName);
        }
        return new Result<>(true);
    }

}
