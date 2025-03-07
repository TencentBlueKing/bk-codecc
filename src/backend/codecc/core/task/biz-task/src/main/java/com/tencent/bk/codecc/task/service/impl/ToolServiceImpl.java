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
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy,modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import static com.tencent.devops.common.constant.ComConstants.CommonJudge;
import static com.tencent.devops.common.constant.ComConstants.DefectStatType;
import static com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import static com.tencent.devops.common.constant.ComConstants.FUNC_TOOL_SWITCH;
import static com.tencent.devops.common.constant.ComConstants.PipelineToolUpdateType;
import static com.tencent.devops.common.constant.ComConstants.Status;
import static com.tencent.devops.common.constant.ComConstants.Tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolStatisticRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.CommonTaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.DeletedTaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolCheckerSetEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.model.ToolCountScriptEntity;
import com.tencent.bk.codecc.task.model.ToolStatisticEntity;
import com.tencent.bk.codecc.task.service.IRegisterToolBizService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskProjectCountVO;
import com.tencent.bk.codecc.task.vo.TaskUpdateVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoWithMetadataVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.bk.codecc.task.vo.ToolTaskInfoVO;
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * ci流水线工具管理服务层代码
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Slf4j
@Service
public class ToolServiceImpl implements ToolService {

    private static Logger logger = LoggerFactory.getLogger(ToolServiceImpl.class);
    @Autowired
    protected ToolRepository toolRepository;
    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected Client client;
    @Value("${time.analysis.maxhour:#{null}}")
    private String maxHour;
    @Autowired
    private ToolDao toolDao;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private BizServiceFactory<IRegisterToolBizService> bizServiceFactory;
    @Autowired
    private ToolMetaCacheService toolMetaCache;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private AuthExRegisterApi authExRegisterApi;
    @Autowired
    private PlatformService platformService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ToolStatisticRepository toolStatisticRepository;

    @Autowired
    private DeletedTaskDao deletedTaskDao;

    @Override
    public Result<Boolean> registerTools(BatchRegisterVO batchRegisterVO, TaskInfoEntity taskInfoEntity,
            String userName) {
        Result<Boolean> registerResult;
        long taskId = batchRegisterVO.getTaskId();
        if (CollectionUtils.isEmpty(batchRegisterVO.getTools())) {
            logger.error("no tools will be registered!");
            return new Result<>(false);
        }
        if (null == taskInfoEntity) {
            taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
            if (null == taskInfoEntity) {
                log.error("task does not exist! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)},
                        null);
            }
        }

        //配置流水线编排
        pipelineConfig(batchRegisterVO, taskInfoEntity, userName);

        //批量接入工具
        List<String> failTools = new ArrayList<>();
        List<String> successTools = new ArrayList<>();
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
            toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        }
        for (ToolConfigInfoVO toolConfigInfoVO : batchRegisterVO.getTools()) {
            // 工具没有停用才允许注册
            if (!taskService.checkToolRemoved(toolConfigInfoVO.getToolName(), taskInfoEntity)) {
                try {
                    ToolConfigInfoEntity toolConfigInfoEntity = registerTool(toolConfigInfoVO, taskInfoEntity,
                            userName);
                    toolConfigInfoEntityList.add(toolConfigInfoEntity);
                    successTools.add(toolConfigInfoVO.getToolName());

                } catch (Exception e) {
                    log.error("register tool fail! tool name: {}", toolConfigInfoVO.getToolName(), e);
                    failTools.add(toolConfigInfoVO.getToolName());
                }
            }
        }

        // 新注册的工具都需要设置强制全量扫描标志
        if (CollectionUtils.isNotEmpty(successTools)) {
            log.info("set force full scan, taskId:{}, toolNames:{}", taskId, successTools);
            client.get(ServiceToolBuildInfoResource.class).setForceFullScan(taskId, successTools);
        }

