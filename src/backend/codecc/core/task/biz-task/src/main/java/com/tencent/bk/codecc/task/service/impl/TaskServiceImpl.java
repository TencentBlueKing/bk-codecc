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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;
import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import static com.tencent.devops.common.constant.ComConstants.CLEAN_TASK_STATUS;
import static com.tencent.devops.common.constant.ComConstants.CLEAN_TASK_WHITE_LIST;
import static com.tencent.devops.common.constant.ComConstants.DISABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.ENABLE_ACTION;
import static com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CODE_REPOSITORY;
import static com.tencent.devops.common.constant.ComConstants.FUNC_SCAN_SCHEDULE;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_INFO;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TASK_SWITCH;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TRIGGER_ANALYSIS;
import static com.tencent.devops.common.constant.ComConstants.MODIFY_INFO;
import static com.tencent.devops.common.constant.ComConstants.Status;
import static com.tencent.devops.common.constant.ComConstants.Step4MutliTool;
import static com.tencent.devops.common.constant.ComConstants.StepStatus;
import static com.tencent.devops.common.constant.ComConstants.TOOL_LICENSE_WHITE_LIST;
import static com.tencent.devops.common.constant.ComConstants.TRIGGER_ANALYSIS;
import static com.tencent.devops.common.constant.ComConstants.Tool;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_TOOL_PARAMS_LABEL_NAME;
import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_TOOL_PARAMS_TIPS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXPIRED_TASK_STATUS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXTERNAL_JOB;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SCORING_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_EXPIRED_TASK_STATUS;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SCORING_OPENSOURCE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.api.ServiceClusterStatisticRestReource;
import com.tencent.bk.codecc.defect.api.ServiceMetricsRestResource;
import com.tencent.bk.codecc.defect.api.ServiceReportDefectRestResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerDimensionRequest;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolConfigRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.enums.TaskSortType;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.DisableTaskEntity;
import com.tencent.bk.codecc.task.model.NotifyCustomEntity;
import com.tencent.bk.codecc.task.model.TaskIdInfo;
import com.tencent.bk.codecc.task.model.TaskIdToolInfoEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.TaskStatisticEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import com.tencent.bk.codecc.task.service.IAuthorTransferBizService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.CodeLibraryInfoVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.NotifyCustomVO;
import com.tencent.bk.codecc.task.vo.RepoInfoVO;
import com.tencent.bk.codecc.task.vo.RuntimeUpdateMetaVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigRequest;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse.TaskBase;
import com.tencent.bk.codecc.task.vo.TaskListReqVO;
import com.tencent.bk.codecc.task.vo.TaskListVO;
import com.tencent.bk.codecc.task.vo.TaskMemberVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO.LastAnalysis;
import com.tencent.bk.codecc.task.vo.TaskOwnerAndMemberVO;
import com.tencent.bk.codecc.task.vo.TaskStatusVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateDeptInfoVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigParamJsonVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineToolParamVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineToolVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.ScanConfigurationVO;
import com.tencent.bk.codecc.task.vo.scanconfiguration.TimeAnalysisConfigVO;
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO;
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.GetLastAnalysisResultsVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.StatisticTaskCodeLineToolVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO;
import com.tencent.devops.common.api.clusterresult.CcnClusterResultVO;
import com.tencent.devops.common.api.clusterresult.DefectClusterResultVO;
import com.tencent.devops.common.api.clusterresult.DupcClusterResultVO;
import com.tencent.devops.common.api.clusterresult.SecurityClusterResultVO;
import com.tencent.devops.common.api.clusterresult.StandardClusterResultVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi;
import com.tencent.devops.common.auth.api.pojo.external.AuthRole;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.auth.api.util.PermissionUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 任务服务实现类
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private AuthExRegisterApi authExRegisterApi;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    private EmailNotifyService emailNotifyService;

    @Autowired
    private IAuthorTransferBizService authorTransferBizService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private CommonDao commonDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private Client client;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    private ToolDao toolDao;
    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private TaskStatisticRepository taskStatisticRepository;

    @Autowired
    private BaseDataService baseDataService;

    @Autowired
    private ToolConfigRepository toolConfigRepository;

    private LoadingCache<String, BaseDataEntity> toolTypeBaseDataCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseDataEntity>() {
                @Override
                public BaseDataEntity load(String toolName) {
                    if (StringUtils.isBlank(toolName)) {
                        return null;
                    }
                    String toolType = toolMetaCache.getToolBaseMetaCache(toolName).getType();
                    return baseDataRepository.findFirstByParamTypeAndParamCode("TOOL_TYPE", toolType);
                }
            });

    /**
     * 工具许可项目白名单缓存(即只有指定的项目才能使用该工具，用于某些收费工具对特定项目使用)
     */
    private LoadingCache<String, Set<String>> toolLicenseWhiteListCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String toolName) {
                    Set<String> projectIdSet = Sets.newHashSet();
                    if (StringUtils.isNotEmpty(toolName)) {
                        BaseDataEntity baseData =
                                baseDataRepository.findFirstByParamTypeAndParamCode(TOOL_LICENSE_WHITE_LIST, toolName);
                        if (baseData != null && StringUtils.isNotEmpty(baseData.getParamValue())) {
                            String toolSetStr = baseData.getParamValue();
                            projectIdSet.addAll(List2StrUtil.fromString(toolSetStr, ComConstants.STRING_SPLIT));
                        }
                    }
                    return projectIdSet;
                }
            });

    private LoadingCache<String, BaseDataEntity> toolDimensionBaseDataCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseDataEntity>() {
                @Override
                public BaseDataEntity load(String dimension) {
                    if (StringUtils.isBlank(dimension)) {
                        return null;
                    }
                    return baseDataRepository.findFirstByParamTypeAndParamCode("TOOL_TYPE", dimension);
                }
            });

    @Override
    public TaskListVO getTaskList(String projectId, String user, TaskSortType taskSortType,
            TaskListReqVO taskListReqVO) {

        List<TaskInfoEntity> resultTasks = getQualifiedTaskList(projectId, user, null,
                null != taskListReqVO ? taskListReqVO.getTaskSource() : null);

        final String toolIdsOrder = commonDao.getToolOrder();

        List<TaskDetailVO> taskDetailVOList = resultTasks.stream().filter(taskInfoEntity ->
                        StringUtils.isNotEmpty(taskInfoEntity.getToolNames())
                                //流水线停用任务不展示
                                && !(taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())
                                && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                                .equalsIgnoreCase(taskInfoEntity.getCreateFrom())))
                .map(taskInfoEntity -> {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
                    taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
                    return taskDetailVO;
                }).collect(Collectors.toList());

        Result<Map<String, List<ToolLastAnalysisResultVO>>> taskAndTaskLogResult =
                client.get(ServiceTaskLogRestResource.class)
                        .getBatchTaskLatestTaskLog(taskDetailVOList);
        Map<String, List<ToolLastAnalysisResultVO>> taskAndTaskLogMap;
        if (taskAndTaskLogResult.isOk() && MapUtils.isNotEmpty(taskAndTaskLogResult.getData())) {
            taskAndTaskLogMap = taskAndTaskLogResult.getData();
        } else {
            log.error("get batch task log fail or task log is empty!");
            taskAndTaskLogMap = new HashMap<>();
        }

        // 查询用户有权限的流水线
        Set<String> pipelines = authExPermissionApi.queryPipelineListForUser(user, projectId,
                Sets.newHashSet(PipelineAuthAction.EXECUTE.getActionName()));

        //对工具清单进行处理
        List<TaskDetailVO> taskDetailVOS = resultTasks.stream().filter(taskInfoEntity ->
                        //流水线停用任务不展示
                        !(taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())
                                && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                                .equalsIgnoreCase(taskInfoEntity.getCreateFrom())))
                .map(taskInfoEntity -> {
                    TaskDetailVO taskDetailVO = new TaskDetailVO();
                    BeanUtils.copyProperties(taskInfoEntity, taskDetailVO, "toolConfigInfoList");
                    //设置置顶标识
                    Set<String> topUsers = taskInfoEntity.getTopUser();
                    if (CollectionUtils.isNotEmpty(topUsers) && topUsers.contains(user)) {
                        taskDetailVO.setTopFlag(1);
                    } else {
                        taskDetailVO.setTopFlag(-1);
                    }

                    //获取分析完成时间
                    List<ToolLastAnalysisResultVO> taskLogGroupVOs = new ArrayList<>();
                    String toolNames = taskInfoEntity.getToolNames();
                    if (StringUtils.isNotEmpty(toolNames)) {
                        if (MapUtils.isNotEmpty(taskAndTaskLogMap)) {
                            taskLogGroupVOs = taskAndTaskLogMap.get(String.valueOf(taskInfoEntity.getTaskId()));
                            if (null == taskLogGroupVOs) {
                                taskLogGroupVOs = new ArrayList<>();
                            }
                        }
                    }

                    taskDetailVO.setHasNoPermission(false);
                    if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                            .equalsIgnoreCase(taskDetailVO.getCreateFrom())) {
                        if (CollectionUtils.isEmpty(pipelines) || !pipelines.contains(taskDetailVO.getPipelineId())) {
                            taskDetailVO.setHasNoPermission(true);
                        }
                    }
                    List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
                    if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
                        List<ToolConfigInfoVO> toolConfigInfoVOList = new ArrayList<>();
                        boolean isAllSuspended = true;
                        Long minStartTime = Long.MAX_VALUE;
                        Boolean processFlag = false;
                        Integer totalFinishStep = 0;
                        Integer totalStep = 0;
                        for (ToolConfigInfoEntity toolConfigInfoEntity : toolConfigInfoEntityList) {
                            if (null == toolConfigInfoEntity
                                    || StringUtils.isEmpty(toolConfigInfoEntity.getToolName())) {
                                continue;
                            }

                            // 获取工具展示名称
                            ToolMetaBaseVO toolMetaBaseVO =
                                    toolMetaCache.getToolBaseMetaCache(toolConfigInfoEntity.getToolName());

                            if (toolConfigInfoEntity.getFollowStatus() != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
                                    && !ComConstants.ToolIntegratedStatus.D.name().equals(
                                    toolMetaBaseVO.getStatus())) {

                                //更新工具显示状态
                                //如果有失败的工具，则显示失败的状态
                                if (!processFlag) {
                                    processFlag = taskDetailDisplayInfo(toolConfigInfoEntity, taskDetailVO,
                                            toolMetaBaseVO.getDisplayName());
                                }
                                //添加进度条
                                totalFinishStep += toolConfigInfoEntity.getCurStep();
                                switch (toolMetaBaseVO.getPattern()) {
                                    case "LINT":
                                        totalStep += 5;
                                        break;
                                    case "CCN":
                                        totalStep += 5;
                                        break;
                                    case "DUPC":
                                        totalStep += 5;
                                        break;
                                    default:
                                        totalStep += 6;
                                        break;
                                }
                            }

                            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO);

                            //设置分析完成时间
                            for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : taskLogGroupVOs) {
                                if (toolLastAnalysisResultVO.getToolName()
                                        .equalsIgnoreCase(toolConfigInfoVO.getToolName())) {
                                    toolConfigInfoVO.setEndTime(toolLastAnalysisResultVO.getEndTime());
                                    toolConfigInfoVO.setStartTime(toolLastAnalysisResultVO.getStartTime());
                                }
                            }
                            minStartTime = Math.min(minStartTime, toolConfigInfoVO.getStartTime());

                            if (StringUtils.isNotEmpty(toolMetaBaseVO.getDisplayName())) {
                                toolConfigInfoVO.setDisplayName(toolMetaBaseVO.getDisplayName());
                            }

                            if (toolConfigInfoVO.getFollowStatus() != ComConstants.FOLLOW_STATUS.WITHDRAW.value()) {
                                isAllSuspended = false;
                            }
                            if (toolConfigInfoEntity.getCheckerSet() != null) {
                                ToolCheckerSetVO checkerSetVO = new ToolCheckerSetVO();
                                BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), checkerSetVO);
                                toolConfigInfoVO.setCheckerSet(checkerSetVO);
                            }
                            toolConfigInfoVOList.add(toolConfigInfoVO);
                        }
                        if (isAllSuspended) {
                            log.info("all tool is suspended! task id: {}", taskInfoEntity.getTaskId());
                            if (CollectionUtils.isNotEmpty(toolConfigInfoVOList)) {
                                toolConfigInfoVOList.get(0)
                                        .setFollowStatus(ComConstants.FOLLOW_STATUS.EXPERIENCE.value());
                            }
                        }
                        if (totalStep == 0) {
                            taskDetailVO.setDisplayProgress(0);
                        } else {
                            if (totalFinishStep > totalStep) {
                                totalFinishStep = totalStep;
                            }
                            taskDetailVO.setDisplayProgress(totalFinishStep * 100 / totalStep);
                        }
                        if (null == taskDetailVO.getDisplayStepStatus()) {
                            taskDetailVO.setDisplayStepStatus(ComConstants.StepStatus.SUCC.value());
                        }
                        if (minStartTime < Long.MAX_VALUE) {
                            taskDetailVO.setMinStartTime(minStartTime);
                        } else {
                            taskDetailVO.setMinStartTime(0L);
                        }
                        log.info("handle tool list finish! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(toolConfigInfoVOList);
                    } else {
                        log.info("tool list is empty! task id: {}", taskInfoEntity.getTaskId());
                        taskDetailVO.setToolConfigInfoList(new ArrayList<>());
                        taskDetailVO.setMinStartTime(0L);
                    }

                    List<ToolConfigInfoVO> toolConfigInfoVOs = new ArrayList<>();
                    //重置工具顺序，并且对工具清单顺序也进行重排
                    taskDetailVO.setToolNames(resetToolOrderByType(taskDetailVO.getToolNames(), toolIdsOrder,
                            taskDetailVO.getToolConfigInfoList(),
                            toolConfigInfoVOs));
                    taskDetailVO.setToolConfigInfoList(toolConfigInfoVOs);
                    return taskDetailVO;
                }).collect(Collectors.toList());
        //根据任务状态过滤
        if (taskListReqVO != null && null != taskListReqVO.getTaskStatus()) {
            taskDetailVOS = taskDetailVOS.stream().filter(taskDetailVO -> {
                Boolean selected = false;
                switch (taskListReqVO.getTaskStatus()) {
                    case SUCCESS:
                        if (null != taskDetailVO.getDisplayStepStatus() && null != taskDetailVO.getDisplayStep()
                                && taskDetailVO.getDisplayStepStatus() == ComConstants.StepStatus.SUCC.value()
                                && taskDetailVO.getDisplayStep() >= ComConstants.Step4MutliTool.COMPLETE.value()) {
                            selected = true;
                        }
                        break;
                    case FAIL:
                        if (null != taskDetailVO.getDisplayStepStatus()
                                && taskDetailVO.getDisplayStepStatus() == ComConstants.StepStatus.FAIL.value()) {
                            selected = true;
                        }
                        break;
                    case WAITING:
                        if (null == taskDetailVO.getDisplayStepStatus()
                                || (null != taskDetailVO.getDisplayStepStatus()
                                && taskDetailVO.getDisplayStepStatus() == ComConstants.StepStatus.SUCC.value()
                                && (null == taskDetailVO.getDisplayStep()
                                || taskDetailVO.getDisplayStep() == ComConstants.StepStatus.SUCC.value()))) {
                            selected = true;
                        }
                        break;
                    case ANALYSING:
                        if (null != taskDetailVO.getDisplayStepStatus() && null != taskDetailVO.getDisplayStep()
                                && taskDetailVO.getDisplayStepStatus() != ComConstants.StepStatus.FAIL.value()
                                && taskDetailVO.getDisplayStep() > ComConstants.Step4MutliTool.READY.value()
                                && taskDetailVO.getDisplayStep() < ComConstants.Step4MutliTool.COMPLETE.value()) {
                            selected = true;
                        }
                        break;
                    case DISABLED:
                        if (ComConstants.Status.DISABLE.value() == taskDetailVO.getStatus()) {
                            selected = true;
                        }
                        break;
                    default:
                        break;
                }
                return selected;
            }).collect(Collectors.toList());
        }

        if (taskListReqVO.getPageable() == null || !taskListReqVO.getPageable()) {
            taskDetailVOS.stream()
                    .forEach(taskDetailVO -> taskDetailVO.setCodeLibraryInfo(getRepoInfo(taskDetailVO.getTaskId())));
        }

        return sortByDate(taskListReqVO, taskDetailVOS, taskSortType);
    }


    @Override
    public TaskListVO getTaskBaseList(String projectId, String user) {
        List<TaskInfoEntity> taskList = getQualifiedTaskListForTaskBase(projectId, user);
        if (CollectionUtils.isEmpty(taskList)) {
            return null;
        }

        List<TaskDetailVO> taskBaseVOList = taskList.stream().map(taskInfoEntity -> {
            TaskDetailVO taskDetailVO = new TaskDetailVO();
            taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
            taskDetailVO.setEntityId(taskInfoEntity.getEntityId());
            taskDetailVO.setNameCn(taskInfoEntity.getNameCn());
            taskDetailVO.setNameEn(taskInfoEntity.getNameEn());
            taskDetailVO.setStatus(taskInfoEntity.getStatus());
            taskDetailVO.setToolNames(taskInfoEntity.getToolNames());
            taskDetailVO.setCreatedDate(taskInfoEntity.getCreatedDate());
            taskDetailVO.setDisableTime(taskInfoEntity.getDisableTime());
            //设置置顶标识
            taskDetailVO.setTopFlag(-1);
            if (CollectionUtils.isNotEmpty(taskInfoEntity.getTopUser())) {
                if (taskInfoEntity.getTopUser().contains(user)) {
                    taskDetailVO.setTopFlag(1);
                }
            }
            return taskDetailVO;
        }).collect(Collectors.toList());

        List<TaskDetailVO> enableTaskList = taskBaseVOList.stream()
                .filter(taskDetailVO -> !TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus()))
                .sorted((o1, o2) -> o2.getTopFlag() - o1.getTopFlag() == 0
                        ? o2.getCreatedDate().compareTo(o1.getCreatedDate()) : o2.getTopFlag() - o1.getTopFlag())
                .collect(Collectors.toList());

        List<TaskDetailVO> disableTaskList = taskBaseVOList.stream()
                .filter(taskDetailVO -> TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus()))
                .sorted((o1, o2) -> o2.getTopFlag() - o1.getTopFlag() == 0
                        ? (StringUtils.isEmpty(o2.getDisableTime()) ? Long.valueOf(0) :
                        Long.valueOf(o2.getDisableTime()))
                        .compareTo(StringUtils.isEmpty(o1.getDisableTime()) ? Long.valueOf(0) :
                                Long.valueOf(o1.getDisableTime())) :
                        o2.getTopFlag() - o1.getTopFlag())
                .collect(Collectors.toList());

        return new TaskListVO(enableTaskList, disableTaskList);
    }

    private List<TaskInfoEntity> getQualifiedTaskListForTaskBase(String projectId, String user) {
        Set<String> createFromSet = Sets.newHashSet(
                ComConstants.BsTaskCreateFrom.BS_PIPELINE.value(),
                ComConstants.BsTaskCreateFrom.BS_CODECC.value()
        );

        List<TaskInfoEntity> taskInfoEntityList =
                taskRepository.findSpecialFieldByProjectIdAndCreateFromIn(projectId, createFromSet);

        // 查询用户有权限的CodeCC任务
        Set<String> tasks = authExPermissionApi.queryTaskListForUser(user, projectId,
                Sets.newHashSet(CodeCCAuthAction.REPORT_VIEW.getActionName()));

        // 查询用户有权限的流水线
        Set<String> pipelines = authExPermissionApi.queryPipelineListForUser(user, projectId,
                Sets.newHashSet(PipelineAuthAction.VIEW.getActionName()));

        List<String> adminMembers = authExPermissionApi.getAdminMembers();

        //查询任务清单速度优化
        List<TaskInfoEntity> retList = taskInfoEntityList.stream().filter(taskInfoEntity ->
                ((CollectionUtils.isNotEmpty(taskInfoEntity.getTaskOwner())
                        && taskInfoEntity.getTaskOwner().contains(user)
                        && taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())
                        && !(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                        .equalsIgnoreCase(taskInfoEntity.getCreateFrom())))
                        || (CollectionUtils.isNotEmpty(tasks)
                        && tasks.contains(String.valueOf(taskInfoEntity.getTaskId())))
                        || (CollectionUtils.isNotEmpty(pipelines) && pipelines.contains(taskInfoEntity.getPipelineId()))
                        //系统管理员有权限查询任务清单
                        || adminMembers.contains(user))
        ).collect(Collectors.toList());

        return retList;
    }


    /**
     * 查询符合条件的任务清单
     *
     * @param projectId
     * @param user
     * @return
     */
    protected List<TaskInfoEntity> getQualifiedTaskList(
            String projectId,
            String user,
            Integer taskStatus,
            String taskSource
    ) {
        // 将原getQualifiedTaskList逻辑，分拆为getQualifiedTaskListCore和getQualifiedTaskList进行复用
        Set<String> createFromSet = StringUtils.isNotEmpty(taskSource)
                ? Sets.newHashSet(taskSource)
                : Sets.newHashSet(BsTaskCreateFrom.BS_PIPELINE.value(), BsTaskCreateFrom.BS_CODECC.value());

        if (projectId.startsWith(ComConstants.GONGFENG_PROJECT_ID_PREFIX)) {
            createFromSet.add(BsTaskCreateFrom.GONGFENG_SCAN.value());
        }

        Set<TaskInfoEntity> taskInfoEntities = taskRepository.findByProjectIdAndCreateFromIn(projectId, createFromSet);

        if (CollectionUtils.isNotEmpty(taskInfoEntities)) {
            List<ToolConfigInfoEntity> toolConfigInfoEntityList = toolConfigRepository.findByTaskIdIn(
                    taskInfoEntities.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList())
            );
            Map<Long, List<ToolConfigInfoEntity>> toolConfigMap = toolConfigInfoEntityList.stream()
                    .collect(Collectors.groupingBy(ToolConfigInfoEntity::getTaskId));

            for (TaskInfoEntity taskInfoEntity : taskInfoEntities) {
                taskInfoEntity.setToolConfigInfoList(toolConfigMap.get(taskInfoEntity.getTaskId()));
            }
        }

        if (taskStatus != null) {
            taskInfoEntities = taskInfoEntities.stream()
                    .filter(x -> x.getStatus().equals(taskStatus))
                    .collect(Collectors.toSet());
        }

        return getQualifiedTaskListCore(projectId, user, taskInfoEntities);
    }

    private List<TaskInfoEntity> getQualifiedTaskListCore(
            String projectId,
            String user,
            Collection<TaskInfoEntity> taskInfoEntities
    ) {
        if (CollectionUtils.isEmpty(taskInfoEntities)) {
            return Lists.newArrayList();
        }

        // 查询用户有权限的CodeCC任务
        Set<String> tasks = authExPermissionApi.queryTaskListForUser(user, projectId,
                Sets.newHashSet(CodeCCAuthAction.REPORT_VIEW.getActionName()));

        // 查询用户有权限的流水线
        Set<String> pipelines = authExPermissionApi.queryPipelineListForUser(user, projectId,
                Sets.newHashSet(PipelineAuthAction.VIEW.getActionName()));

        Set<String> adminMemberSet = Sets.newHashSet(authExPermissionApi.getAdminMembers());

        // 查询任务清单速度优化
        List<TaskInfoEntity> resultTasks = taskInfoEntities.stream().filter(taskInfoEntity -> {
            return ((CollectionUtils.isNotEmpty(taskInfoEntity.getTaskOwner())
                    && taskInfoEntity.getTaskOwner().contains(user)
                    && taskInfoEntity.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())
                    && !(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                    .equalsIgnoreCase(taskInfoEntity.getCreateFrom())))
                    || (CollectionUtils.isNotEmpty(tasks)
                    && tasks.contains(String.valueOf(taskInfoEntity.getTaskId())))
                    || (CollectionUtils.isNotEmpty(pipelines) && pipelines.contains(taskInfoEntity.getPipelineId()))
                    // 系统管理员有权限查询任务清单
                    || adminMemberSet.contains(user)
                    || (!ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                    .equalsIgnoreCase(taskInfoEntity.getCreateFrom())
                    && !ComConstants.BsTaskCreateFrom.BS_CODECC.value()
                    .equalsIgnoreCase(taskInfoEntity.getCreateFrom())));
            // 如果有过滤条件，要加过滤
            // && (taskInfoEntity.getStatus().equals(taskStatus) || null == taskStatus);
        }).collect(Collectors.toList());

        return resultTasks;
    }

    @Override
    public TaskBaseVO getTaskInfo() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String taskId = request.getHeader("X-DEVOPS-TASK-ID");
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        String projectId = request.getHeader(AUTH_HEADER_DEVOPS_PROJECT_ID);
        log.info("getTaskInfo: {}", taskId);
        if (!StringUtils.isNumeric(taskId)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{String.valueOf(taskId)},
                    null);
        }
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(Long.valueOf(taskId));

        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{taskId}, null);
        }

        TaskDetailVO taskBaseVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskBaseVO);

        //添加个性化报告信息
        NotifyCustomVO notifyCustomVO = new NotifyCustomVO();
        NotifyCustomEntity notifyCustomEntity = taskEntity.getNotifyCustomInfo();
        if (null != notifyCustomEntity) {
            BeanUtils.copyProperties(notifyCustomEntity, notifyCustomVO);
        }
        taskBaseVO.setNotifyCustomInfo(notifyCustomVO);

        // 给工具分类及排序，并加入规则集
        sortedToolList(taskBaseVO, taskEntity.getToolConfigInfoList());

        //获取规则和规则集数量
        Result<TaskBaseVO> checkerCountVO =
                client.get(ServiceCheckerSetRestResource.class).getCheckerAndCheckerSetCount(Long.valueOf(taskId),
                        projectId);
        if (checkerCountVO.isOk() && null != checkerCountVO.getData()) {
            taskBaseVO.setCheckerSetName(checkerCountVO.getData().getCheckerSetName());
            taskBaseVO.setCheckerCount(checkerCountVO.getData().getCheckerCount());
        }

        taskBaseVO.setCodeLibraryInfo(getRepoInfo(Long.valueOf(taskId)));

        //如果是灰度项目，则需要显示为api创建
        if (taskEntity.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            taskBaseVO.setCreateFrom(ComConstants.BsTaskCreateFrom.API_TRIGGER.value());
        }

        String userId = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);
        //判断是否可执行
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equalsIgnoreCase(taskEntity.getCreateFrom())
                && StringUtils.isNotEmpty(userId) && authExPermissionApi.isAdminMember(userId)) {
            // 查询用户有权限的流水线
            Set<String> pipelines = authExPermissionApi.queryPipelineListForUser(userId, projectId,
                    Sets.newHashSet(PipelineAuthAction.EXECUTE.getActionName()));
            taskBaseVO.setHasNoPermission(!pipelines.contains(taskEntity.getPipelineId()));
        } else {
            taskBaseVO.setHasNoPermission(false);
        }

        taskBaseVO.setCheckerSetType(ComConstants.CheckerSetType.forValue(taskEntity.getCheckerSetType()));
        // 数据迁移标识
        taskBaseVO.setDataMigrationSuccessful(true);

        return taskBaseVO;
    }

    @Override
    public TaskDetailVO getTaskInfoById(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        try {
            String taskInfoStr = objectMapper.writeValueAsString(taskEntity);
            // 注入任务类型和创建来源

            TaskDetailVO taskDetailVO = objectMapper.readValue(taskInfoStr,
                    new TypeReference<TaskDetailVO>() {
                    });
            setTaskCreateInfo(taskDetailVO);
            return taskDetailVO;
        } catch (IOException e) {
            String message = "string conversion TaskDetailVO error";
            log.error(message, e);
            throw new StreamException(message);
        }
    }

    @Override
    public TaskDetailVO getTaskInfoWithoutToolsByTaskId(Long taskId) {
        List<TaskInfoEntity> taskEntities = taskRepository.findTaskInfoWithoutToolsByTaskId(taskId);
        TaskInfoEntity taskEntity = CollectionUtils.isEmpty(taskEntities) ? null : taskEntities.get(0);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);

        taskDetailVO.setCheckerSetType(ComConstants.CheckerSetType.forValue(taskEntity.getCheckerSetType()));
        return taskDetailVO;
    }

    @Override
    public TaskDetailVO getTaskInfoByStreamName(String streamName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByNameEn(streamName);

        if (taskEntity == null) {
            log.error("can not find task by streamName: {}", streamName);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{streamName}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);

        // 加入工具列表
        List<ToolConfigInfoEntity> toolEntityList = taskEntity.getToolConfigInfoList();
        if (CollectionUtils.isNotEmpty(toolEntityList)) {
            Set<String> toolSet = new HashSet<>();
            taskDetailVO.setToolSet(toolSet);

            for (ToolConfigInfoEntity toolEntity : toolEntityList) {
                if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() != toolEntity.getFollowStatus()
                        && !Tool.CLOC.name().equalsIgnoreCase(toolEntity.getToolName())
                        && !checkToolRemoved(toolEntity.getToolName(), taskEntity)) {
                    toolSet.add(toolEntity.getToolName());
                }
            }
        }

        taskDetailVO.setToolConfigInfoList(toolEntityList.stream().map(toolConfigInfoEntity -> {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "checkerSet");
            return toolConfigInfoVO;
        }).collect(Collectors.toList()));

        // 加入通知定制配置
        if (taskEntity.getNotifyCustomInfo() != null) {
            NotifyCustomVO notifyCustomVO = new NotifyCustomVO();
            BeanUtils.copyProperties(taskEntity.getNotifyCustomInfo(), notifyCustomVO);
            taskDetailVO.setNotifyCustomInfo(notifyCustomVO);
        }

        taskDetailVO.setCheckerSetType(ComConstants.CheckerSetType.forValue(taskEntity.getCheckerSetType()));

        // 是否回写工蜂
        if (taskEntity.getMrCommentEnable() != null) {
            taskDetailVO.setMrCommentEnable(taskEntity.getMrCommentEnable());
        }

        return taskDetailVO;
    }


    protected Boolean taskDetailDisplayInfo(ToolConfigInfoEntity toolConfigInfoEntity, TaskDetailVO taskDetailVO,
            String displayName) {
        Integer displayStepStatus = 0;
        //检测到有任务运行中（非成功状态）
        Boolean processFlag = false;
        //更新工具显示状态
        //如果有失败的工具，则显示失败的状态
        if (toolConfigInfoEntity.getStepStatus() == StepStatus.FAIL.value()) {
            displayStepStatus = StepStatus.FAIL.value();
            taskDetailVO.setDisplayStepStatus(displayStepStatus);
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;

            //如果没找到失败的工具，有分析中的工具，则显示分析中
        } else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value()
                && toolConfigInfoEntity.getCurStep() < Step4MutliTool.COMPLETE.value()
                && toolConfigInfoEntity.getCurStep() > Step4MutliTool.READY.value()
                && displayStepStatus != StepStatus.FAIL.value()) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;

            //如果没找到失败的工具，有准备的工具，则显示准备
        } else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value()
                && toolConfigInfoEntity.getCurStep() == Step4MutliTool.READY.value()
                && displayStepStatus != StepStatus.FAIL.value()) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
            processFlag = true;

            //如果还没找到其他状态，则显示成功
        } else if (toolConfigInfoEntity.getStepStatus() == StepStatus.SUCC.value()
                && toolConfigInfoEntity.getCurStep() >= Step4MutliTool.COMPLETE.value()
                && StringUtils.isBlank(taskDetailVO.getDisplayToolName())) {
            taskDetailVO.setDisplayToolName(toolConfigInfoEntity.getToolName());
            taskDetailVO.setDisplayStep(toolConfigInfoEntity.getCurStep());
            taskDetailVO.setDisplayName(displayName);
        }
        return processFlag;

    }

    /**
     * 获取任务接入的工具列表
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskBaseVO getTaskToolList(long taskId) {
        List<ToolConfigInfoEntity> toolEntityList = toolRepository.findByTaskId(Long.valueOf(taskId));

        TaskBaseVO taskBaseVO = new TaskBaseVO();

        // 给工具分类及排序
        sortedToolList(taskBaseVO, toolEntityList);

        return taskBaseVO;
    }

    /**
     * 修改任务
     *
     * @param taskUpdateVO
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_INFO, operType = MODIFY_INFO)
    public Boolean updateTask(TaskUpdateVO taskUpdateVO, Long taskId, String userName) {
        // 检查参数
        if (!checkParam(taskUpdateVO)) {
            return false;
        }

        // 任务是否注册过
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskInfoEntity)) {
            log.error("can not find task info");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 修改任务信息
        taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(),
                taskUpdateVO.getTaskOwner(),
                taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(), taskUpdateVO.getStatus(),
                userName);

        //根据语言解绑规则集
        if (!taskUpdateVO.getCodeLang().equals(taskInfoEntity.getCodeLang())) {
            log.info("update the code lang, and set full scan: {}, {} -> {}", taskId, taskInfoEntity.getCodeLang(),
                    taskUpdateVO.getCodeLang());
            client.get(ServiceCheckerSetRestResource.class).updateCheckerSetAndTaskRelation(taskId,
                    taskUpdateVO.getCodeLang(), userName);
        }

        return true;
    }

    @Override
    public Boolean updateTaskRepoOwner(Long taskId, List<String> repoOwners, String userName) {
        return taskDao.updateRepoOwner(taskId, repoOwners, userName);
    }

    @Override
    public Boolean updateRuntimeInfo(Long taskId, RuntimeUpdateMetaVO runtimeUpdateMetaVO, String userName) {
        taskDao.updatePipelineTaskInfo(taskId, runtimeUpdateMetaVO.getPipelineTaskId(),
                runtimeUpdateMetaVO.getPipelineTaskName(), userName, runtimeUpdateMetaVO.getTimeout());
        return true;
    }


    @Override
    public TaskOverviewVO getTaskOverview(Long taskId, String buildNum) {
        List<TaskInfoEntity> taskEntities = taskRepository.findToolListByTaskId(taskId);
        TaskInfoEntity taskEntity = CollectionUtils.isEmpty(taskEntities) ? null : taskEntities.get(0);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        TaskOverviewVO taskOverviewVO = new TaskOverviewVO();
        taskOverviewVO.setTaskId(taskId);
        List<TaskOverviewVO.LastAnalysis> toolLastAnalysisList = new ArrayList<>();
        Map<String, TaskOverviewVO.LastAnalysis> toolLastAnalysisMap = new HashMap<>();

        List<ToolLastAnalysisResultVO> lastAnalysisResultVOs;

        if (NumberUtils.isNumber(buildNum)) {
            GetLastAnalysisResultsVO getLastAnalysisResultsVO = new GetLastAnalysisResultsVO();
            getLastAnalysisResultsVO.setTaskId(taskId);
            getLastAnalysisResultsVO.setBuildNum(buildNum);

            // 调用defect模块的接口获取工具的某一次分析结果
            Result<List<ToolLastAnalysisResultVO>> result =
                    client.get(ServiceTaskLogRestResource.class).getAnalysisResults(getLastAnalysisResultsVO);
            if (result.isNotOk() || null == result.getData()) {
                log.error("get analysis results fail! taskId is: {}, msg: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            lastAnalysisResultVOs = result.getData();

            if (CollectionUtils.isNotEmpty(lastAnalysisResultVOs)) {
                String buildId = "";
                for (ToolLastAnalysisResultVO resultVO : lastAnalysisResultVOs) {
                    int curStep = resultVO.getCurrStep();
                    if (Arrays.asList(Tool.COVERITY.name(), Tool.KLOCWORK.name()).contains(resultVO.getToolName())) {
                        if (curStep == ComConstants.Step4Cov.DEFECT_SYNS.value()) {
                            curStep = ComConstants.Step4Cov.COMPLETE.value();
                        }
                    } else {
                        if (curStep == Step4MutliTool.COMMIT.value()) {
                            curStep = Step4MutliTool.COMPLETE.value();
                        }
                    }

                    int stepStatus = resultVO.getFlag() == ComConstants.StepFlag.FAIL.value()
                            ? ComConstants.StepStatus.FAIL.value() : ComConstants.StepStatus.SUCC.value();

                    TaskOverviewVO.LastAnalysis lastAnalysis = new TaskOverviewVO.LastAnalysis();
                    String toolName = resultVO.getToolName();
                    lastAnalysis.setToolName(toolName);
                    lastAnalysis.setCurStep(curStep);
                    lastAnalysis.setStepStatus(stepStatus);
                    toolLastAnalysisMap.put(toolName, lastAnalysis);
                    toolLastAnalysisList.add(lastAnalysis);
                    buildId = resultVO.getBuildId();
                }
                // 获取度量信息
                Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
                if (metricsRes.isOk() && metricsRes.getData() != null) {
                    try {
                        org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();

            if (CollectionUtils.isEmpty(toolConfigInfoList)) {
                return taskOverviewVO;
            }

            for (ToolConfigInfoEntity tool : toolConfigInfoList) {
                if (tool == null) {
                    continue;
                }
                int followStatus = tool.getFollowStatus();
                if (followStatus != TaskConstants.FOLLOW_STATUS.WITHDRAW.value()) {
                    TaskOverviewVO.LastAnalysis lastAnalysis = new TaskOverviewVO.LastAnalysis();
                    String toolName = tool.getToolName();
                    lastAnalysis.setToolName(toolName);
                    lastAnalysis.setCurStep(tool.getCurStep());
                    lastAnalysis.setStepStatus(tool.getStepStatus());
                    toolLastAnalysisMap.put(toolName, lastAnalysis);
                    toolLastAnalysisList.add(lastAnalysis);
                }
            }

            GetLastAnalysisResultsVO getLastAnalysisResultsVO = new GetLastAnalysisResultsVO();
            getLastAnalysisResultsVO.setTaskId(taskId);
            getLastAnalysisResultsVO.setToolSet(toolLastAnalysisMap.keySet());

            // 调用defect模块的接口获取工具的最近一次分析结果
            Result<List<ToolLastAnalysisResultVO>> result =
                    client.get(ServiceTaskLogRestResource.class).getLastAnalysisResults(getLastAnalysisResultsVO);
            if (result.isNotOk() || null == result.getData()) {
                log.error("get last analysis results fail! taskId is: {}, msg: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }

            lastAnalysisResultVOs = result.getData();
        }

        if (CollectionUtils.isNotEmpty(lastAnalysisResultVOs)) {
            String buildId = "";
            for (ToolLastAnalysisResultVO toolLastAnalysisResultVO : lastAnalysisResultVOs) {
                TaskOverviewVO.LastAnalysis lastAnalysis =
                        toolLastAnalysisMap.get(toolLastAnalysisResultVO.getToolName());
                lastAnalysis.setLastAnalysisResult(toolLastAnalysisResultVO.getLastAnalysisResultVO());
                long elapseTime = toolLastAnalysisResultVO.getElapseTime();
                long endTime = toolLastAnalysisResultVO.getEndTime();
                long startTime = toolLastAnalysisResultVO.getStartTime();
                long lastAnalysisTime = startTime;
                if (elapseTime == 0 && endTime != 0) {
                    elapseTime = endTime - startTime;
                }

                lastAnalysis.setElapseTime(elapseTime);
                lastAnalysis.setLastAnalysisTime(lastAnalysisTime);
                lastAnalysis.setBuildId(toolLastAnalysisResultVO.getBuildId());
                lastAnalysis.setBuildNum(toolLastAnalysisResultVO.getBuildNum());
                buildId = toolLastAnalysisResultVO.getBuildId();
            }
            // 获取度量信息
            Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
            if (metricsRes.isOk() && metricsRes.getData() != null) {
                try {
                    org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("", e);
                }
            }
        }
        String orderToolIds = commonDao.getToolOrder();
        List<String> toolOrderList = Arrays.asList(orderToolIds.split(","));

        toolLastAnalysisList = toolLastAnalysisList.stream()
                .filter(lastAnalysis ->
                        !lastAnalysis.getToolName().equals(Tool.GITHUBSTATISTIC.name())
                                && !lastAnalysis.getToolName().equals(Tool.SCC.name())
                                && !checkToolRemoved(lastAnalysis.getToolName(), taskEntity))
                .sorted(Comparator.comparingInt(o -> toolOrderList.contains(o.getToolName())
                        ? toolOrderList.indexOf(o.getToolName()) : Integer.MAX_VALUE))
                .collect(Collectors.toList());

        taskOverviewVO.setTaskId(taskId);
        //taskOverviewVO.setStatus();
        taskOverviewVO.setLastAnalysisResultList(toolLastAnalysisList);

        return taskOverviewVO;
    }

    @Override
    public TaskOverviewVO getTaskOverview(Long taskId, String buildNum, String orderBy) {
        if (StringUtils.isBlank(orderBy) || orderBy.equals("TOOL")) {
            return getTaskOverview(taskId, buildNum);
        }

        List<TaskInfoEntity> taskEntities = taskRepository.findToolListByTaskId(taskId);
        TaskInfoEntity taskEntity = CollectionUtils.isEmpty(taskEntities) ? null : taskEntities.get(0);
        if (taskEntity == null) {
            log.error("can not find task by taskId: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        Map<String, List<String>> toolMap = toolConfigInfoList.stream()
                .filter(it -> it.getFollowStatus() != TaskConstants.FOLLOW_STATUS.WITHDRAW.value()
                        && !checkToolRemoved(it.getToolName(), taskEntity))
                .map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.groupingBy(it -> toolMetaCache.getToolBaseMetaCache(it).getType()));

        TaskOverviewVO taskOverviewVO = new TaskOverviewVO();
        Map<String, TaskOverviewVO.LastCluster> lastClusterResultMap = Maps.newLinkedHashMapWithExpectedSize(5);
        lastClusterResultMap.put(ComConstants.ToolType.DEFECT.name(),
                new TaskOverviewVO.LastCluster(new DefectClusterResultVO(
                        ComConstants.ToolType.DEFECT.name(), toolMap.getOrDefault(
                        ComConstants.ToolType.DEFECT.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ComConstants.ToolType.DEFECT.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ComConstants.ToolType.SECURITY.name(),
                new TaskOverviewVO.LastCluster(new SecurityClusterResultVO(
                        ComConstants.ToolType.SECURITY.name(), toolMap.getOrDefault(
                        ComConstants.ToolType.SECURITY.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ComConstants.ToolType.SECURITY.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ComConstants.ToolType.STANDARD.name(),
                new TaskOverviewVO.LastCluster(new StandardClusterResultVO(
                        ComConstants.ToolType.STANDARD.name(), toolMap.getOrDefault(
                        ComConstants.ToolType.STANDARD.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ComConstants.ToolType.STANDARD.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ComConstants.ToolType.CCN.name(),
                new TaskOverviewVO.LastCluster(new CcnClusterResultVO(
                        ComConstants.ToolType.CCN.name(), toolMap.getOrDefault(
                        ComConstants.ToolType.CCN.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ComConstants.ToolType.CCN.name(), Collections.emptyList()))));

        lastClusterResultMap.put(ComConstants.ToolType.DUPC.name(),
                new TaskOverviewVO.LastCluster(new DupcClusterResultVO(
                        ComConstants.ToolType.DUPC.name(), toolMap.getOrDefault(
                        ComConstants.ToolType.DUPC.name(), Collections.emptyList()).size(),
                        toolMap.getOrDefault(ComConstants.ToolType.DUPC.name(), Collections.emptyList()))));

        Result<TaskLogOverviewVO> result = client.get(ServiceTaskLogOverviewResource.class)
                .getTaskLogOverview(taskId, null, ComConstants.ScanStatus.SUCCESS.getCode());

        if (result.isOk() && result.getData() != null) {
            TaskLogOverviewVO taskLogOverviewVO = result.getData();
            String buildId = taskLogOverviewVO.getBuildId();
            log.info("get task overview by type: {} {}", taskId, buildId);
            // 获取维度统计信息
            Result<List<BaseClusterResultVO>> clusterResult =
                    client.get(ServiceClusterStatisticRestReource.class).getClusterStatistic(taskId, buildId);

            if (clusterResult.isOk() && clusterResult.getData() != null) {
                List<BaseClusterResultVO> clusterVOList = clusterResult.getData();
                log.info("task overview Test {}", clusterVOList);
                clusterVOList.forEach(baseClusterResultVO -> {
                    baseClusterResultVO.setToolList(baseClusterResultVO.getToolList());
                    baseClusterResultVO.setToolNum(baseClusterResultVO.getToolNum());
                    lastClusterResultMap.put(baseClusterResultVO.getType(),
                            new TaskOverviewVO.LastCluster(baseClusterResultVO));
                });
            }

            // 获取度量信息
            Result<MetricsVO> metricsRes = client.get(ServiceMetricsRestResource.class).getMetrics(taskId, buildId);
            if (metricsRes.isOk() && metricsRes.getData() != null) {
                try {
                    org.apache.commons.beanutils.BeanUtils.copyProperties(taskOverviewVO, metricsRes.getData());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        taskOverviewVO.setTaskId(taskId);
        taskOverviewVO.setLastClusterResultList(new ArrayList<>(lastClusterResultMap.values()));
        return taskOverviewVO;
    }


    /**
     * 开启任务
     *
     * @param taskId
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = ENABLE_ACTION)
    public Boolean startTask(Long taskId, String userName) {
        return doStartTask(taskId, userName, true);
    }


    /**
     * 开启任务
     *
     * @param taskId 任务ID
     * @param userName 操作人
     * @param checkPermission 是否检查权限
     * @return boolean
     */
    private Boolean doStartTask(Long taskId, String userName, boolean checkPermission) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        List<String> taskMemberList = taskEntity.getTaskMember();
        List<String> taskOwnerList = taskEntity.getTaskOwner();
        Boolean taskMemberPermission = CollectionUtils.isEmpty(taskMemberList) || !taskMemberList.contains(userName);
        Boolean taskOwnerPermission = CollectionUtils.isEmpty(taskOwnerList) || !taskOwnerList.contains(userName);
        if (checkPermission && taskMemberPermission && taskOwnerPermission) {
            log.error("current user has no permission to the task");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
        }

        if (CollectionUtils.isNotEmpty(taskEntity.getExecuteDate())
                && StringUtils.isNotBlank(taskEntity.getExecuteTime())) {
            log.error("The task is already open and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_START);
        }

        // 如果是蓝盾项目，要开启流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId())) {
            // 启动时，把原先的定时任务恢复
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo)) {
                log.error("pipeline execution timing is empty.");
                //                throw new CodeCCException(TaskMessageCode.PIPELINE_EXECUTION_TIME_EMPTY);
            } else {
                String lastExecuteTime = lastDisableTaskInfo.getLastExecuteTime();
                List<String> lastExecuteDate = lastDisableTaskInfo.getLastExecuteDate();

                // 开启定时执行的日期时间
                taskEntity.setExecuteTime(lastExecuteTime);
                taskEntity.setExecuteDate(lastExecuteDate);
                // 删除DB保存的执行时间
                taskEntity.setLastDisableTaskInfo(null);
                pipelineService.modifyCodeCCTiming(taskEntity, lastExecuteDate, lastExecuteTime, userName);
            }
        }

        taskEntity.setDisableTime("");
        taskEntity.setDisableReason("");
        taskEntity.setStatus(TaskConstants.TaskStatus.ENABLE.value());

        //在权限中心重新注册任务
        authExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskId), taskEntity.getNameEn(),
                taskEntity.getProjectId());

        //恢复日报
        if (null != taskEntity.getNotifyCustomInfo()
                && StringUtils.isNotBlank(taskEntity.getNotifyCustomInfo().getReportJobName())) {
            JobExternalDto jobExternalDto =
                    new JobExternalDto(taskEntity.getNotifyCustomInfo().getReportJobName(), "", "", "", new HashMap<>(),
                            OperationType.RESUME);
            rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 停用任务
     *
     * @param taskId
     * @param disabledReason
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopTask(Long taskId, String disabledReason, String userName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        return doStopTask(taskEntity, disabledReason, userName, true);
    }

    /**
     * 停用任务
     *
     * @param pipelineId
     * @param disabledReason
     * @param userName
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopTask(String pipelineId, String disabledReason, String userName) {
        List<TaskInfoEntity> taskEntityList = taskRepository.findAllByPipelineId(pipelineId);
        if (CollectionUtils.isEmpty(taskEntityList)) {
            log.error("taskInfo not exists! pipeline id is: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(pipelineId)},
                    null);
        }
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        taskEntityList.forEach(taskInfoEntity -> {
            try {
                doStopTask(taskInfoEntity, disabledReason, userName, false);
            } catch (Exception e) {
                log.info("stop task fail! task id: {}", taskInfoEntity.getTaskId());
                result.set(false);
            }
        });
        return result.get();
    }


    @Override
    @OperationHistory(funcId = FUNC_TASK_SWITCH, operType = DISABLE_ACTION)
    public Boolean stopSinglePipelineTask(String pipelineId, String multiPipelineMark, String disabledReason,
            String userName) {
        String queryMark = multiPipelineMark;
        if (StringUtils.isBlank(multiPipelineMark)) {
            queryMark = null;
        }
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByPipelineIdAndMultiPipelineMark(pipelineId, queryMark);
        if (Objects.isNull(taskInfoEntity)) {
            log.error("taskInfo not exists! pipeline id is: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(pipelineId)},
                    null);
        }
        return doStopTask(taskInfoEntity, disabledReason, userName, false);
    }

    /**
     * 管理员在OP停用任务
     *
     * @param taskId 任务ID
     * @param disabledReason 停用理由
     * @param userName 操作人
     * @return boolean
     */
    @Override
    public Boolean stopTaskByAdmin(Long taskId, String disabledReason, String userName) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        return doStopTask(taskEntity, disabledReason, userName, false);
    }


    /**
     * 管理员在OP开启任务
     *
     * @param taskId 任务ID
     * @param userName 操作人
     * @return boolean
     */
    @Override
    public Boolean startTaskByAdmin(Long taskId, String userName) {
        return doStartTask(taskId, userName, false);
    }


    private Boolean doStopTask(TaskInfoEntity taskEntity,
            String disabledReason,
            String userName,
            boolean checkPermission) {
        long taskId = taskEntity.getTaskId();
        if (BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            log.info("gongfeng project not allowed to disable");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
        }

        //判断是否有权限
        List<String> taskMemberList = taskEntity.getTaskMember();
        List<String> taskOwnerList = taskEntity.getTaskOwner();
        Boolean taskMemberPermission = CollectionUtils.isEmpty(taskMemberList) || !taskMemberList.contains(userName);
        Boolean taskOwnerPermission = CollectionUtils.isEmpty(taskOwnerList) || !taskOwnerList.contains(userName);
        if (checkPermission && taskMemberPermission && taskOwnerPermission) {
            log.error("current user has no permission to the task");
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, new Exception());
        }

        if (StringUtils.isNotBlank(taskEntity.getDisableTime())) {
            log.error("The task is already close and cannot be repeated.");
            throw new CodeCCException(TaskMessageCode.TASK_HAS_CLOSE);
        }

        // 如果是蓝盾项目，并且是服务创建的，要停止流水线定时触发任务
        if (StringUtils.isNotBlank(taskEntity.getProjectId())
                && BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            String executeTime = taskEntity.getExecuteTime();
            List<String> executeDate = taskEntity.getExecuteDate();

            if (CollectionUtils.isEmpty(executeDate)) {
                log.error("pipeline execute date is empty. task id : {}", taskId);
                executeDate = Collections.emptyList();
            }

            if (StringUtils.isBlank(executeTime)) {
                log.error("pipeline execute time is empty. task id : {}", taskId);
                executeTime = "";
            }

            // 调用蓝盾API 删除定时构建原子
            pipelineService.deleteCodeCCTiming(userName, taskEntity);

            // 存储启用日期时间到DisableTaskEntity
            DisableTaskEntity lastDisableTaskInfo = taskEntity.getLastDisableTaskInfo();
            if (Objects.isNull(lastDisableTaskInfo)) {
                lastDisableTaskInfo = new DisableTaskEntity();
            }

            lastDisableTaskInfo.setLastExecuteTime(executeTime);
            lastDisableTaskInfo.setLastExecuteDate(executeDate);
            taskEntity.setLastDisableTaskInfo(lastDisableTaskInfo);
        }

        //要将权限中心的任务成员，任务管理员同步到task表下面，便于后续启用时再进行注册
        TaskMemberVO taskMemberVO = getTaskUsers(taskId, taskEntity.getProjectId());
        taskEntity.setTaskMember(taskMemberVO.getTaskMember());
        taskEntity.setTaskOwner(taskMemberVO.getTaskOwner());
        taskEntity.setTaskViewer(taskMemberVO.getTaskViewer());

        //在权限中心中删除相应的资源
        if (BsTaskCreateFrom.BS_CODECC.value().equalsIgnoreCase(taskEntity.getCreateFrom())) {
            try {
                authExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), taskEntity.getProjectId());
            } catch (UnauthorizedException e) {
                log.error("delete iam resource fail! error message: {}", e.getMessage());
                throw new CodeCCException(TaskMessageCode.CLOSE_TASK_FAIL);
            }
        }

        log.info("stopping task: delete pipeline scheduled atom and auth center resource success! project id: {}",
                taskEntity.getProjectId());

        taskEntity.setExecuteDate(new ArrayList<>());
        taskEntity.setExecuteTime("");
        taskEntity.setDisableTime(String.valueOf(System.currentTimeMillis()));
        taskEntity.setDisableReason(disabledReason);
        taskEntity.setStatus(TaskConstants.TaskStatus.DISABLE.value());

        //停止日报
        if (null != taskEntity.getNotifyCustomInfo()
                && StringUtils.isNotBlank(taskEntity.getNotifyCustomInfo().getReportJobName())) {
            JobExternalDto jobExternalDto = new JobExternalDto(
                    taskEntity.getNotifyCustomInfo().getReportJobName(),
                    "",
                    "",
                    "",
                    new HashMap<>(),
                    OperationType.PARSE
            );
            rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    /**
     * 获取代码库配置信息
     *
     * @param taskId
     * @return
     */
    @Override
    public TaskCodeLibraryVO getCodeLibrary(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 获取所有工具的基础信息
        Map<String, ToolMetaBaseVO> toolMetaMap = toolMetaCache.getToolMetaListFromCache(Boolean.FALSE, Boolean.TRUE);

        // 获取排序好的所有工具
        String[] toolIdArr = Optional.ofNullable(taskEntity.getToolNames())
                .map(tool -> tool.split(ComConstants.STRING_SPLIT))
                .orElse(new String[]{});

        // 获取工具配置Map
        Map<String, JSONObject> chooseJsonMap = getToolConfigInfoMap(taskEntity);

        Map<String, GlobalMessage> tipsMessage = globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_TIPS);
        Map<String, GlobalMessage> labelNameMessage =
                globalMessageUtil.getGlobalMessageMap(GLOBAL_TOOL_PARAMS_LABEL_NAME);
        List<ToolConfigParamJsonVO> paramJsonList = new ArrayList<>();
        for (String toolName : toolIdArr) {
            // 工具被禁用则不显示
            if (!chooseJsonMap.keySet().contains(toolName)) {
                continue;
            }

            // 获取工具对应的基本数据
            ToolMetaBaseVO toolMetaBaseVO = toolMetaMap.get(toolName);

            if (Objects.nonNull(toolMetaBaseVO)) {
                String params = toolMetaBaseVO.getParams();
                if (StringUtils.isNotBlank(params) && !ComConstants.STRING_NULL_ARRAY.equals(params)) {
                    JSONObject chooseJson = chooseJsonMap.get(toolName);
                    List<Map<String, Object>> arrays = JsonUtil.INSTANCE.to(params);
                    for (Map<String, Object> array : arrays) {
                        ToolConfigParamJsonVO toolConfig = JsonUtil.INSTANCE.mapTo(array, ToolConfigParamJsonVO.class);

                        // 工具参数标签[ labelName ]国际化
                        GlobalMessage labelGlobalMessage = labelNameMessage.get(String.format("%s:%s", toolName,
                                toolConfig.getVarName()));
                        if (Objects.nonNull(labelGlobalMessage)) {
                            String globalLabelName = globalMessageUtil.getMessageByLocale(labelGlobalMessage);
                            toolConfig.setLabelName(globalLabelName);
                        }

                        // 工具参数提示[ tips ]国际化
                        GlobalMessage tipGlobalMessage = tipsMessage.get(String.format("%s:%s", toolName,
                                toolConfig.getVarName()));
                        if (Objects.nonNull(tipGlobalMessage)) {
                            String globalTips = globalMessageUtil.getMessageByLocale(tipGlobalMessage);
                            toolConfig.setVarTips(globalTips);
                        }

                        String toolChooseValue = Objects.isNull(chooseJson)
                                ? toolConfig.getVarDefault()
                                : StringUtils.isBlank((String) chooseJson.get(toolConfig.getVarName()))
                                        ? toolConfig.getVarDefault() : (String) chooseJson.get(toolConfig.getVarName());

                        toolConfig.setTaskId(taskId);
                        toolConfig.setToolName(toolMetaBaseVO.getName());
                        toolConfig.setChooseValue(toolChooseValue);
                        paramJsonList.add(toolConfig);
                    }
                }
            }
        }

        TaskCodeLibraryVO taskCodeLibrary = getRepoInfo(taskId);
        BeanUtils.copyProperties(taskEntity, taskCodeLibrary);
        taskCodeLibrary.setToolConfigList(paramJsonList);
        taskCodeLibrary.setRepoHashId(taskEntity.getRepoHashId());

        return taskCodeLibrary;
    }


    @Override
    public Boolean checkTaskExists(long taskId) {
        return taskRepository.findFirstByTaskId(taskId) != null;
    }


    /**
     * 更新代码库信息
     *
     * @param taskId
     * @param taskDetailVO
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_CODE_REPOSITORY, operType = MODIFY_INFO)
    public Boolean updateCodeLibrary(Long taskId, String userName, TaskDetailVO taskDetailVO) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }

        // 更新工具配置信息
        updateToolConfigInfoEntity(taskDetailVO, taskEntity, userName);

        // 代码仓库是否修改
        boolean repoIdUpdated = false;
        if (StringUtils.isNotEmpty(taskDetailVO.getRepoHashId())) {
            if (!taskDetailVO.getRepoHashId().equals(taskEntity.getRepoHashId())) {
                log.info("change repo for task: {}, {} -> {}", taskDetailVO.getTaskId(), taskEntity.getRepoHashId(),
                        taskDetailVO.getRepoHashId());
                repoIdUpdated = true;
            }
        }
        // 项目编译脚本是否修改
        boolean compileCommandUpdated = false;
        if (taskDetailVO.getProjectBuildCommand() != null) {
            if (!taskDetailVO.getProjectBuildCommand().equals(taskEntity.getProjectBuildCommand())) {
                log.info("code analyze task: {} compileCommand changed.", taskDetailVO.getTaskId());
                compileCommandUpdated = true;
            }
        }
        taskEntity.setRepoHashId(taskDetailVO.getRepoHashId());
        taskEntity.setBranch(taskDetailVO.getBranch());
        taskEntity.setScmType(taskDetailVO.getScmType());
        taskEntity.setAliasName(taskDetailVO.getAliasName());
        taskEntity.setOsType(StringUtils.isNotEmpty(taskDetailVO.getOsType()) ? taskDetailVO.getOsType() :
                taskEntity.getOsType());
        taskEntity.setBuildEnv(MapUtils.isNotEmpty(taskDetailVO.getBuildEnv()) ? taskDetailVO.getBuildEnv() :
                taskEntity.getBuildEnv());
        taskEntity.setProjectBuildType((StringUtils.isNotEmpty(taskDetailVO.getProjectBuildType())
                ? taskDetailVO.getProjectBuildType() : taskEntity.getProjectBuildType()));
        taskEntity.setProjectBuildCommand(StringUtils.isNotEmpty(taskDetailVO.getProjectBuildCommand())
                ? taskDetailVO.getProjectBuildCommand() : taskEntity.getProjectBuildCommand());

        BatchRegisterVO registerVO = new BatchRegisterVO();
        registerVO.setRepoHashId(taskEntity.getRepoHashId());
        registerVO.setBranch(taskEntity.getBranch());
        registerVO.setScmType(taskEntity.getScmType());
        registerVO.setOsType(taskDetailVO.getOsType());
        registerVO.setBuildEnv(taskDetailVO.getBuildEnv());
        registerVO.setProjectBuildType(taskDetailVO.getProjectBuildType());
        registerVO.setProjectBuildCommand(taskDetailVO.getProjectBuildCommand());
        // 更新流水线设置
        // 新版v3插件不需要更新model，直接codecc后台取对应数据了
        pipelineService.updateCodeLibrary(userName, registerVO, taskEntity);

        // 设置强制全量扫描标志
        if (repoIdUpdated || compileCommandUpdated) {
            setForceFullScan(taskEntity);
        }

        return taskDao.updateEntity(taskEntity, userName);
    }


    private String getRelPath(List<ToolConfigParamJsonVO> toolConfigList) {
        if (CollectionUtils.isNotEmpty(toolConfigList)) {
            for (ToolConfigParamJsonVO toolConfigParamJsonVO : toolConfigList) {
                if (Tool.GOML.name().equalsIgnoreCase(toolConfigParamJsonVO.getToolName())) {
                    if (ComConstants.PARAMJSON_KEY_REL_PATH.equalsIgnoreCase(toolConfigParamJsonVO.getVarName())) {
                        return toolConfigParamJsonVO.getChooseValue();
                    }
                }
            }
        }
        return "";
    }


    /**
     * 获取任务成员及管理员清单
     *
     * @param taskId
     * @param projectId
     * @return
     */
    @Override
    public TaskMemberVO getTaskUsers(long taskId, String projectId) {

        TaskMemberVO taskMemberVO = new TaskMemberVO();
        String taskCreateFrom = authTaskService.getTaskCreateFrom(taskId);
        if (ComConstants.BsTaskCreateFrom.BS_CODECC.value().equals(taskCreateFrom)) {
            // 获取各角色对应用户列表
            List<String> taskMembers = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_MEMBER.getCodeccActions()));
            List<String> taskOwners = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_OWNER.getCodeccActions()));
            List<String> taskViews = authExPermissionApi.queryTaskUserListForAction(String.valueOf(taskId), projectId,
                    PermissionUtil.INSTANCE.getCodeCCPermissionsFromActions(AuthRole.TASK_VIEWER.getCodeccActions()));
            taskMemberVO.setTaskMember(taskMembers);
            taskMemberVO.setTaskOwner(taskOwners);
            taskMemberVO.setTaskViewer(taskViews);
        }

        return taskMemberVO;
    }

    @Override
    public Boolean manualExecuteTaskNoProxy(long taskId, String isFirstTrigger, String userName) {
        return manualExecuteTask(taskId, isFirstTrigger, userName);
    }

    @Override
    @OperationHistory(funcId = FUNC_TRIGGER_ANALYSIS, operType = TRIGGER_ANALYSIS)
    public Boolean manualExecuteTask(long taskId, String isFirstTrigger, String userName) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList)) {
            log.info("tool list is empty! task id: {}", taskId);
            return false;
        }

        //遍历查找工具清单中是否有cloc工具，如果有的话，需要下线cloc工具
        List<ToolConfigInfoEntity> clocToolConfigInfoList = toolConfigInfoEntityList.stream()
                .filter(toolConfigInfoEntity -> Tool.CLOC.name().equalsIgnoreCase(toolConfigInfoEntity.getToolName())
                        && toolConfigInfoEntity.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                .peek(toolConfigInfoEntity -> {
                    toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                    toolConfigInfoEntity.setUpdatedBy(userName);
                    toolConfigInfoEntity.setUpdatedDate(System.currentTimeMillis());
                }).collect(Collectors.toList());
        //更新变更的工具列表
        if (CollectionUtils.isNotEmpty(clocToolConfigInfoList)) {
            toolRepository.saveAll(clocToolConfigInfoList);
        }

        List<ToolConfigInfoEntity> clocList = toolConfigInfoEntityList.stream().filter(it ->
                Tool.SCC.name().equals(it.getToolName())
        ).collect(Collectors.toList());

        if (clocList.isEmpty()) {
            ToolConfigInfoEntity clocToolConfig = new ToolConfigInfoEntity();
            long time = System.currentTimeMillis();
            clocToolConfig.setTaskId(taskInfoEntity.getTaskId());
            clocToolConfig.setToolName(Tool.SCC.name());
            clocToolConfig.setCreatedBy(userName);
            clocToolConfig.setCreatedDate(time);
            clocToolConfig.setUpdatedBy(userName);
            clocToolConfig.setUpdatedDate(time);
            clocToolConfig.setCurStep(Step4MutliTool.READY.value());
            clocToolConfig.setStepStatus(StepStatus.SUCC.value());
            clocToolConfig.setFollowStatus(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value());
            clocToolConfig.setLastFollowStatus(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value());
            toolConfigInfoEntityList.add(clocToolConfig);
            List<ToolConfigInfoEntity> tools = toolRepository.saveAll(toolConfigInfoEntityList);
            taskInfoEntity.setToolConfigInfoList(tools);
            taskRepository.save(taskInfoEntity);
            List<String> forceFullScanTools = new ArrayList<String>() {{
                add(Tool.SCC.name());
            }};
            client.get(ServiceToolBuildInfoResource.class)
                    .setForceFullScan(taskInfoEntity.getTaskId(), forceFullScanTools);
        } else if (clocList.get(0).getFollowStatus() == FOLLOW_STATUS.WITHDRAW.value()) {
            ToolConfigInfoEntity tool = clocList.get(0);
            tool.setFollowStatus(FOLLOW_STATUS.ACCESSED.value());
            toolRepository.save(tool);
        }

        Set<String> toolSet = toolConfigInfoEntityList.stream().filter(toolConfigInfoEntity ->
                        FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()
                                && !checkToolRemoved(toolConfigInfoEntity.getToolName(), taskInfoEntity))
                .map(ToolConfigInfoEntity::getToolName).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(toolSet)) {
            // 支持并发后不再停用正在运行的流水线
            //停止原有正在运行的流水线
            /*Result<Boolean> stopResult = client.get(ServiceTaskLogRestResource.class).stopRunningTask
            (taskInfoEntity.getPipelineId(), taskInfoEntity.getNameEn(),
                    toolSet, taskInfoEntity.getProjectId(), taskId, userName);
            if (stopResult.isNotOk() || null == stopResult.getData() || !stopResult.getData())
            {
                log.error("stop running pipeline fail! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }*/

            // 启动流水线
            String buildId = pipelineService.startPipeline(taskInfoEntity.getPipelineId(),
                    taskInfoEntity.getProjectId(),
                    taskInfoEntity.getNameEn(), taskInfoEntity.getCreateFrom(), new ArrayList<>(toolSet), userName);

            //更新任务状态
            toolSet.forEach(tool ->
                    pipelineService.updateTaskInitStep(isFirstTrigger, taskInfoEntity, buildId, tool, userName)
            );

            log.info("start pipeline and send delay message");
            rabbitTemplate.convertAndSend(EXCHANGE_EXPIRED_TASK_STATUS, ROUTE_EXPIRED_TASK_STATUS,
                    new ScanTaskTriggerDTO(taskId, buildId), message -> {
                        //todo 配置在配置文件里
                        message.getMessageProperties().setDelay(900 * 60 * 1000);
                        return message;
                    });
        }
        return true;
    }

    @Override
    public Boolean sendStartTaskSignal(Long taskId, String buildId, Integer timeout) {
        //todo 后续和流水线对齐
        rabbitTemplate.convertAndSend(EXCHANGE_EXPIRED_TASK_STATUS, ROUTE_EXPIRED_TASK_STATUS,
                new ScanTaskTriggerDTO(taskId, buildId), message -> {
                    message.getMessageProperties().setDelay(timeout != null ? timeout * 1000 : 24 * 60 * 60 * 1000);
                    return message;
                });
        return true;
    }


    /**
     * 通过流水线ID获取任务信息
     *
     * @param pipelineId
     * @param user
     * @return
     */
    @Override
    public PipelineTaskVO getTaskInfoByPipelineId(String pipelineId, String multiPipelineMark, String user) {
        String finalMultiPipelineMark = multiPipelineMark;
        if (StringUtils.isBlank(finalMultiPipelineMark)) {
            finalMultiPipelineMark = null;
        }
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByPipelineIdAndMultiPipelineMark(pipelineId,
                finalMultiPipelineMark);
        if (taskInfoEntity == null) {
            log.error("can not find task by pipeline id: {}", pipelineId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"pipeline id"}, null);
        }

        return convertTaskInfoEntityToVo(taskInfoEntity);
    }

    @Override
    public List<PipelineTaskVO> getTaskInfoByPipelineId(String pipelineId, String user) {
        List<TaskInfoEntity> taskInfoEntities = taskRepository.findAllByPipelineId(pipelineId);
        if (CollectionUtils.isEmpty(taskInfoEntities)) {
            return Collections.emptyList();
        }
        List<PipelineTaskVO> pipelineTaskVOS = new ArrayList<>();
        taskInfoEntities.forEach(entry -> {
            pipelineTaskVOS.add(convertTaskInfoEntityToVo(entry));
        });
        return pipelineTaskVOS;
    }

    @Override
    public List<Long> getTaskIdsByPipelineId(String pipelineId, String user) {
        List<TaskInfoEntity> taskInfoEntities = taskRepository.findAllByPipelineId(pipelineId);
        if (CollectionUtils.isEmpty(taskInfoEntities)) {
            return Collections.emptyList();
        }
        return taskInfoEntities.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
    }

    private PipelineTaskVO convertTaskInfoEntityToVo(TaskInfoEntity taskInfoEntity) {
        PipelineTaskVO taskDetailVO = new PipelineTaskVO();
        taskDetailVO.setProjectId(taskInfoEntity.getProjectId());
        taskDetailVO.setTaskId(taskInfoEntity.getTaskId());
        taskDetailVO.setTools(Lists.newArrayList());
        taskDetailVO.setEnName(taskInfoEntity.getNameEn());
        taskDetailVO.setCnName(taskInfoEntity.getNameCn());
        taskDetailVO.setAutoLang(taskInfoEntity.getAutoLang());

        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList()) {
                // modified by neildwu 2021-04-20 ，存量项目去除cloc工具
                if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus()
                        && !ComConstants.Tool.CLOC.name().equalsIgnoreCase(toolConfigInfoEntity.getToolName())
                        && !checkToolRemoved(toolConfigInfoEntity.getToolName(), taskInfoEntity)) {
                    PipelineToolVO pipelineToolVO = new PipelineToolVO();
                    pipelineToolVO.setToolName(toolConfigInfoEntity.getToolName());
                    if (toolConfigInfoEntity.getCheckerSet() != null) {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), checkerSetVO);
                        pipelineToolVO.setCheckerSetInUse(checkerSetVO);
                    }
                    if (StringUtils.isNotEmpty(toolConfigInfoEntity.getParamJson())) {
                        pipelineToolVO.setParams(getParams(toolConfigInfoEntity.getParamJson()));
                    }
                    taskDetailVO.getTools().add(pipelineToolVO);
                }
            }
        }
        // 加入语言的显示名称
        List<String> codeLanguages = taskInfoEntity.getCodeLang() != null
                ? pipelineService.localConvertDevopsCodeLang(taskInfoEntity.getCodeLang()) : Collections.emptyList();
        taskDetailVO.setCodeLanguages(codeLanguages);
        return taskDetailVO;
    }

    /**
     * 检查工具是否已经下架
     *
     * @param toolName
     * @param taskInfoEntity
     * @return true:已下架，false:未下架
     */
    @Override
    public boolean checkToolRemoved(String toolName, TaskInfoEntity taskInfoEntity) {
        // 指定项目不限制，可以执行coverity
        try {
            Set<String> projectIdSet = toolLicenseWhiteListCache.get(toolName);
            if (CollectionUtils.isNotEmpty(projectIdSet) && projectIdSet.contains(taskInfoEntity.getProjectId())) {
                return false;
            }
        } catch (ExecutionException e) {
            log.warn("get tool license white list exception: {}, {}, {}",
                    toolName, taskInfoEntity.getProjectId(), taskInfoEntity.getTaskId());
        }

        ToolMetaBaseVO toolMetaBase = toolMetaCache.getToolBaseMetaCache(toolName);
        if (ComConstants.ToolIntegratedStatus.D.name().equals(toolMetaBase.getStatus())) {
            log.info("tool was removed: {}, {}", toolName, taskInfoEntity.getTaskId());
            return true;
        }

        return false;
    }


    @Override
    public Long getTaskIdByPipelineInfo(String pipelineId, String multiPipelineMark) {
        String finalMultiPipelineMark = multiPipelineMark;
        if (StringUtils.isBlank(finalMultiPipelineMark)) {
            finalMultiPipelineMark = null;
        }
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByPipelineIdAndMultiPipelineMark(pipelineId,
                finalMultiPipelineMark);
        if (null == taskInfoEntity) {
            return null;
        }
        return taskInfoEntity.getTaskId();
    }

    @Override
    public TaskStatusVO getTaskStatus(Long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return null;
        }
        return new TaskStatusVO(taskInfoEntity.getStatus(), taskInfoEntity.getGongfengProjectId());
    }

    /**
     * 获取所有的基础工具信息
     *
     * @return
     */
    @Override
    public Map<String, ToolMetaBaseVO> getToolMetaListFromCache() {
        return toolMetaCache.getToolMetaListFromCache(Boolean.TRUE, Boolean.FALSE);
    }

    @Override
    public TaskInfoEntity getTaskById(Long taskId) {
        return taskRepository.findFirstByTaskId(taskId);
    }


    @Override
    public List<TaskBaseVO> getTasksByBgId(Integer bgId) {
        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findByBgId(bgId);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            return taskInfoEntityList.stream().map(taskInfoEntity -> {
                TaskBaseVO taskBaseVO = new TaskBaseVO();
                BeanUtils.copyProperties(taskInfoEntity, taskBaseVO,
                        "taskOwner", "executeDate", "enableToolList", "disableToolList");
                return taskBaseVO;
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<TaskBaseVO> getTasksByIds(List<Long> taskIds) {
        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findByTaskIdIn(taskIds);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            return taskInfoEntityList.stream().map(taskInfoEntity -> {
                TaskBaseVO taskBaseVO = new TaskBaseVO();
                BeanUtils.copyProperties(taskInfoEntity, taskBaseVO,
                        "taskOwner", "executeDate", "enableToolList", "disableToolList");
                return taskBaseVO;
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskEntity
     */
    @Override
    public void setForceFullScan(TaskInfoEntity taskEntity) {
        if (CollectionUtils.isNotEmpty(taskEntity.getToolConfigInfoList())) {
            List<String> setForceFullScanToolNames = Lists.newArrayList();
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskEntity.getToolConfigInfoList()) {
                setForceFullScanToolNames.add(toolConfigInfoEntity.getToolName());
            }
            log.info("set force full scan, taskId:{}, toolNames:{}", taskEntity.getTaskId(), setForceFullScanToolNames);
            Result<Boolean> toolBuildInfoVOResult =
                    client.get(ServiceToolBuildInfoResource.class).setForceFullScan(taskEntity.getTaskId(),
                            setForceFullScanToolNames);
            if (toolBuildInfoVOResult == null || toolBuildInfoVOResult.isNotOk()) {
                log.error("set force full san failed! taskId={}, toolNames={}", taskEntity.getScanType(),
                        setForceFullScanToolNames);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL,
                        new String[]{"set force full san failed!"}, null);
            }
        }
    }

    /**
     * 修改任务扫描触发配置
     *
     * @param taskId
     * @param user
     * @param scanConfigurationVO
     * @return
     */
    @Override
    @OperationHistory(funcId = FUNC_SCAN_SCHEDULE, operType = MODIFY_INFO)
    public Boolean updateScanConfiguration(Long taskId, String user, ScanConfigurationVO scanConfigurationVO) {
        // 更新定时分析配置
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (scanConfigurationVO.getTimeAnalysisConfig() != null
                && BsTaskCreateFrom.BS_CODECC.value().equals(taskInfoEntity.getCreateFrom())) {
            TimeAnalysisConfigVO timeAnalysisConfigVO = scanConfigurationVO.getTimeAnalysisConfig();
            if (timeAnalysisConfigVO != null) {
                // 调用Kotlin方法时需要去掉null
                if (timeAnalysisConfigVO.getExecuteDate() == null) {
                    timeAnalysisConfigVO.setExecuteDate(Lists.newArrayList());
                }
                if (timeAnalysisConfigVO.getExecuteTime() == null) {
                    timeAnalysisConfigVO.setExecuteTime("");
                }
                //保存任务清单
                pipelineService.modifyCodeCCTiming(taskInfoEntity, timeAnalysisConfigVO.getExecuteDate(),
                        timeAnalysisConfigVO.getExecuteTime(), user);
            }
        }

        // 需要设置强制全量，避免走快速增量的逻辑的情况
        // 1、扫描方式由快速全量或差异扫描变成全量
        // 2、从差异扫描变成快速全量
        // 3、编译型工具有修改编译脚本
        // 更新扫描方式
        if (scanConfigurationVO.getScanType() != null) {
            if ((taskInfoEntity.getScanType() == ComConstants.ScanType.INCREMENTAL.code
                    || taskInfoEntity.getScanType() == ComConstants.ScanType.BRANCH_DIFF_MODE.code)
                    && scanConfigurationVO.getScanType() == ComConstants.ScanType.FULL.code) {
                log.info("task: {}, scanType from increment/diff change to full.", taskId);
                setForceFullScan(taskInfoEntity);
            } else if (taskInfoEntity.getScanType() == ComConstants.ScanType.BRANCH_DIFF_MODE.code
                    && scanConfigurationVO.getScanType() == ComConstants.ScanType.INCREMENTAL.code) {
                log.info("task: {}, scanType from diff change to increment.", taskId);
                setForceFullScan(taskInfoEntity);
            }
            taskInfoEntity.setScanType(scanConfigurationVO.getScanType());
        }

        // 更新项目编译脚本
        if (scanConfigurationVO.getCompileCommand() != null) {
            if (!scanConfigurationVO.getCompileCommand().equals(taskInfoEntity.getProjectBuildCommand())) {
                log.info("task: {}, compileCommand changed.", taskId);
                setForceFullScan(taskInfoEntity);
            }
            taskInfoEntity.setProjectBuildCommand(scanConfigurationVO.getCompileCommand());
        }

        // 更新告警作者转换配置
        authorTransfer(taskId, scanConfigurationVO, taskInfoEntity);

        // 更新扫描方式
        if (scanConfigurationVO.getMrCommentEnable() != null) {
            taskInfoEntity.setMrCommentEnable(scanConfigurationVO.getMrCommentEnable());
        }

        //更新是否允许页面忽略告警配置
        Boolean prohibitIgnore = scanConfigurationVO.getProhibitIgnore();
        if (null != prohibitIgnore) {
            taskInfoEntity.setProhibitIgnore(prohibitIgnore);
        }

        taskRepository.save(taskInfoEntity);
        return true;
    }

    @Override
    public Boolean authorTransferForApi(Long taskId, List<ScanConfigurationVO.TransferAuthorPair> transferAuthorPairs,
            String userId) {
        log.info("api author transfer function, user id: {}, task id: {}", userId, taskId);
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        ScanConfigurationVO scanConfigurationVO = new ScanConfigurationVO();
        scanConfigurationVO.setTransferAuthorList(transferAuthorPairs);
        authorTransfer(taskId, scanConfigurationVO, taskInfoEntity);
        return true;
    }

    private void authorTransfer(Long taskId, ScanConfigurationVO scanConfigurationVO, TaskInfoEntity taskInfoEntity) {
        AuthorTransferVO authorTransferVO = new AuthorTransferVO();
        authorTransferVO.setTaskId(taskId);
        List<String> tools = toolService.getEffectiveToolList(taskInfoEntity);
        authorTransferVO.setEffectiveTools(tools);
        List<ScanConfigurationVO.TransferAuthorPair> transferAuthorList = scanConfigurationVO.getTransferAuthorList();
        if (CollectionUtils.isNotEmpty(transferAuthorList)) {
            List<AuthorTransferVO.TransferAuthorPair> newTransferAuthorList = transferAuthorList.stream()
                    .map(authorPair -> {
                        AuthorTransferVO.TransferAuthorPair transferAuthorPair =
                                new AuthorTransferVO.TransferAuthorPair();
                        transferAuthorPair.setSourceAuthor(authorPair.getSourceAuthor());
                        transferAuthorPair.setTargetAuthor(authorPair.getTargetAuthor());
                        return transferAuthorPair;
                    })
                    .collect(Collectors.toList());
            authorTransferVO.setTransferAuthorList(newTransferAuthorList);
        }
        authorTransferBizService.authorTransfer(authorTransferVO);
    }

    /**
     * 更新工具配置信息
     *
     * @param taskDetailVO
     * @param taskEntity
     */
    private void updateToolConfigInfoEntity(TaskDetailVO taskDetailVO, TaskInfoEntity taskEntity, String userName) {
        // 获取当前任务的工具的配置信息
        List<ToolConfigInfoEntity> toolConfigList = taskEntity.getToolConfigInfoList();
        // 提交更新任务工具的配置信息
        List<ToolConfigParamJsonVO> updateToolConfigList = taskDetailVO.getDevopsToolParams();

        //根据原有的和提交的，更新工具参数

        if (CollectionUtils.isNotEmpty(toolConfigList) && CollectionUtils.isNotEmpty(updateToolConfigList)) {
            //提交参数map
            Map<String, String> paramMap = updateToolConfigList.stream().collect(Collectors.toMap(
                    ToolConfigParamJsonVO::getVarName, ToolConfigParamJsonVO::getChooseValue
            ));
            toolConfigList.forEach(toolConfigInfoEntity -> {
                ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolConfigInfoEntity.getToolName());
                String toolParamJson = toolMetaBaseVO.getParams();
                if (StringUtils.isEmpty(toolParamJson)) {
                    return;
                }
                //原有参数
                String previousParamJson = toolConfigInfoEntity.getParamJson();
                JSONObject previousParamObj = StringUtils.isNotBlank(previousParamJson)
                        ? JSONObject.fromObject(previousParamJson) : new JSONObject();
                JSONArray toolParamsArray = new JSONArray(toolParamJson);
                for (int i = 0; i < toolParamsArray.length(); i++) {
                    org.json.JSONObject paramJsonObj = toolParamsArray.getJSONObject(i);
                    String varName = paramJsonObj.getString("varName");
                    String varValue = paramMap.get(varName);
                    if (StringUtils.isNotEmpty(varValue)) {
                        previousParamObj.put(varName, varValue);
                    }
                }
                toolConfigInfoEntity.setParamJson(previousParamObj.toString());
                toolConfigInfoEntity.setUpdatedBy(userName);
                toolConfigInfoEntity.setUpdatedDate(System.currentTimeMillis());
                toolRepository.save(toolConfigInfoEntity);
            });
        }
    }


    /**
     * 获取工具配置Map
     *
     * @param taskEntity
     * @return
     */
    @NotNull
    private Map<String, JSONObject> getToolConfigInfoMap(TaskInfoEntity taskEntity) {
        // 获取工具配置的值
        List<ToolConfigInfoEntity> toolConfigInfoList = taskEntity.getToolConfigInfoList();
        Map<String, JSONObject> chooseJsonMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolConfigInfoList)) {
            // 排除下架的工具
            toolConfigInfoList.stream()
                    .filter(config -> config.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                    .forEach(config -> {
                        String paramJson = config.getParamJson();
                        JSONObject params = new JSONObject();
                        if (StringUtils.isNotBlank(paramJson) && !ComConstants.STRING_NULL_ARRAY.equals(paramJson)) {
                            params = JSONObject.fromObject(paramJson);
                        }
                        chooseJsonMap.put(config.getToolName(), params);
                    });
        }
        return chooseJsonMap;
    }


    /**
     * 判断提交的参数是否为空
     *
     * @param taskUpdateVO
     * @return
     */
    private Boolean checkParam(TaskUpdateVO taskUpdateVO) {
        if (StringUtils.isBlank(taskUpdateVO.getNameCn())) {
            return false;
        }
        return taskUpdateVO.getCodeLang() > 0;
    }

    /**
     * 检查参数并赋默认值
     *
     * @param reqVO req
     */
    private void checkParam(QueryMyTasksReqVO reqVO) {
        if (reqVO.getPageNum() == null) {
            reqVO.setPageNum(1);
        }

        if (reqVO.getPageSize() == null) {
            reqVO.setPageSize(10);
        }

        if (reqVO.getSortField() == null) {
            reqVO.setSortField("taskId");
        }
    }

    /**
     * 给工具分类及排序
     *
     * @param taskBaseVO
     * @param toolEntityList
     */
    private void sortedToolList(TaskBaseVO taskBaseVO, List<ToolConfigInfoEntity> toolEntityList) {
        // 如果工具不为空，对工具排序并且赋值工具展示名
        if (CollectionUtils.isNotEmpty(toolEntityList)) {
            List<ToolConfigBaseVO> enableToolList = new ArrayList<>();
            List<ToolConfigBaseVO> disableToolList = new ArrayList<>();

            List<String> toolIDArr = getToolOrders();
            for (String toolName : toolIDArr) {
                for (ToolConfigInfoEntity toolEntity : toolEntityList) {
                    if (toolName.equals(toolEntity.getToolName())) {
                        ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
                        BeanUtils.copyProperties(toolEntity, toolConfigBaseVO);
                        ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
                        toolConfigBaseVO.setToolDisplayName(toolMetaBaseVO.getDisplayName());
                        toolConfigBaseVO.setToolPattern(toolMetaBaseVO.getPattern());
                        toolConfigBaseVO.setToolType(toolMetaBaseVO.getType());

                        // 加入规则集
                        if (toolEntity.getCheckerSet() != null) {
                            CheckerSetVO checkerSetVO = new CheckerSetVO();
                            BeanUtils.copyProperties(toolEntity.getCheckerSet(), checkerSetVO);
                            toolConfigBaseVO.setCheckerSet(checkerSetVO);
                        }

                        if (TaskConstants.FOLLOW_STATUS.WITHDRAW.value() == toolConfigBaseVO.getFollowStatus()) {
                            disableToolList.add(toolConfigBaseVO);
                        } else {
                            enableToolList.add(toolConfigBaseVO);
                        }
                    }
                }
            }

            taskBaseVO.setEnableToolList(enableToolList);
            taskBaseVO.setDisableToolList(disableToolList);
        }
    }


    /**
     * 重置工具的顺序，数据库中工具是按接入的先后顺序排序的，前端展示要按照工具类型排序
     *
     * @param toolNames
     * @param toolIdsOrder
     * @return
     */
    private String resetToolOrderByType(String toolNames, final String toolIdsOrder,
            List<ToolConfigInfoVO> unsortedToolList,
            List<ToolConfigInfoVO> sortedToolList) {
        if (StringUtils.isEmpty(toolNames)) {
            return null;
        }

        String[] toolNamesArr = toolNames.split(",");
        List<String> originToolList = Arrays.asList(toolNamesArr);
        String[] toolIDArr = toolIdsOrder.split(",");
        List<String> orderToolList = Arrays.asList(toolIDArr);
        Iterator<String> it = orderToolList.iterator();
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            String toolId = it.next();
            if (originToolList.contains(toolId)) {
                sb.append(toolId).append(",");
                List<ToolConfigInfoVO> filteredList = unsortedToolList.stream()
                        .filter(toolConfigInfoVO ->
                                toolId.equalsIgnoreCase(toolConfigInfoVO.getToolName())
                        ).collect(Collectors.toList());

                sortedToolList.addAll(CollectionUtils.isNotEmpty(filteredList) ? filteredList : Collections.EMPTY_LIST);
            }
        }
        if (sb.length() > 0) {
            toolNames = sb.substring(0, sb.length() - 1);
        }

        return toolNames;
    }


    protected TaskListVO sortByDate(
            TaskListReqVO taskListReqVO, List<TaskDetailVO> taskDetailVOS, TaskSortType taskSortType) {
        log.info("sortByDate taskListReqVO: [{}]", taskListReqVO);
        if (taskListReqVO.getShowDisabledTask() != null) {
            if (!taskListReqVO.getShowDisabledTask()) {
                // 删除集合中停用的任务
                taskDetailVOS.removeIf(taskDetailVO -> (taskDetailVO.getStatus() != null
                        && taskDetailVO.getStatus().equals(TaskConstants.TaskStatus.DISABLE.value())));
            }
        }
        TaskListVO taskList = new TaskListVO();
        List<TaskDetailVO> enableProjs = new ArrayList<>();
        List<TaskDetailVO> disableProjs = new ArrayList<>();
        for (TaskDetailVO taskDetailVO : taskDetailVOS) {
            if (taskListReqVO.getPageable() != null && taskListReqVO.getPageable()) {
                enableProjs.add(taskDetailVO);
            } else {
                if (!TaskConstants.TaskStatus.DISABLE.value().equals(taskDetailVO.getStatus())) {
                    enableProjs.add(taskDetailVO);
                } else {
                    disableProjs.add(taskDetailVO);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(taskDetailVOS)) {
            //分离已启用项目和停用项目

            //启用的项目按创建时间倒排,如果有置顶就放在最前面
            switch (taskSortType) {
                case CREATE_DATE:
                    enableProjs.sort((o1, o2) -> o2.getTopFlag() - o1.getTopFlag() == 0
                            ? o2.getCreatedDate().compareTo(o1.getCreatedDate()) :
                            o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                case LAST_EXECUTE_DATE:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0
                                    ? o2.getMinStartTime().compareTo(o1.getMinStartTime()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                case SIMPLIFIED_PINYIN:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0 ? Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                    .compare(StringUtils.isNotBlank(o1.getNameCn()) ? o1.getNameCn() : o1.getNameEn(),
                                            StringUtils.isNotBlank(o2.getNameCn()) ? o2.getNameCn() : o2.getNameEn()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
                default:
                    enableProjs.sort((o1, o2) ->
                            o2.getTopFlag() - o1.getTopFlag() == 0
                                    ? o2.getCreatedDate().compareTo(o1.getCreatedDate()) :
                                    o2.getTopFlag() - o1.getTopFlag()
                    );
                    break;
            }
        }

        if (taskListReqVO.getPageable() != null && taskListReqVO.getPageable()) {
            int startIndex = (taskListReqVO.getPage() - 1) * taskListReqVO.getPageSize();
            List<TaskDetailVO> pageProjs = enableProjs.stream()
                    .skip(startIndex)
                    .limit(taskListReqVO.getPageSize())
                    .collect(Collectors.toList());
            Result<List<MetricsVO>> result = client.get(ServiceMetricsRestResource.class)
                    .getMetrics(pageProjs.stream().map(TaskDetailVO::getTaskId).collect(Collectors.toList()));
            if (result.isOk() && result.getData() != null) {
                Map<Long, MetricsVO> metricsMap = result.getData()
                        .stream()
                        .collect(Collectors.toMap(MetricsVO::getTaskId, a -> a, (t1, t2) -> t1));
                pageProjs.forEach(taskDetailVO -> {
                    MetricsVO metricsVO;
                    if ((metricsVO = metricsMap.get(taskDetailVO.getTaskId())) != null) {
                        taskDetailVO.setTotalStyleDefectCount(metricsVO.getTotalStyleDefectCount());
                        taskDetailVO.setTotalDefectCount(metricsVO.getTotalDefectCount());
                        taskDetailVO.setTotalSecurityDefectCount(metricsVO.getTotalSecurityDefectCount());
                        taskDetailVO.setRdIndicatorsScore(metricsVO.getRdIndicatorsScore());
                        taskDetailVO.setOpenScan(metricsVO.isOpenScan());
                    }
                });
            }
            Pageable pageable = PageRequest.of(taskListReqVO.getPage() - 1, taskListReqVO.getPageSize());
            taskList.setPageTasks(
                    new PageImpl<>(pageProjs, pageable, enableProjs.size()));
        } else {
            taskList.setEnableTasks(enableProjs);
            taskList.setDisableTasks(disableProjs);
        }
        return taskList;
    }

    /**
     * 获取工具排序
     *
     * @return
     */
    private List<String> getToolOrders() {
        String toolIdsOrder = commonDao.getToolOrder();
        return List2StrUtil.fromString(toolIdsOrder, ComConstants.STRING_SPLIT);
    }

    /**
     * 获取工具特殊参数列表
     *
     * @param paramJsonStr
     * @return
     */
    private List<PipelineToolParamVO> getParams(String paramJsonStr) {
        List<PipelineToolParamVO> params = Lists.newArrayList();
        if (StringUtils.isNotEmpty(paramJsonStr)) {
            JSONObject paramJson = JSONObject.fromObject(paramJsonStr);
            if (paramJson != null && !paramJson.isNullObject()) {
                for (Object paramKeyObj : paramJson.keySet()) {
                    String paramKey = (String) paramKeyObj;
                    PipelineToolParamVO pipelineToolParamVO = new PipelineToolParamVO(paramKey,
                            paramJson.getString(paramKey));
                    params.add(pipelineToolParamVO);
                }
            }
        }
        return params;
    }


    /**
     * 根据条件获取任务基本信息清单
     *
     * @param taskListReqVO 请求体对象
     * @return list
     */
    @Override
    public TaskListVO getTaskDetailList(QueryTaskListReqVO taskListReqVO) {
        Integer taskStatus = taskListReqVO.getStatus();
        String toolName = taskListReqVO.getToolName();
        Integer bgId = taskListReqVO.getBgId();
        Integer deptId = taskListReqVO.getDeptId();
        List<String> createFrom = taskListReqVO.getCreateFrom();
        Boolean isExcludeTaskIds = Boolean.valueOf(taskListReqVO.getIsExcludeTaskIds());
        List<Long> taskIdsReq = Lists.newArrayList(taskListReqVO.getTaskIds());

        List<Integer> deptIds = null;
        if (deptId != null && deptId != 0) {
            deptIds = Lists.newArrayList(deptId);
        }

        TaskListVO taskList = new TaskListVO(Collections.emptyList(), Collections.emptyList());
        List<TaskDetailVO> tasks = new ArrayList<>();

        // 根据isExcludeTaskIds来判断参数taskIdsReq 的处理方式，来获取任务ID列表
        List<Long> queryTaskIds = getTaskIdListByFlag(toolName, isExcludeTaskIds, taskIdsReq);

        // 根据任务状态获取注册过该工具的任务列表
        List<TaskInfoEntity> taskInfoEntityList =
                taskDao.queryTaskInfoEntityList(taskStatus, bgId, deptIds, queryTaskIds, createFrom, null);
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            taskInfoEntityList.forEach(entity -> {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                BeanUtils.copyProperties(entity, taskDetailVO);
                tasks.add(taskDetailVO);
            });
        }

        if (Status.ENABLE.value() == taskStatus) {
            taskList.setEnableTasks(tasks);
        } else {
            taskList.setDisableTasks(tasks);
        }
        return taskList;
    }

    /**
     * 根据isExcludeTaskIds来判断参数taskIdsReq 的处理方式，来获取任务ID列表
     *
     * @param toolName 工具名称
     * @param isExcludeTaskIds true: 排除taskIdsReq false: 从taskIdsReq排除
     * @param taskIdsReq 参数(任务ID列表)
     * @return task id list
     */
    @NotNull
    private List<Long> getTaskIdListByFlag(String toolName, Boolean isExcludeTaskIds, List<Long> taskIdsReq) {
        List<Long> queryTaskIds;
        if (BooleanUtils.isTrue(isExcludeTaskIds)) {
            List<Long> notWithdrawTasks = Lists.newArrayList();
            List<ToolConfigInfoEntity> toolConfigInfos =
                    toolRepository.findByToolNameAndFollowStatusNot(toolName, FOLLOW_STATUS.WITHDRAW.value());
            if (CollectionUtils.isNotEmpty(toolConfigInfos)) {
                toolConfigInfos.forEach(entity -> notWithdrawTasks.add(entity.getTaskId()));
            }
            // 剔除参数taskListReqVO的任务
            notWithdrawTasks.removeAll(taskIdsReq);
            queryTaskIds = notWithdrawTasks;
        } else {
            List<Long> withdrawTasks = Lists.newArrayList();
            List<ToolConfigInfoEntity> toolConfigInfos = toolRepository.findByToolNameAndFollowStatusIs(toolName,
                    FOLLOW_STATUS.WITHDRAW.value());
            if (CollectionUtils.isNotEmpty(toolConfigInfos)) {
                toolConfigInfos.forEach(entity -> withdrawTasks.add(entity.getTaskId()));
            }
            // 剔除已下架该工具的任务
            taskIdsReq.removeAll(withdrawTasks);
            queryTaskIds = taskIdsReq;
        }
        return queryTaskIds;
    }


    @Override
    public Page<TaskInfoVO> getTasksByAuthor(QueryMyTasksReqVO reqVO) {
        checkParam(reqVO);
        String repoUrl = reqVO.getRepoUrl();
        String branch = reqVO.getBranch();

        List<TaskInfoVO> tasks = Lists.newArrayList();

        List<TaskInfoEntity> allUserTasks =
                taskRepository.findTaskList(reqVO.getAuthor(), TaskConstants.TaskStatus.ENABLE.value());

        if (CollectionUtils.isNotEmpty(allUserTasks)) {
            Set<String> taskProjectIdList = Sets.newHashSet();
            allUserTasks.forEach(task -> {
                String bkProjectId = task.getProjectId();
                if (StringUtils.isNotEmpty(bkProjectId)) {
                    taskProjectIdList.add(bkProjectId);
                }
            });

            Map<String, RepoInfoVO> repoInfoVoMap = pipelineService.getRepoUrlByBkProjects(taskProjectIdList);
            String repoHashId = "";
            RepoInfoVO repoInfoVO = repoInfoVoMap.get(repoUrl);
            if (repoInfoVO != null) {
                repoHashId = repoInfoVO.getRepoHashId();
            }

            for (TaskInfoEntity task : allUserTasks) {
                // 过滤任务
                if (taskFilterIsTrue(branch, repoHashId, task)) {
                    continue;
                }

                TaskInfoVO taskInfoVO = new TaskInfoVO();
                taskInfoVO.setTaskId(task.getTaskId());
                taskInfoVO.setNameCn(task.getNameCn());
                taskInfoVO.setProjectId(task.getProjectId());

                List<String> tools = Lists.newArrayList();
                task.getToolConfigInfoList().forEach(toolInfo -> {
                    // 过滤掉已停用
                    if (toolInfo != null && toolInfo.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value()) {
                        tools.add(toolInfo.getToolName());
                    }
                });

                taskInfoVO.setToolNames(tools);
                tasks.add(taskInfoVO);
            }
        }

        return sortAndPage(reqVO.getPageNum(), reqVO.getPageSize(), reqVO.getSortType(), reqVO.getSortField(), tasks);
    }

    @Override
    public void updateReportInfo(Long taskId, NotifyCustomVO reqVO) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        NotifyCustomEntity previousNofityEntity = taskInfoEntity.getNotifyCustomInfo();
        log.info("update report info from build, task id: {}, before: {}", taskId, previousNofityEntity);

        OperationType operationType;
        String existsJobName = "";
        if (null != previousNofityEntity && StringUtils.isNotEmpty(previousNofityEntity.getReportJobName())) {
            operationType = OperationType.RESCHEDULE;
            existsJobName = previousNofityEntity.getReportJobName();
        } else {
            operationType = OperationType.ADD;
        }

        NotifyCustomEntity notifyCustomEntity = new NotifyCustomEntity();
        BeanUtils.copyProperties(reqVO, notifyCustomEntity);
        // reqVO没有传递jobName
        notifyCustomEntity.setReportJobName(existsJobName);

        //如果定时任务信息不为空，则与定时调度平台通信
        if (CollectionUtils.isNotEmpty(notifyCustomEntity.getReportDate())
                && null != notifyCustomEntity.getReportTime()
                && CollectionUtils.isNotEmpty(notifyCustomEntity.getReportTools())) {
            // 新增任务会返回新的jobName；否则返回existsJobName
            String finalJobName = emailNotifyService.addEmailScheduleTask(
                    taskId,
                    notifyCustomEntity.getReportDate(),
                    notifyCustomEntity.getReportTime(),
                    operationType,
                    existsJobName
            );
            notifyCustomEntity.setReportJobName(finalJobName);
        }

        log.info("update report info from build, task id: {}, after: {}", taskId, notifyCustomEntity);
        taskInfoEntity.setNotifyCustomInfo(notifyCustomEntity);
        taskRepository.save(taskInfoEntity);
    }

    @Override
    public Boolean updateTopUserInfo(Long taskId, String user, Boolean topFlag) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return false;
        }
        Set<String> topUser = taskInfoEntity.getTopUser();
        //如果是置顶操作
        if (topFlag) {
            if (CollectionUtils.isEmpty(topUser)) {
                taskInfoEntity.setTopUser(new HashSet<String>() {
                    {
                        add(user);
                    }
                });
            } else {
                topUser.add(user);
            }
        } else { //如果是取消置顶操作
            if (CollectionUtils.isEmpty(topUser)) {
                log.error("top user list is empty! task id: {}", taskId);
                return false;
            } else {
                topUser.remove(user);
            }
        }

        taskRepository.save(taskInfoEntity);
        return true;
    }


    @Override
    public List<TaskDetailVO> getTaskInfoList(QueryTaskListReqVO taskListReqVO) {
        List<TaskInfoEntity> taskInfoEntityList =
                taskDao.queryTaskInfoEntityList(taskListReqVO.getStatus(), taskListReqVO.getBgId(),
                        taskListReqVO.getDeptIds(), taskListReqVO.getTaskIds(),
                        taskListReqVO.getCreateFrom(), taskListReqVO.getUserId());

        return entities2TaskDetailVoList(taskInfoEntityList);
    }

    @Override
    public Page<TaskDetailVO> getTaskDetailPage(@NotNull QueryTaskListReqVO reqVO) {
        Sort.Direction direction = Sort.Direction.valueOf(reqVO.getSortType());
        Pageable pageable = PageableUtils
                .getPageable(reqVO.getPageNum(), reqVO.getPageSize(), reqVO.getSortField(), direction, "task_id");

        org.springframework.data.domain.Page<TaskInfoEntity> entityPage = taskRepository
                .findByStatusAndBgIdAndDeptIdInAndCreateFromIn(reqVO.getStatus(), reqVO.getBgId(), reqVO.getDeptIds(),
                        reqVO.getCreateFrom(), pageable);
        List<TaskInfoEntity> taskInfoEntityList = entityPage.getContent();

        List<TaskDetailVO> taskInfoList = entities2TaskDetailVoList(taskInfoEntityList);

        // 页码+1展示
        return new Page<>(entityPage.getTotalElements(), entityPage.getNumber() + 1, entityPage.getSize(),
                entityPage.getTotalPages(), taskInfoList);
    }

    @NotNull
    private List<TaskDetailVO> entities2TaskDetailVoList(List<TaskInfoEntity> taskInfoEntityList) {
        List<TaskDetailVO> taskInfoList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(taskInfoEntityList)) {
            taskInfoList = taskInfoEntityList.stream().map(taskInfoEntity -> {
                TaskDetailVO taskDetailVO = new TaskDetailVO();
                BeanUtils.copyProperties(taskInfoEntity, taskDetailVO);
                return taskDetailVO;
            }).collect(Collectors.toList());
        }
        return taskInfoList;
    }


    private boolean taskFilterIsTrue(String branch, String repoHashId, TaskInfoEntity task) {
        // 如果不是工蜂代码扫描创建的任务
        String createFrom = task.getCreateFrom();
        if (!BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom)) {
            // 过滤代码库不匹配的
            String taskRepoHashId = task.getRepoHashId();
            if (StringUtils.isBlank(taskRepoHashId) || !taskRepoHashId.equals(repoHashId)) {
                return true;
            }
            // 过滤分支不符合的，参数branch为null则不检查
            if (branch != null && !branch.equals(task.getBranch())) {
                return true;
            }
        }
        // 过滤未添加工具的
        return task.getToolConfigInfoList() == null;
    }

    private void setEmptyAnalyze(TaskInfoEntity taskEntity, TaskOverviewVO taskOverviewVO) {
        List<LastAnalysis> lastAnalyses = new ArrayList<>();
        if (taskEntity.getToolConfigInfoList() != null) {
            taskEntity.getToolConfigInfoList().forEach(toolConfigInfoEntity -> {
                LastAnalysis lastAnalysis = new LastAnalysis();
                lastAnalysis.setToolName(toolConfigInfoEntity.getToolName());
                lastAnalyses.add(lastAnalysis);
            });
        }
        taskOverviewVO.setLastAnalysisResultList(lastAnalyses);
    }


    @NotNull
    private Page<TaskInfoVO> sortAndPage(int pageNum, int pageSize, String sortType, String sortField,
            List<TaskInfoVO> tasks) {
        if (!Sort.Direction.ASC.name().equalsIgnoreCase(sortType)) {
            sortType = Sort.Direction.DESC.name();
        }
        ListSortUtil.sort(tasks, sortField, sortType);

        int totalPageNum = 0;
        int total = tasks.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        if (total > 0) {
            totalPageNum = (total + pageSize - 1) / pageSize;
        }

        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        List<TaskInfoVO> taskInfoVoList = tasks.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        return new Page<>(total, pageNum == 0 ? 1 : pageNum, pageSize, totalPageNum, taskInfoVoList);
    }


    @Override
    public Set<Integer> queryDeptIdByBgId(Integer bgId) {
        // 指定工蜂扫描的部门ID
        List<TaskInfoEntity> deptIdList = taskDao.queryDeptId(bgId, BsTaskCreateFrom.GONGFENG_SCAN.value());

        return deptIdList.stream().filter(elem -> elem.getDeptId() > 0).map(TaskInfoEntity::getDeptId)
                .collect(Collectors.toSet());
    }

    @Override
    public Boolean refreshTaskOrgInfo(Long taskId) {
        return true;
    }

    @Override
    public void updateTaskOwnerAndMember(TaskOwnerAndMemberVO vo, Long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            return;
        }

        if (vo.getTaskOwner() == null) {
            vo.setTaskOwner(Lists.newArrayList());
        } else {
            vo.getTaskOwner().removeIf(StringUtils::isEmpty);
        }

        if (vo.getTaskMember() == null) {
            vo.setTaskMember(Lists.newArrayList());
        } else {
            vo.getTaskMember().removeIf(StringUtils::isEmpty);
        }

        taskInfoEntity.setTaskMember(vo.getTaskMember());
        taskInfoEntity.setTaskOwner(vo.getTaskOwner());
        taskRepository.save(taskInfoEntity);
    }

    @Override
    public Boolean triggerBkPluginScoring() {
        rabbitTemplate.convertAndSend(EXCHANGE_SCORING_OPENSOURCE, ROUTE_SCORING_OPENSOURCE, "");
        return Boolean.TRUE;
    }

    @Override
    public List<Long> queryTaskIdByCreateFrom(List<String> taskCreateFrom) {
        List<TaskIdInfo> taskInfoEntityList = taskDao.findTaskIdList(Status.ENABLE.value(), taskCreateFrom);
        return taskInfoEntityList.stream().map(TaskIdInfo::getTaskId).collect(Collectors.toList());
    }


    /**
     * 获取开源或非开源的有效任务ID
     *
     * @param defectStatType enum
     * @return list
     */
    @Override
    public List<Long> queryTaskIdByType(@NotNull ComConstants.DefectStatType defectStatType) {
        List<String> createFrom = getCreateFrom(defectStatType);
        return queryTaskIdByCreateFrom(createFrom);
    }

    @NotNull
    protected List<String> getCreateFrom(@NotNull ComConstants.DefectStatType defectStatType) {
        List<String> createFrom;
        if (ComConstants.DefectStatType.GONGFENG_SCAN.value().equals(defectStatType.value())) {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
        } else {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());

        }
        return createFrom;
    }

    /**
     * 初始化获取任务数量
     *
     * @param day 天数
     * @return
     */
    @Override
    public Boolean initTaskCountScript(Integer day) {
        // 获取日期
        List<String> dates = DateTimeUtils.getBeforeDaily(day);
        // 获取每天对应的任务数量
        List<TaskStatisticEntity> taskCountData = Lists.newArrayList();
        for (String date : dates) {
            try {
                // 获取开源任务数量
                getTaskCount(taskCountData, date, ComConstants.DefectStatType.GONGFENG_SCAN.value());
                // 获取非开源任务数量
                getTaskCount(taskCountData, date, ComConstants.DefectStatType.USER.value());
            } catch (Exception e) {
                log.error("Failed to obtain task data: {}", date, e);
            }
        }
        taskStatisticRepository.saveAll(taskCountData);
        return true;
    }

    /**
     * 获取任务数量
     *
     * @param taskCountData 容器
     * @param date 时间(string)
     * @param createFrom 来源
     */
    private void getTaskCount(@NotNull List<TaskStatisticEntity> taskCountData, String date, String createFrom) {
        // 获取结束时间
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(date, date);
        TaskStatisticEntity taskStatisticEntity = new TaskStatisticEntity();
        taskStatisticEntity.setDataFrom(createFrom);
        taskStatisticEntity.setDate(date);
        // 根据结束时间、来源获取任务数量
        Long count = taskDao.findDailyTaskCount(startTimeAndEndTime[1], createFrom);
        taskStatisticEntity.setTaskCount(count.intValue());

        List<Long> taskIdList;
        if (createFrom.equals(ComConstants.DefectStatType.GONGFENG_SCAN.value())) {
            taskIdList = queryTaskIdByCreateFrom(Collections.singletonList(createFrom));
        } else {
            taskIdList = queryTaskIdByCreateFrom(
                    Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value()));
        }
        // 获取分析次数 封装请求体
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        queryTaskListReqVO.setStartTime(startTimeAndEndTime[0]);
        queryTaskListReqVO.setEndTime(startTimeAndEndTime[1]);
        queryTaskListReqVO.setTaskIds(taskIdList);
        // 调用接口获取分析次数
        Integer taskAnalyzeCount =
                client.get(ServiceTaskLogOverviewResource.class).getTaskAnalyzeCount(queryTaskListReqVO).getData();
        taskStatisticEntity.setAnalyzeCount(taskAnalyzeCount != null ? taskAnalyzeCount : 0);
        taskCountData.add(taskStatisticEntity);
    }

    @Override
    public TaskDetailVO getTaskInfoWithoutToolsByStreamName(String nameEn) {

        TaskInfoEntity taskEntity = taskRepository.findFirstByNameEn(nameEn);
        if (taskEntity == null) {
            log.error("can not find task by streamName: {}", nameEn);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{nameEn}, null);
        }

        TaskDetailVO taskDetailVO = new TaskDetailVO();
        BeanUtils.copyProperties(taskEntity, taskDetailVO);
        return taskDetailVO;
    }

    /**
     * 根据任务类型设置任务类型和创建来源
     * 需要 BS_PIPELINE / BS_CODECC / GONGFENG_SCAN
     * 对于工蜂扫描任务需要区分 API 任务和 定时扫描任务，API触发任务给出相应的 apCode
     *
     * @param taskDetailVO
     */
    protected void setTaskCreateInfo(TaskDetailVO taskDetailVO) {
        if (taskDetailVO.getCreateFrom().equals(BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            return;
        }

        //如果是灰度项目，则要显示api触发来源
        if (StringUtils.isNotBlank(taskDetailVO.getProjectId())
                && taskDetailVO.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            taskDetailVO.setTaskType(BsTaskCreateFrom.API_TRIGGER.value());
            taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
            return;
        }

        // 设置为非工蜂项目
        taskDetailVO.setTaskType(taskDetailVO.getCreateFrom());
        taskDetailVO.setCreateSource(taskDetailVO.getCreatedBy());
    }

    /**
     * 根据任务类型设置任务类型和创建来源
     * 需要 BS_PIPELINE / BS_CODECC / GONGFENG_SCAN
     * 对于工蜂扫描任务需要区分 API 任务和 定时扫描任务，API触发任务给出相应的 apCode
     *
     * @param taskDetailVOList
     */
    protected void setTaskCreateInfo(List<TaskDetailVO> taskDetailVOList) {
        if (CollectionUtils.isEmpty(taskDetailVOList)) {
            return;
        }
        for (TaskDetailVO taskDetailVO : taskDetailVOList) {
            taskDetailVO.setTaskType(taskDetailVO.getCreateFrom());
        }
    }

    /**
     * 获取代码库 路径/别名 和 分支名称
     *
     * @param taskId
     */
    @Override
    public TaskCodeLibraryVO getRepoInfo(Long taskId) {
        TaskInfoEntity taskEntity = taskRepository.findFirstByTaskId(taskId);
        if (Objects.isNull(taskEntity)) {
            log.error("taskInfo not exists! task id is: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS,
                    new String[]{String.valueOf(taskId)}, null);
        }

        TaskCodeLibraryVO taskCodeLibrary = new TaskCodeLibraryVO();

        if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.BS_PIPELINE.value())) {
            log.info("task create from pipeline: {}", taskId);
            setPipelineRepoInfo(taskCodeLibrary, taskId);
        } else if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.BS_CODECC.value())) {
            log.info("task create from codecc: {}", taskId);
            /*
             * 当create_from是codecc时，需要区分两种情况
             * 1. 如果是灰度池中的项目，则需要取代码库信息，获取方式和开源项目一样
             * 2. 如果是一般项目，则按照原来逻辑进行
             */
            if (!taskEntity.getProjectId().startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
                taskCodeLibrary.setCodeInfo(Collections.singletonList(
                        new CodeLibraryInfoVO("",
                                taskEntity.getAliasName(),
                                taskEntity.getBranch())));
            } else {
                setGongfengRepoInfo(taskCodeLibrary, taskEntity);
            }

        } else if (taskEntity.getCreateFrom().equals(BsTaskCreateFrom.GONGFENG_SCAN.value())) {
            // 对于工蜂扫描任务还需要区分是api创建任务还是定时任务
            log.info("task create from gongfeng scan: {}", taskId);
            setGongfengRepoInfo(taskCodeLibrary, taskEntity);
        } else {
            log.error("invalid task: {}", taskId);
        }

        return taskCodeLibrary;
    }

    @Override
    public boolean addWhitePath(long taskId, List<String> pathList) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        boolean flag = isListEqualsExpectOrder(pathList, taskInfoEntity.getWhitePaths());

        if (!flag && taskDao.upsertPathOfTask(taskId, pathList)) {
            // 设置强制全量扫描标志
            setForceFullScan(taskInfoEntity);
            return true;
        }
        return false;
    }

    /**
     * 按任务id获取项目id map
     */
    @Override
    public Map<Long, String> getProjectIdMapByTaskId(@NotNull QueryTaskListReqVO taskListReqVO) {
        List<TaskInfoEntity> taskInfoEntityList =
                taskDao.queryTaskInfoEntityList(taskListReqVO.getStatus(), taskListReqVO.getBgId(),
                        taskListReqVO.getDeptIds(), taskListReqVO.getTaskIds(), taskListReqVO.getCreateFrom(), null);

        if (taskInfoEntityList == null) {
            taskInfoEntityList = Lists.newArrayList();
        }
        return taskInfoEntityList.stream()
                .collect(Collectors.toMap(TaskInfoEntity::getTaskId, TaskInfoEntity::getProjectId));
    }

    private boolean isListEqualsExpectOrder(List<?> l1, List<?> l2) {
        if (l1 == null || l2 == null) {
            return l1 == l2;
        }
        return (l1.containsAll(l2) && l2.containsAll(l1));
    }

    /**
     * 设置工蜂扫描任务的代码库信息
     * 工蜂扫描包含开源扫描任务和 API触发任务，需要做区分
     *
     * @param taskCodeLibraryVO
     * @param taskInfoEntity
     */
    protected void setGongfengRepoInfo(TaskCodeLibraryVO taskCodeLibraryVO,
            TaskInfoEntity taskInfoEntity) {
    }

    /**
     * 设置流水线创建任务的代码库信息
     * 流水线代码库信息不存在于 Task 表，需要从构建记录中解析
     *
     * @param taskId
     * @param taskCodeLibrary
     */
    private void setPipelineRepoInfo(TaskCodeLibraryVO taskCodeLibrary, long taskId) {
        Result<Map<String, TaskLogRepoInfoVO>> res = client.get(ServiceTaskLogRestResource.class)
                .getLastAnalyzeRepoInfo(taskId);
        if (res == null || res.isNotOk() || res.getData() == null) {
            log.error("fail to get last analyze repoInfo, taskId: {}", taskId);
            return;
        }

        List<CodeLibraryInfoVO> codeLibraryInfoVOList = new ArrayList<>();
        Map<String, TaskLogRepoInfoVO> repoInfo = res.getData();
        repoInfo.keySet()
                .stream()
                .filter(repoUrl -> StringUtils.isNotBlank(repoUrl)
                        && repoInfo.get(repoUrl) != null
                        && StringUtils.isNotBlank(repoInfo.get(repoUrl).getBranch()))
                .forEach(repoUrl -> {
                    codeLibraryInfoVOList.add(
                            new CodeLibraryInfoVO(
                                    repoUrl,
                                    pickupAliasNameFromUrl(repoUrl),
                                    repoInfo.get(repoUrl).getBranch()
                            )
                    );
                });
        taskCodeLibrary.setCodeInfo(codeLibraryInfoVOList);
    }

    /**
     * 从 Git / SVN 代码库URL中提取代码库别名
     * 不限于 HTTP 协议
     *
     * @param url
     */
    protected String pickupAliasNameFromUrl(String url) {
        return url.replaceFirst("(.*)://[^/]+/", "")
                .replaceFirst("\\.[a-zA-Z]*(/)?", "");
    }

    /**
     * 编辑任务信息
     *
     * @param reqVO 请求体
     * @return bool
     */
    @Override
    public Boolean editTaskDetail(TaskUpdateDeptInfoVO reqVO) {
        log.info("editTaskDetail req: {}", reqVO);
        if (reqVO != null) {
            // 调用dao编辑 bg 部门 中心 管理员
            taskDao.editTaskDetail(reqVO);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取工具已下架对应的任务id集合
     *
     * @param toolSet 工具集合
     * @return map
     */
    @Override
    public Map<String, Set<Long>> queryTaskIdByWithdrawTool(Set<String> toolSet) {
        HashMap<String, Set<Long>> toolTaskIdSet = Maps.newHashMap();
        if (CollectionUtils.isEmpty(toolSet)) {
            return toolTaskIdSet;
        }

        List<Integer> followStatusList = Lists.newArrayList(FOLLOW_STATUS.WITHDRAW.value());
        List<TaskIdToolInfoEntity> toolInfoEntityList = toolDao.findTaskIdByTool(toolSet, followStatusList);

        return toolInfoEntityList.stream().collect(Collectors
                .groupingBy(TaskIdToolInfoEntity::getToolName,
                        Collectors.mapping(TaskIdToolInfoEntity::getTaskId, Collectors.toSet())));
    }

    @Override
    public long countTaskSize() {
        List<BaseDataVO> baseDataVOList = baseDataService.findBaseDataInfoByType(CLEAN_TASK_STATUS);
        List<Integer> statusList = null;
        if (baseDataVOList != null && !baseDataVOList.isEmpty()) {
            String status = baseDataVOList.get(0).getParamValue();
            statusList = Arrays.stream(status.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        if (statusList == null || statusList.size() == 2) {
            return taskRepository.count();
        } else {
            return taskRepository.countByStatus(statusList.get(0));
        }
    }

    @Override
    public List<TaskDetailVO> getTaskIdByPage(int page, int pageSize) {
        List<BaseDataVO> baseDataVOList = baseDataService.findBaseDataInfoByType(CLEAN_TASK_STATUS);
        List<Integer> statusList = Arrays.asList(0, 1);
        if (baseDataVOList != null && !baseDataVOList.isEmpty()) {
            String status = baseDataVOList.get(0).getParamValue();
            statusList = Arrays.stream(status.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        List<TaskInfoEntity> taskIdEntityList = taskDao.findTaskIdByPage(page, pageSize, statusList);
        List<TaskDetailVO> taskDetailVOList = new ArrayList<>();
        taskIdEntityList.forEach(it -> {
            TaskDetailVO taskDetailVO = new TaskDetailVO();
            BeanUtils.copyProperties(it, taskDetailVO);
            taskDetailVOList.add(taskDetailVO);
        });
        setTaskCreateInfo(taskDetailVOList);
        List<BaseDataEntity> taskIdWhiteList = baseDataRepository.findByParamCodeInAndParamType(
                taskDetailVOList.stream()
                        .map(it -> String.valueOf(it.getTaskId()))
                        .collect(Collectors.toList()), CLEAN_TASK_WHITE_LIST
        );

        if (taskIdWhiteList != null && !taskIdWhiteList.isEmpty()) {
            ImmutableMap<Object, BaseDataEntity> taskIdWhiteMap = Maps.uniqueIndex(
                    taskIdWhiteList, BaseDataEntity::getParamCode
            );

            taskDetailVOList.forEach(it -> {
                BaseDataEntity baseDataEntity = taskIdWhiteMap.get(String.valueOf(it.getTaskId()));
                if (baseDataEntity != null) {
                    it.setTaskType(CLEAN_TASK_WHITE_LIST);
                    it.setCleanIndex(Integer.parseInt(baseDataEntity.getParamValue()));
                }
            });
        }

        return taskDetailVOList;
    }

    /**
     * 定时任务统计任务数、代码行、工具数
     *
     * @param reqVO 请求体
     */
    @Override
    public void statisticTaskCodeLineTool(StatisticTaskCodeLineToolVO reqVO) {

    }

    /**
     * 按项目id获取任务列表
     */
    @Override
    public List<TaskBaseVO> queryTaskListByProjectId(String projectId) {
        List<TaskInfoEntity> taskEntityList =
                taskRepository.findByProjectIdAndStatus(projectId, ComConstants.Status.ENABLE.value());

        List<String> toolIDArr = getToolOrders();

        List<TaskBaseVO> taskBaseVOList = Lists.newArrayList();
        for (TaskInfoEntity entity : taskEntityList) {
            List<ToolConfigInfoEntity> toolConfigInfoList = entity.getToolConfigInfoList();
            if (CollectionUtils.isEmpty(toolConfigInfoList)) {
                continue;
            }

            TaskBaseVO taskBaseVO = new TaskBaseVO();
            BeanUtils.copyProperties(entity, taskBaseVO);

            List<ToolConfigBaseVO> enableToolList = Lists.newArrayList();
            for (ToolConfigInfoEntity toolEntity : toolConfigInfoList) {
                String toolName = toolEntity.getToolName();
                if (!toolIDArr.contains(toolName)
                        || TaskConstants.FOLLOW_STATUS.WITHDRAW.value() == toolEntity.getFollowStatus()) {
                    continue;
                }

                ToolConfigBaseVO toolConfigBaseVO = new ToolConfigBaseVO();
                BeanUtils.copyProperties(toolEntity, toolConfigBaseVO);
                ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
                toolConfigBaseVO.setToolDisplayName(toolMetaBaseVO.getDisplayName());
                toolConfigBaseVO.setToolPattern(toolMetaBaseVO.getPattern());
                toolConfigBaseVO.setToolType(toolMetaBaseVO.getType());
                enableToolList.add(toolConfigBaseVO);
            }

            taskBaseVO.setEnableToolList(enableToolList);
            taskBaseVOList.add(taskBaseVO);
        }

        for (TaskBaseVO taskBaseVO : taskBaseVOList) {
            TaskCodeLibraryVO repoInfo = getRepoInfo(taskBaseVO.getTaskId());
            taskBaseVO.setCodeLibraryInfo(repoInfo);
        }

        return taskBaseVOList;
    }

    /**
     * 分页获取有效任务的项目id
     *
     * @param createFrom 来源
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return list
     */
    @Override
    public List<String> queryProjectIdPage(Set<String> createFrom, Integer pageNum, Integer pageSize) {
        log.info("project id query param: createFrom: {}, pageNum:{}, pageSize:{}", createFrom, pageNum, pageSize);

        Pageable pageable = PageableUtils.getPageable(pageNum, pageSize);
        return taskDao.findProjectIdPage(createFrom, pageable);
    }

    /**
     * 按项目id分页获取有效任务id
     *
     * @param projectId 项目id
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return list
     */
    @Override
    public List<Long> queryTaskIdPageByProjectId(String projectId, Integer pageNum, Integer pageSize) {
        log.info("task id query param: projectId: {}, pageNum:{}, pageSize:{}", projectId, pageNum, pageSize);

        Pageable pageable = PageableUtils.getPageable(pageNum, pageSize);
        return taskDao.findTaskIdPageByProjectId(projectId, pageable);
    }

    @Override
    public TaskInfoWithSortedToolConfigResponse getTaskInfoWithSortedToolConfig(
            TaskInfoWithSortedToolConfigRequest request
    ) {
        if (CollectionUtils.isEmpty(request.getTaskIdList())) {
            return new TaskInfoWithSortedToolConfigResponse(Lists.newArrayList());
        }

        List<TaskInfoEntity> taskInfoEntityList = taskRepository.findNoneDBRefByTaskIdIn(request.getTaskIdList());
        if (CollectionUtils.isEmpty(taskInfoEntityList)) {
            return new TaskInfoWithSortedToolConfigResponse(Lists.newArrayList());
        }

        List<Long> taskIdList = taskInfoEntityList.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
        List<ToolConfigInfoEntity> toolConfigList = toolConfigRepository.findByTaskIdIn(taskIdList);
        if (CollectionUtils.isNotEmpty(toolConfigList)) {
            Map<Long, List<ToolConfigInfoEntity>> toolConfigMap =
                    toolConfigList.stream().collect(Collectors.groupingBy(ToolConfigInfoEntity::getTaskId));

            for (TaskInfoEntity taskInfoEntity : taskInfoEntityList) {
                long curTaskId = taskInfoEntity.getTaskId();
                taskInfoEntity.setToolConfigInfoList(toolConfigMap.get(curTaskId));
            }
        }

        if (Boolean.TRUE.equals(request.getNeedSorted())) {
            String toolIdsOrder = commonDao.getToolOrder();
            List<String> toolOrderList = Arrays.asList(toolIdsOrder.split(ComConstants.STRING_SPLIT));
            Comparator<ToolConfigInfoEntity> comparator = Comparator.comparing(toolConfig ->
                    toolOrderList.contains(toolConfig.getToolName())
                            ? toolOrderList.indexOf(toolConfig.getToolName())
                            : Integer.MAX_VALUE
            );

            for (TaskInfoEntity taskInfoEntity : taskInfoEntityList) {
                if (CollectionUtils.isEmpty(taskInfoEntity.getToolConfigInfoList())) {
                    continue;
                }

                taskInfoEntity.setToolConfigInfoList(
                        taskInfoEntity.getToolConfigInfoList().stream()
                                .filter(Objects::nonNull)
                                .sorted(comparator)
                                .collect(Collectors.toList())
                );
            }
        }

        List<TaskBase> retVOList = taskInfoEntityList.stream().map(taskInfoEntity -> {
            TaskBase taskVO = new TaskBase();
            BeanUtils.copyProperties(taskInfoEntity, taskVO);
            taskVO.setToolConfigInfoList(Lists.newArrayList());

            if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
                taskInfoEntity.getToolConfigInfoList().forEach(source -> {
                    ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                    BeanUtils.copyProperties(source, toolConfigInfoVO);
                    taskVO.getToolConfigInfoList().add(toolConfigInfoVO);
                });
            }

            return taskVO;
        }).collect(Collectors.toList());

        return new TaskInfoWithSortedToolConfigResponse(retVOList);
    }

    @Override
    public List<TaskBaseVO> getTaskIdAndCreateFromWithPage(long lastTaskId, Integer limit) {
        // 页大小最大5k
        if (limit == null || limit <= 0L || limit > 5000) {
            limit = 5000;
        }

        List<TaskInfoEntity> taskInfoList = taskDao.findTaskIdAndCreateFromByLastTaskIdWithPage(lastTaskId, limit);
        List<TaskBaseVO> retList = Lists.newArrayList();

        for (TaskInfoEntity taskInfo : taskInfoList) {
            TaskBaseVO taskBaseVO = new TaskBaseVO();
            org.springframework.beans.BeanUtils.copyProperties(taskInfo, taskBaseVO);
            retList.add(taskBaseVO);
        }

        return retList;
    }

    @Override
    public List<TaskBaseVO> listTaskBase(String userId, String projectId) {
        if (StringUtils.isEmpty(projectId) || StringUtils.isEmpty(userId)) {
            log.warn("list task by project, args can not be null: {}, {}", projectId, userId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"projectId", "userId"});
        }

        List<TaskInfoEntity> taskInfoEntityList = queryTaskByProjectIdWithPermissionCore(projectId, userId);
        Collator collator = Collator.getInstance(Locale.SIMPLIFIED_CHINESE);

        return taskInfoEntityList.stream()
                .map(x -> {
                    TaskBaseVO vo = new TaskBaseVO();
                    vo.setTaskId(x.getTaskId());
                    vo.setNameCn(x.getNameCn());
                    return vo;
                })
                .sorted((x1, y2) -> collator.compare(x1.getNameCn(), y2.getNameCn()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> queryTaskIdByProjectIdWithPermission(String projectId, String userId) {
        return queryTaskByProjectIdWithPermissionCore(projectId, userId).stream()
                .map(TaskInfoEntity::getTaskId)
                .collect(Collectors.toList());
    }

    /**
     * 根据项目获取当前用户有权限的任务列表
     *
     * @param projectId
     * @param userId
     * @return
     */
    protected List<TaskInfoEntity> queryTaskByProjectIdWithPermissionCore(String projectId, String userId) {
        List<String> createFromList = Lists.newArrayList(
                BsTaskCreateFrom.BS_PIPELINE.value(),
                BsTaskCreateFrom.BS_CODECC.value()
        );

        if (projectId.startsWith(ComConstants.GONGFENG_PROJECT_ID_PREFIX)) {
            createFromList.add(BsTaskCreateFrom.GONGFENG_SCAN.value());
        }

        List<TaskInfoEntity> taskList = taskRepository.findByProjectIdAndCreateFromInAndStatus(
                projectId,
                createFromList,
                Status.ENABLE.value()
        );

        return getQualifiedTaskListCore(projectId, userId, taskList);
    }

    @Override
    public Map<Long, String> listTaskNameCn(List<Long> taskIdList) {
        if (CollectionUtils.isEmpty(taskIdList)) {
            return Maps.newHashMap();
        }

        return taskRepository.findNoneDBRefByTaskIdIn(taskIdList).stream()
                .collect(Collectors.toMap(TaskInfoEntity::getTaskId, TaskInfoEntity::getNameCn, (x, y) -> x));
    }

    @Override
    public boolean multiTaskVisitable(String projectId) {
        if (projectId.startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            return false;
        }

        if (projectId.startsWith(ComConstants.CUSTOMPROJ_ID_PREFIX)) {
            List<String> createFromList = Lists.newArrayList(
                    BsTaskCreateFrom.BS_CODECC.value(),
                    BsTaskCreateFrom.BS_PIPELINE.value()
            );

            List<TaskInfoEntity> notGongfengList = taskRepository.findPageableByProjectIdAndCreateFromAndStatus(
                    projectId, createFromList, Status.ENABLE.value(),
                    PageRequest.of(0, 1)
            );

            return CollectionUtils.isNotEmpty(notGongfengList);
        }

        return true;
    }

    @Override
    public List<MetadataVO> listTaskToolDimension(List<Long> taskIdList, String projectId) {
        List<TaskInfoEntity> taskInfoEntityList;

        if (CollectionUtils.isEmpty(taskIdList)) {
            taskInfoEntityList = taskRepository.findByProjectIdAndStatus(projectId, Status.ENABLE.value());
            taskIdList = taskInfoEntityList.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
        } else {
            taskInfoEntityList = taskRepository.findByTaskIdIn(taskIdList);
        }

        List<ToolConfigInfoEntity> emptyToolConfigList = Lists.newArrayList();
        List<String> toolNameList = taskInfoEntityList.stream()
                .flatMap(
                        x -> x.getToolConfigInfoList() != null
                                ? x.getToolConfigInfoList().stream()
                                : emptyToolConfigList.stream()
                )
                .filter(y -> y != null && y.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value())
                .map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(toolNameList)) {
            return Lists.newArrayList();
        }

        Map<String, MetadataVO> resultMap = Maps.newHashMap();
        List<String> dimensionList = client.get(ServiceCheckerRestResource.class)
                .queryDimensionByToolChecker(
                        new QueryTaskCheckerDimensionRequest(taskIdList, toolNameList, projectId)
                ).getData();

        if (CollectionUtils.isNotEmpty(dimensionList)) {
            for (String dimension : dimensionList) {
                try {
                    BaseDataEntity baseDataEntity = toolDimensionBaseDataCache.get(dimension);
                    if (baseDataEntity != null) {
                        MetadataVO metadataVO = new MetadataVO();
                        metadataVO.setEntityId(baseDataEntity.getEntityId());
                        metadataVO.setKey(baseDataEntity.getParamCode());
                        metadataVO.setName(baseDataEntity.getParamName());
                        metadataVO.setFullName(baseDataEntity.getParamExtend1());
                        resultMap.put(baseDataEntity.getParamCode(), metadataVO);
                    }
                } catch (ExecutionException ignore) { // NOCC:EmptyCatchBlock(设计如此:)

                }
            }
        }

        return Lists.newArrayList(resultMap.values());
    }

}