        // 保存工具信息,只有当不为空的时候才保存
        if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
            //遍历查找工具清单中是否有cloc工具，如果有的话，需要下线cloc工具
            toolConfigInfoEntityList.stream().filter(toolConfigInfoEntity -> Tool.CLOC.name()
                    .equalsIgnoreCase(toolConfigInfoEntity.getToolName())).forEach(toolConfigInfoEntity -> {
                toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                toolConfigInfoEntity.setUpdatedBy(userName);
                toolConfigInfoEntity.setUpdatedDate(System.currentTimeMillis());
            });
            toolConfigInfoEntityList = toolRepository.saveAll(toolConfigInfoEntityList);
            taskInfoEntity.setToolConfigInfoList(toolConfigInfoEntityList);
        }
        taskRepository.save(taskInfoEntity);

        //全部工具添加失败
        if (failTools.size() == batchRegisterVO.getTools().size()) {
            registerResult = new Result<>(0, TaskMessageCode.ADD_TOOL_FAIL, "所有工具添加失败", false);
        } else if (successTools.size() == batchRegisterVO.getTools().size()) { //全部工具添加成功
            registerResult = new Result<>(true);
        } else {
            StringBuffer buffer = new StringBuffer();
            formatToolNames(successTools, buffer);
            buffer.append("添加成功；\n");
            formatToolNames(failTools, buffer);
            buffer.append("添加失败");
            registerResult = new Result<>(0, TaskMessageCode.ADD_TOOL_PARTIALLY_SUCCESS, buffer.toString(), false);
        }
        return registerResult;
    }


    @Override
    public ToolConfigInfoEntity registerTool(ToolConfigInfoVO toolConfigInfo, TaskInfoEntity taskInfoEntity,
            String user) {
        String toolName = toolConfigInfo.getToolName();
        String toolNames = taskInfoEntity.getToolNames();
        if (StringUtils.isNotEmpty(toolNames)) {
            List<String> toolNameList = List2StrUtil.fromString(toolNames, ComConstants.TOOL_NAMES_SEPARATOR);
            if (toolNameList.contains(toolName)) {
                if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())
                        && taskInfoEntity.getToolConfigInfoList().stream().anyMatch(
                        toolConfigInfoEntity ->
                                toolConfigInfoEntity.getToolName().equalsIgnoreCase(toolConfigInfo.getToolName()))) {
                    log.error("task [{}] has registered tool before! tool name: {}", taskInfoEntity.getTaskId(),
                            toolName);
                    throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{toolName}, null);
                }
            }
        }
        IRegisterToolBizService registerToolBizService = bizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.REGISTER_TOOL.value(), IRegisterToolBizService.class);
        ToolConfigInfoEntity toolConfigInfoEntity = registerToolBizService.registerTool(toolConfigInfo, taskInfoEntity,
                user);

        //添加项目信息
        addNewTool2Proj(toolConfigInfoEntity, taskInfoEntity, user);

        log.info("register tool[{}] for task[{}] successful", toolName, toolConfigInfoEntity.getTaskId());
        return toolConfigInfoEntity;
    }

    @Override
    @OperationHistory(funcId = FUNC_TOOL_SWITCH)
    public Boolean toolStatusManage(List<String> toolNameList, String manageType, String userName, long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        for (String toolName : toolNameList) {
            ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            if (null == toolConfigInfoEntity) {
                log.error("empty tool config info found out! task id: {}, tool name: {}", taskId, toolName);
                throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
            }
            if (CommonJudge.COMMON_Y.value().equals(manageType)) {
                toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                log.info("enable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            } else {
                toolConfigInfoEntity.setLastFollowStatus(toolConfigInfoEntity.getFollowStatus());
                toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                log.info("disable tool, task id: {}, tool name: {}", toolConfigInfoEntity.getTaskId(),
                        toolConfigInfoEntity.getToolName());
            }
            toolRepository.save(toolConfigInfoEntity);
        }

        if (CommonJudge.COMMON_Y.value().equalsIgnoreCase(manageType)) {
            // registerVo及relPath为空代表CodeElement为空，与之前的逻辑符合
            pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                    PipelineToolUpdateType.ADD, null, null);
        } else if (CommonJudge.COMMON_N.value().equalsIgnoreCase(manageType)) {
            pipelineService.updatePipelineTools(userName, taskId, toolNameList, taskInfoEntity,
                    PipelineToolUpdateType.REMOVE, null, null);
        }
        return true;
    }

    @Override
    public ToolConfigInfoVO getToolByTaskIdAndName(long taskId, String toolName) {
        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (null == toolConfigInfoEntity) {
            log.error("no tool info found!, task id: {}, tool name: {}", taskId, toolName);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{toolName}, null);
        }
        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "ignoreCheckers", "checkerProps");

        // 加入规则集
        if (toolConfigInfoEntity.getCheckerSet() != null) {
            ToolCheckerSetVO toolCheckerSetVO = new ToolCheckerSetVO();
            BeanUtils.copyProperties(toolConfigInfoEntity.getCheckerSet(), toolCheckerSetVO);
            toolConfigInfoVO.setCheckerSet(toolCheckerSetVO);
        }
        return toolConfigInfoVO;
    }

    @Override
    public ToolConfigInfoWithMetadataVO getToolWithMetadataByTaskIdAndName(long taskId, String toolName) {
        // 获取工具配置
        ToolConfigInfoVO toolConfigInfoVO = getToolByTaskIdAndName(taskId, toolName);
        ToolConfigInfoWithMetadataVO toolConfigInfoWithMetadataVO = new ToolConfigInfoWithMetadataVO();
        BeanUtils.copyProperties(toolConfigInfoVO, toolConfigInfoWithMetadataVO);
        if (toolConfigInfoVO.getCheckerSet() != null) {
            toolConfigInfoWithMetadataVO.setCheckerSet(toolConfigInfoVO.getCheckerSet());
        }

        // 获取工具元数据
        ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
        toolConfigInfoWithMetadataVO.setToolMetaBaseVO(toolMetaBaseVO);

        // 获取项目语言
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (null == taskInfoEntity) {
            log.error("task does not exist! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{String.valueOf(taskId)}, null);
        }
        toolConfigInfoWithMetadataVO.setCodeLang(taskInfoEntity.getCodeLang());
        return toolConfigInfoWithMetadataVO;
    }


    /**
     * 停用流水线
     *
     * @param taskId
     * @param projectId
     * @return
     */
    @Override
    public Boolean deletePipeline(Long taskId, String projectId, String userName) {
        boolean deleteTask = authExRegisterApi.deleteCodeCCTask(String.valueOf(taskId), projectId);
        if (!deleteTask) {
            return false;
        }

        TaskUpdateVO taskUpdateVO = new TaskUpdateVO();
        taskUpdateVO.setStatus(TaskConstants.TaskStatus.DISABLE.value());
        taskUpdateVO.setDisableTime(String.valueOf(System.currentTimeMillis()));
        return taskDao.updateTask(taskUpdateVO.getTaskId(), taskUpdateVO.getCodeLang(), taskUpdateVO.getNameCn(),
                taskUpdateVO.getTaskOwner(), taskUpdateVO.getTaskMember(), taskUpdateVO.getDisableTime(),
                taskUpdateVO.getStatus(), userName);
    }

    @Override
    public Boolean updatePipelineTool(Long taskId, List<String> toolList, String userName) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (BsTaskCreateFrom.BS_CODECC.name().equals(taskInfoEntity.getCreateFrom())) {
            log.error("=========the task is created from codecc!=============");
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        pipelineService.updatePipelineTools(userName, taskId, toolList, taskInfoEntity, PipelineToolUpdateType.ADD,
                null, null);
        return true;
    }

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolNames
     * @return
     */
    @Override
    public Boolean clearCheckerSet(Long taskId, List<String> toolNames) {
        if (CollectionUtils.isNotEmpty(toolNames)) {
            toolDao.clearCheckerSet(taskId, toolNames);
        }
        return true;
    }

    /**
     * 清除任务和工具关联的规则集
     *
     * @param taskId
     * @param toolCheckerSets
     * @return
     */
    @Override
    public Boolean addCheckerSet2Task(Long taskId, List<ToolCheckerSetVO> toolCheckerSets) {
        if (CollectionUtils.isNotEmpty(toolCheckerSets)) {
            List<ToolCheckerSetEntity> toolCheckerSetEntities = Lists.newArrayList();
            for (ToolCheckerSetVO toolCheckerSetVO : toolCheckerSets) {
                ToolCheckerSetEntity toolCheckerSetEntity = new ToolCheckerSetEntity();
                BeanUtils.copyProperties(toolCheckerSetVO, toolCheckerSetEntity);
                toolCheckerSetEntities.add(toolCheckerSetEntity);
            }
            toolDao.setCheckerSet(taskId, toolCheckerSetEntities);
        }
        return true;
    }

    @Override
    public ToolConfigPlatformVO getToolConfigPlatformInfo(Long taskId, String toolName) {
        if (taskId == null || taskId == 0 || StringUtils.isBlank(toolName)) {
            logger.error("taskId or toolName is not allowed to be empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigInfoEntity toolConfigInfoEntity = toolRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        if (toolConfigInfoEntity == null) {
            logger.error("task [{}] or toolName is invalid!", taskId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId or toolName"}, null);
        }

        ToolConfigPlatformVO toolConfigPlatformVO = new ToolConfigPlatformVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigPlatformVO);

        String port = "";
        String userName = "";
        String passwd = "";
        String platformIp = toolConfigInfoEntity.getPlatformIp();
        if (StringUtils.isNotBlank(platformIp)) {
            PlatformVO platformVO = platformService.getPlatformByToolNameAndIp(toolName, platformIp);
            if (null != platformVO) {
                port = platformVO.getPort();
                userName = platformVO.getUserName();
                passwd = platformVO.getPasswd();
            }
        }
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);

        toolConfigPlatformVO.setIp(platformIp);
        toolConfigPlatformVO.setPort(port);
        toolConfigPlatformVO.setUserName(userName);
        toolConfigPlatformVO.setPassword(passwd);
        toolConfigPlatformVO.setNameEn(taskInfoEntity.getNameEn());
        toolConfigPlatformVO.setNameCn(taskInfoEntity.getNameCn());

        return toolConfigPlatformVO;
    }

    @Override
    public Boolean updateToolPlatformInfo(Long taskId, String userName, ToolConfigPlatformVO toolConfigPlatformVO) {
        // 1.检查参数
        if (toolConfigPlatformVO == null) {
            logger.error("toolConfigPlatformVO is null!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"reqObj"}, null);
        }
        // 2.检查参数
        Long taskIdReq = toolConfigPlatformVO.getTaskId();
        String toolName = toolConfigPlatformVO.getToolName();
        if (taskIdReq == null || !taskIdReq.equals(taskId) || StringUtils.isBlank(toolName) || StringUtils.isBlank(
                userName)) {
            logger.error("parameter is invalid! task:{} userName:{} reqObj:{}", taskId, userName, toolConfigPlatformVO);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"parameter"}, null);
        }
        // 检查任务ID是否有效
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskIdReq);
        if (taskInfoEntity == null) {
            logger.error("taskId [{}] is invalid!", taskIdReq);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"taskId"}, null);
        }
        // 检查platform IP是否存在
        String platformIp = toolConfigPlatformVO.getIp();
        if (StringUtils.isNotBlank(platformIp)) {
            PlatformVO platformVO = platformService.getPlatformByToolNameAndIp(toolName, platformIp);
            if (platformVO == null) {
                logger.error("platform ip [{}] is not found!", platformIp);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"platform ip"}, null);
            }
        }

        return toolDao.updateToolConfigInfo(taskIdReq, toolName, userName, toolConfigPlatformVO.getSpecConfig(),
                platformIp);
    }

    @Override
    public Result<Boolean> updateTools(Long taskId, String user, BatchRegisterVO batchRegisterVO) {
        // 查询任务已接入的工具列表
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);

        log.info("start to update tool for task: {}", taskId);

        Map<String, ToolConfigInfoEntity> toolConfigInfoEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList()) {
                toolConfigInfoEntityMap.put(toolConfigInfoEntity.getToolName(), toolConfigInfoEntity);
            }
        }

        // 获取需要启用的工具列表
        long curTime = System.currentTimeMillis();
        List<ToolConfigInfoEntity> updateStatusTools = Lists.newArrayList();
        Iterator<ToolConfigInfoVO> it = batchRegisterVO.getTools().iterator();
        Set<String> reqTools = Sets.newHashSet();
        while (it.hasNext()) {
            ToolConfigInfoVO toolConfigInfoVO = it.next();
            reqTools.add(toolConfigInfoVO.getToolName());
            if (toolConfigInfoEntityMap.get(toolConfigInfoVO.getToolName()) != null) {
                ToolConfigInfoEntity toolConfigInfoEntity = toolConfigInfoEntityMap.get(toolConfigInfoVO.getToolName());
                int toolFollowStatus = toolConfigInfoEntity.getFollowStatus();
                if (FOLLOW_STATUS.WITHDRAW.value() == toolFollowStatus) {
                    toolConfigInfoEntity.setFollowStatus(toolConfigInfoEntity.getLastFollowStatus());
                    toolConfigInfoEntity.setUpdatedBy(user);
                    toolConfigInfoEntity.setUpdatedDate(curTime);
                    updateStatusTools.add(toolConfigInfoEntity);
                    log.info("enable task {} tool {}", taskId, toolConfigInfoEntity.getToolName());
                }
                it.remove();
            }
        }

        // 获取需要停用的工具列表
        if (CollectionUtils.isNotEmpty(taskInfoEntity.getToolConfigInfoList())) {
            for (ToolConfigInfoEntity toolConfigInfoEntity : taskInfoEntity.getToolConfigInfoList()) {
                //cloc工具统一需要停用
                if (!reqTools.contains(toolConfigInfoEntity.getToolName()) || Tool.CLOC.name()
                        .equalsIgnoreCase(toolConfigInfoEntity.getToolName())) {
                    int toolFollowStatus = toolConfigInfoEntity.getFollowStatus();
                    if (FOLLOW_STATUS.WITHDRAW.value() != toolFollowStatus) {
                        toolConfigInfoEntity.setFollowStatus(FOLLOW_STATUS.WITHDRAW.value());
                        toolConfigInfoEntity.setUpdatedBy(user);
                        toolConfigInfoEntity.setUpdatedDate(curTime);
                        updateStatusTools.add(toolConfigInfoEntity);
                        log.info("disable task {} tool {}", taskId, toolConfigInfoEntity.getToolName());
                    }
                }
            }
        }

        // 更新工具状态
        if (CollectionUtils.isNotEmpty(updateStatusTools)) {
            toolRepository.saveAll(taskInfoEntity.getToolConfigInfoList());
        }

        // 新增的工具需要接入
        if (CollectionUtils.isNotEmpty(batchRegisterVO.getTools())) {
            return registerTools(batchRegisterVO, taskInfoEntity, user);
        } else {
            return new Result<>(true);
        }
    }

    private void formatToolNames(List<String> tools, StringBuffer buffer) {
        buffer.append("工具[");
        for (int i = 0; i < tools.size(); i++) {
            String toolName = tools.get(i);
            String displayName = toolMetaCache.getToolDisplayName(toolName);
            buffer.append(null != displayName ? displayName : "");
            if (i != tools.size() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append("]");
    }

    /**
     * 配置流水线编排
     *
     * @param batchRegisterVO
     * @param taskInfoEntity
     * @param userName
     * @throws JsonProcessingException
     */
    private void pipelineConfig(BatchRegisterVO batchRegisterVO, TaskInfoEntity taskInfoEntity, String userName) {
        long taskId = taskInfoEntity.getTaskId();
        String pipelineId = taskInfoEntity.getPipelineId();
        String relPath = getRelPath(batchRegisterVO);
        if (StringUtils.isEmpty(pipelineId)) {
            List<String> defaultExecuteDate = getTaskDefaultReportDate();
            String defaultExecuteTime = getTaskDefaultTime();

            // 创建流水线编排并获取流水线ID
            pipelineId = pipelineService.assembleCreatePipeline(batchRegisterVO, taskInfoEntity, defaultExecuteTime,
                    defaultExecuteDate, userName, relPath, "CREATE");
            log.info("create pipeline success! project id: {}, codecc task id: {}, pipeline id: {}",
                    taskInfoEntity.getProjectId(), taskInfoEntity.getTaskId(), pipelineId);

            taskInfoEntity.setPipelineId(pipelineId);
            //表示通过codecc_web平台创建蓝盾codecc任务
            taskInfoEntity.setCreateFrom(BsTaskCreateFrom.BS_CODECC.value());
            //保存定时执行信息
//            taskInfoEntity.setExecuteTime(defaultExecuteTime);
//            taskInfoEntity.setExecuteDate(defaultExecuteDate);
        } else {
            //更新流水线编排中的工具
            List<String> toolNames = new ArrayList<>();
            for (ToolConfigInfoVO tool : batchRegisterVO.getTools()) {
                toolNames.add(tool.getToolName());
            }
            if (!toolNames.contains(Tool.GOML.name())) {
                relPath = null;
            }
            pipelineService.updatePipelineTools(userName, taskId, toolNames, taskInfoEntity, PipelineToolUpdateType.ADD,
                    batchRegisterVO, relPath);
        }

    }

    /**
     * 获取相对路径
     *
     * @param registerVO
     * @return
     */
    private String getRelPath(BatchRegisterVO registerVO) {
        if (CollectionUtils.isNotEmpty(registerVO.getTools())) {
            for (ToolConfigInfoVO toolConfig : registerVO.getTools()) {
                //常量要区分吗
                if (Tool.GOML.name().equals(toolConfig.getToolName()) && StringUtils.isNotEmpty(
                        toolConfig.getParamJson())) {
                    JSONObject paramJson = new JSONObject(toolConfig.getParamJson());
                    if (paramJson.has(ComConstants.PARAMJSON_KEY_REL_PATH)) {
                        return paramJson.getString(ComConstants.PARAMJSON_KEY_REL_PATH);
                    }
                }
            }
        }
        return "";
    }

    /**
     * 更新工具分析步骤及状态
     *
     * @param toolConfigBaseVO
     */
    @Override
    public void updateToolStepStatus(ToolConfigBaseVO toolConfigBaseVO) {
        toolDao.updateToolStepStatusByTaskIdAndToolName(toolConfigBaseVO);
    }

    protected void addNewTool2Proj(ToolConfigInfoEntity toolConfigInfoEntity, TaskInfoEntity taskInfoEntity,
            String user) {
        String toolName = toolConfigInfoEntity.getToolName();
        String toolNames = taskInfoEntity.getToolNames();
        if (StringUtils.isNotEmpty(toolNames)) {
            toolNames = String.format("%s%s%s", toolNames, ComConstants.TOOL_NAMES_SEPARATOR, toolName);
            taskInfoEntity.setToolNames(toolNames);
        } else {
            toolNames = toolName;
            taskInfoEntity.setToolNames(toolNames);
        }
        taskInfoEntity.setUpdatedBy(user);
        taskInfoEntity.setUpdatedDate(System.currentTimeMillis());
    }


    protected String getTaskDefaultTime() {
        float time = new Random().nextInt(Integer.valueOf(maxHour) * 2) / 2f;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, (int) (time * 60));
        String defaultTime = timeFormat.format(cal.getTime());
        log.info("task default time {}", defaultTime);
        return defaultTime;
    }


    protected List<String> getTaskDefaultReportDate() {
        List<String> reportDate = new ArrayList<>();
        reportDate.add(String.valueOf(Calendar.MONDAY));
        reportDate.add(String.valueOf(Calendar.TUESDAY));
        reportDate.add(String.valueOf(Calendar.WEDNESDAY));
        reportDate.add(String.valueOf(Calendar.THURSDAY));
        reportDate.add(String.valueOf(Calendar.FRIDAY));
        reportDate.add(String.valueOf(Calendar.SATURDAY));
        reportDate.add(String.valueOf(Calendar.SUNDAY));
        return reportDate;
    }

    @Override
    public List<String> getEffectiveToolList(long taskId) {
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        //获取工具配置实体类清单
        List<String> toolNameList = getEffectiveToolList(taskInfoEntity);
        return toolNameList;
    }

    @Override
    public List<String> getEffectiveToolList(TaskInfoEntity taskInfoEntity) {
        //获取工具配置实体类清单
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList)) {
            return new ArrayList<>();
        }
        List<String> toolNameList = toolConfigInfoEntityList.stream()
                .filter(toolConfigInfoEntity -> FOLLOW_STATUS.WITHDRAW.value()
                        != toolConfigInfoEntity.getFollowStatus()).map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.toList());
        return toolNameList;
    }

    @Override
    public List<ToolConfigInfoVO> batchGetToolConfigList(QueryTaskListReqVO queryReqVO) {
        List<ToolConfigInfoVO> toolConfigInfoVoList = Lists.newArrayList();

        Collection<Long> taskIds = queryReqVO.getTaskIds();
        if (CollectionUtils.isNotEmpty(taskIds)) {
            List<ToolConfigInfoEntity> configInfoEntityList = toolRepository.findByTaskIdIn(taskIds);
            if (CollectionUtils.isNotEmpty(configInfoEntityList)) {
                toolConfigInfoVoList = configInfoEntityList.stream().map(entity -> {
                    ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                    BeanUtils.copyProperties(entity, toolConfigInfoVO);
                    return toolConfigInfoVO;
                }).collect(Collectors.toList());
            }
        }

        return toolConfigInfoVoList;
    }


    /**
     * 根据taskId集合刷新工具的跟进状态
     *
     * @param userName 用户名
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean refreshToolFollowStatusByTaskIds(String userName, QueryTaskListReqVO reqVO) {
        log.info("refreshToolFollowStatusByTaskIdList executor: [{}], reqVO: [{}]", userName, reqVO);
        // 由于手动掉用的数据量并不大,这里默认pageSize为20
        Pageable pageable = PageableUtils.getPageable(1, 20, "task_id", Sort.Direction.DESC, "");

        Set<Long> taskIdSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(reqVO.getTaskIds())) {
            Collection<Long> taskIds = reqVO.getTaskIds();
            taskIdSet = new HashSet<>(taskIds);
        }
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            // 筛选出有效的任务ID
            taskInfoEntities = taskRepository.findByStatusAndCreateFromInAndTaskIdIn(Status.ENABLE.value(),
                    Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value()),
                    taskIdSet);
        }
        if (CollectionUtils.isNotEmpty(taskInfoEntities)) {
            return refreshToolFollowStatus(pageable, taskInfoEntities);
        } else {
            log.info("The valid task ID is empty!");
            return false;
        }
    }

    @Override
    public Boolean batchUpdateToolFollowStatus(Integer pageSize) {
        Pageable pageable = PageableUtils.getPageable(1, pageSize, "task_id", Sort.Direction.DESC, "");

        // 1.查询有效的任务ID
        List<TaskInfoEntity> taskInfoEntities = taskRepository.findByStatusAndCreateFromIn(Status.ENABLE.value(),
                Lists.newArrayList(BsTaskCreateFrom.BS_CODECC.value(), BsTaskCreateFrom.BS_PIPELINE.value()));

        return refreshToolFollowStatus(pageable, taskInfoEntities);
    }

    /**
     * 公共方法 操作刷新工具跟进状态
     *
     * @param pageable 分页器
     * @param taskInfoEntities 有效的任务id集合
     * @return boolean
     */
    @NotNull
    private Boolean refreshToolFollowStatus(Pageable pageable, List<TaskInfoEntity> taskInfoEntities) {
        List<Long> taskList = taskInfoEntities.stream().map(TaskInfoEntity::getTaskId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskList)) {
            log.error("End batchUpdateToolFollowStatus, task id list is empty!");
            return false;
        }

        // 2.按分页查询未跟进的工具列表
        List<ToolConfigInfoEntity> toolConfigInfoEntities = toolDao.getTaskIdsAndFollowStatusPage(taskList,
                Lists.newArrayList(FOLLOW_STATUS.NOT_FOLLOW_UP_0.value(), FOLLOW_STATUS.NOT_FOLLOW_UP_1.value()),
                pageable);
        if (CollectionUtils.isEmpty(toolConfigInfoEntities)) {
            log.info("End batchUpdateToolFollowStatus, Not follow up tool is empty!");
            return true;
        }

        Map<String, List<ToolConfigInfoEntity>> toolConfigInfoEntityMap = new HashMap<>();
        for (ToolConfigInfoEntity toolConfigInfoEntity : toolConfigInfoEntities) {
            if (toolConfigInfoEntity.getTaskId() != 0 && StringUtils.isNotEmpty(toolConfigInfoEntity.getToolName())) {
                // 将TaskId和ToolName组合成Key
                List<ToolConfigInfoEntity> toolConfigInfos = toolConfigInfoEntityMap.get(
                        toolConfigInfoEntity.getTaskId() + toolConfigInfoEntity.getToolName());
                if (CollectionUtils.isEmpty(toolConfigInfos)) {
                    toolConfigInfos = Lists.newArrayList();
                }
                toolConfigInfos.add(toolConfigInfoEntity);
                toolConfigInfoEntityMap.put(toolConfigInfoEntity.getTaskId() + toolConfigInfoEntity.getToolName(),
                        toolConfigInfos);
            }
        }
        // 根据工具名分组
        Map<String, List<ToolConfigInfoEntity>> queryToolConfigInfoEntityMap = toolConfigInfoEntities.stream()
                .filter(entity -> StringUtils.isNotEmpty(entity.getToolName()))
                .collect(Collectors.groupingBy(ToolConfigInfoEntity::getToolName));

        List<QueryTaskListReqVO> queryTaskListReqVODataList = new ArrayList<>();
        for (Map.Entry<String, List<ToolConfigInfoEntity>> entry : queryToolConfigInfoEntityMap.entrySet()) {
            String toolName = entry.getKey();
            List<ToolConfigInfoEntity> reqVOList = entry.getValue();
            Collection<Long> taskIds = null;
            if (CollectionUtils.isNotEmpty(reqVOList)) {
                taskIds = reqVOList.stream().map(ToolConfigInfoEntity::getTaskId).collect(Collectors.toList());
            }

            QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
            queryTaskListReqVO.setToolName(toolName);
            queryTaskListReqVO.setTaskIds(taskIds);

            // 调用defect服务接口 查询分析成功的任务和工具信息
            List<QueryTaskListReqVO> taskListReqVOList = client.get(OpDefectRestResource.class)
                    .queryAccessedTaskAndToolName(queryTaskListReqVO).getData();
            if (CollectionUtils.isNotEmpty(taskListReqVOList)) {
                queryTaskListReqVODataList.addAll(taskListReqVOList);
            }
        }

        // 获取分析成功的ToolConfigInfoEntity
        List<ToolConfigInfoEntity> accToolConfigInfoEntity = new ArrayList<>();
        for (QueryTaskListReqVO queryTaskListReqVO : queryTaskListReqVODataList) {
            List<ToolConfigInfoEntity> toolConfigInfoEntityList = toolConfigInfoEntityMap.get(
                    queryTaskListReqVO.getTaskId() + queryTaskListReqVO.getToolName());
            if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
                for (ToolConfigInfoEntity toolConfigInfoEntity : toolConfigInfoEntityList) {
                    if (toolConfigInfoEntity != null) {
                        accToolConfigInfoEntity.add(toolConfigInfoEntity);
                    }
                }
            }
        }
        log.info("accToolConfigInfoEntity: [{}]", accToolConfigInfoEntity.size());

        // 3.批量更新跟进状态
        toolDao.batchUpdateToolFollowStatus(accToolConfigInfoEntity, FOLLOW_STATUS.ACCESSED);

        log.info("finish batchUpdateToolFollowStatus, count: {}", accToolConfigInfoEntity.size());
        return true;
    }

    @Override
    public ToolTaskInfoVO getToolInfoConfigByToolName(String toolName, String detailTime) {
        // 将字符串时间格式转换成时间戳
        long[] startTimeAndEndTime = DateTimeUtils.getStartTimeAndEndTime(detailTime, detailTime);
        long startTime = startTimeAndEndTime[0];
        long endTime = startTimeAndEndTime[1];
        // 组建返回数据
        Map<Integer, List<TaskDetailVO>> taskInfoMap = Maps.newHashMap();
        taskInfoMap.put(FOLLOW_STATUS.WITHDRAW.value(), Lists.newArrayList());
        taskInfoMap.put(FOLLOW_STATUS.ACCESSED.value(), Lists.newArrayList());
        // 项目id
        Set<String> totalProjectIds = Sets.newHashSet();
        Set<Long> totalTaskIds = Sets.newHashSet();

        // 机器创建的项目id
        String filterProjectId = StringUtils.isNotEmpty(toolName)
                ? String.format("%s%s", ComConstants.GRAY_PROJECT_PREFIX, toolName) : "";

        // 统计任务表
        statisticToolAccessAndStop(taskDao, startTime, endTime, totalProjectIds, totalTaskIds, toolName,
                taskInfoMap, filterProjectId);

        // 统计删除任务表
        statisticToolAccessAndStop(deletedTaskDao, startTime, endTime, totalProjectIds, totalTaskIds, toolName,
                taskInfoMap, filterProjectId);

        ToolTaskInfoVO toolTaskInfoVO = new ToolTaskInfoVO();
        toolTaskInfoVO.setTaskIdMap(taskInfoMap);
        toolTaskInfoVO.setTotalProjectCount(totalProjectIds.size());
        toolTaskInfoVO.setTotalTaskCount(totalTaskIds.size());
        return toolTaskInfoVO;
    }

    private void statisticToolAccessAndStop(CommonTaskDao commonTaskDao, Long startTime, Long endTime,
                                            Set<String> totalProjectIds, Set<Long> totalTaskIds, String toolName,
                                            Map<Integer, List<TaskDetailVO>> taskInfoMap, String filterProjectId) {

        int currentSize;
        Long lastTaskId = 0L;
        do {
            // 分页查询的任务id
            List<Long> taskIds =  commonTaskDao.getTaskIdList(lastTaskId, ComConstants.COMMON_NUM_10000,
                    filterProjectId);

            // 当前页任务中接入工具的任务id集合
            Set<Long> toolAccessTaskIds = getToolAccessTaskIds(taskIds, toolName);

            // 接入工具任务数和项目数总量添加
            totalTaskIds.addAll(toolAccessTaskIds);
            totalProjectIds.addAll(commonTaskDao.getProjectCount(toolAccessTaskIds).stream().map(
                    TaskProjectCountVO::getProjectId).collect(Collectors.toSet()));

            // 接入工具的任务id集合中满足首次使用条件和停用条件的任务信息
            Pair<List<TaskDetailVO>, List<TaskDetailVO>> firstAccessToolTaskInfo = getFirstAccessAndStopTaskInfo(
                    toolAccessTaskIds, toolName, startTime, endTime, commonTaskDao);
            // 组建返回内容
            taskInfoMap.get(FOLLOW_STATUS.ACCESSED.value()).addAll(firstAccessToolTaskInfo.getFirst());
            taskInfoMap.get(FOLLOW_STATUS.WITHDRAW.value()).addAll(firstAccessToolTaskInfo.getSecond());

            if (taskIds.size() > 0) {
                lastTaskId = taskIds.get(taskIds.size() - 1);
            }
            currentSize = taskIds.size();
        } while (currentSize >= ComConstants.COMMON_NUM_10000);
    }

    /**
     * 获取任务id中接入某工具的任务id集合
     * @param taskIds 任务id集合
     * @param toolName 工具名称
     * @return 接入工具的任务id集合
     */
    private Set<Long> getToolAccessTaskIds(List<Long> taskIds, String toolName) {
        List<ToolConfigInfoEntity> entities = toolDao.findByTaskIdsAndToolName(taskIds, toolName);

        // 当前页接入该工具的任务
        return entities.stream().map(ToolConfigInfoEntity::getTaskId).collect(Collectors.toSet());
    }

    /**
     * 接入工具的任务id集合中满足首次使用条件和停用条件的任务信息
     * @param toolAccessTaskIds 接入工具的任务列表
     * @param toolName 工具名称
     * @param startTime 起始时间
     * @param endTime
     * @param commonTaskDao
     * @return
     */
    private Pair<List<TaskDetailVO>, List<TaskDetailVO>> getFirstAccessAndStopTaskInfo(
            Set<Long> toolAccessTaskIds, String toolName, Long startTime, Long endTime, CommonTaskDao commonTaskDao) {

        // 工具首次接入在时间范围内
        List<ToolConfigInfoEntity> firstAccessTools = toolDao.findByTaskIdsAndToolNameAndTime(
                toolAccessTaskIds, toolName, startTime, endTime);
        Set<Long> firstAccessTaskIds =
                firstAccessTools.stream().map(ToolConfigInfoEntity::getTaskId).collect(Collectors.toSet());
        List<TaskInfoEntity> firstAccessTaskInfo = commonTaskDao.getTaskByTaskIds(firstAccessTaskIds);

        // 删除任务中,工具下架时间是当前时段
        List<ToolConfigInfoEntity> deletedStopToolEntities =
                toolDao.findStopByTaskIdsAndToolNameAndTime(toolAccessTaskIds, toolName, startTime, endTime);
        Set<Long> toolWithDrawTaskIds =  deletedStopToolEntities.stream().map(
                ToolConfigInfoEntity::getTaskId).collect(Collectors.toSet());
        List<TaskInfoEntity> stopToolTaskInfo = commonTaskDao.getTaskByTaskIds(toolWithDrawTaskIds);

        // 工具在接入状态，但是任务删除是当前时段
        List<ToolConfigInfoEntity> deletedTaskAccess = toolDao.findUseByTaskIdAndToolName(toolAccessTaskIds, toolName);
        Set<Long> deletedTaskAccessIds = deletedTaskAccess.stream().map(ToolConfigInfoEntity::getTaskId).collect(
                Collectors.toSet());
        // 剔除掉工具下架已统计的任务id
        deletedTaskAccessIds.removeAll(toolWithDrawTaskIds);
        List<TaskInfoEntity> deletedStopTasks = commonTaskDao.getStopTask(
                deletedTaskAccessIds, startTime, endTime);
        stopToolTaskInfo.addAll(deletedStopTasks);

        return Pair.of(taskInfoEntityToTaskDetailVO(firstAccessTaskInfo),
                taskInfoEntityToTaskDetailVO(stopToolTaskInfo));
    }


    private List<TaskDetailVO> taskInfoEntityToTaskDetailVO(List<TaskInfoEntity> taskInfoEntities) {
        return taskInfoEntities.stream().map(taskInfoEntity -> {
            TaskDetailVO taskDetailVO = new TaskDetailVO();
            BeanUtils.copyProperties(taskInfoEntity, taskDetailVO);
            return taskDetailVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> getTaskInfoByToolNameAndTaskId(List<Long> taskIdList, String toolName) {
        log.info("taskIdList :{} {}", taskIdList, toolName);
        List<ToolConfigInfoEntity> entities = toolDao.findByTaskIdsAndToolName(taskIdList, toolName);
        return entities.stream().map(ToolConfigInfoEntity::getTaskId).collect(Collectors.toList());
    }

    /**
     * 根据工具名分组 查询开源的工具数量
     *
     * @param taskIdList 任务id集合
     * @param toolCountData 容器
     * @param date 日期
     * @param endTime 时间
     * @param createFrom 来源
     */
    private void getToolCount(List<Long> taskIdList, List<ToolStatisticEntity> toolCountData, String date, long endTime,
            String createFrom) {

        List<ToolCountScriptEntity> toolCountScriptList = toolDao.findDailyToolCount(taskIdList, endTime);

        for (ToolCountScriptEntity toolCountScript : toolCountScriptList) {
            ToolStatisticEntity toolStatisticEntity = new ToolStatisticEntity();
            // 设置时间
            toolStatisticEntity.setDate(date);
            // 设置工具名称
            toolStatisticEntity.setToolName(toolCountScript.getToolName());
            // 设置来源
            toolStatisticEntity.setDataFrom(createFrom);
            // 设置总数量
            toolStatisticEntity.setToolCount(toolCountScript.getCount());

            toolCountData.add(toolStatisticEntity);
        }
    }


}
