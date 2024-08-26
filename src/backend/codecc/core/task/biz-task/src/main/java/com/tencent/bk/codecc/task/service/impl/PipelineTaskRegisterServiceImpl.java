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

package com.tencent.bk.codecc.task.service.impl;

import static com.tencent.devops.common.constant.CommonMessageCode.UTIL_EXECUTE_FAIL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.task.component.BaseDataCommonCache;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskConstants.TaskStatus;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.CustomProjEntity;
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService;
import com.tencent.bk.codecc.task.utils.CommonKafkaClient;
import com.tencent.bk.codecc.task.utils.OrgInfoUtils;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.CheckerSetPackageType;
import com.tencent.devops.common.constant.ComConstants.CodeLang;
import com.tencent.devops.common.constant.ComConstants.OpenSourceCheckerSetType;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.prometheus.BkTimed;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.TaskCreateFromUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 流水线创建任务实现类
 * 分三种情况：
 * 已注册且有效，则返回项目已经存在
 * 已注册且无效，则重置为有效，并且添加资源到蓝盾平台，添加或者更新工具配置
 * 未注册，则全新注册
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Service("pipelineTaskRegisterService")
@Slf4j
public class PipelineTaskRegisterServiceImpl extends AbstractTaskRegisterService {

    private static final Map<String, String> defaultCheckerSetMap = createDefaultCheckerSetMap();
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CommonKafkaClient commonKafkaClient;
    @Autowired
    private BaseDataRepository baseDataRepository;
    @Autowired
    private BaseDataCommonCache baseDataCommonCache;
    @Autowired
    private TaskDao taskDao;

    private static Map<String, String> createDefaultCheckerSetMap() {
        Map<String, String> folderMap = new HashMap<>();
        folderMap.put(Tool.COVERITY.name(), "codecc_default_rules_" + Tool.COVERITY.name().toLowerCase());
        folderMap.put(Tool.KLOCWORK.name(), "codecc_default_rules_" + Tool.KLOCWORK.name().toLowerCase());
        folderMap.put(Tool.PINPOINT.name(), "codecc_default_rules_" + Tool.PINPOINT.name().toLowerCase());
        folderMap.put(Tool.CPPLINT.name(), "codecc_default_rules_" + Tool.CPPLINT.name().toLowerCase());
        folderMap.put(Tool.CHECKSTYLE.name(), "codecc_default_rules_" + Tool.CHECKSTYLE.name().toLowerCase());
        folderMap.put(Tool.STYLECOP.name(), "codecc_default_rules_" + Tool.STYLECOP.name().toLowerCase());
        folderMap.put(Tool.GOML.name(), "codecc_default_rules_" + Tool.GOML.name().toLowerCase());
        folderMap.put(Tool.DETEKT.name(), "codecc_default_rules_" + Tool.DETEKT.name().toLowerCase());
        folderMap.put(Tool.PYLINT.name(), "codecc_default_rules_" + Tool.PYLINT.name().toLowerCase());
        folderMap.put(Tool.OCCHECK.name(), "codecc_default_rules_" + Tool.OCCHECK.name().toLowerCase());
        folderMap.put(ComConstants.PHPCSStandardCode.PSR2.name(), "codecc_default_psr2_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.PSR12.name(), "codecc_default_psr12_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.PSR1.name(), "codecc_default_psr1_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.PEAR.name(), "codecc_default_pear_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.Zend.name(), "codecc_default_zend_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.Squiz.name(), "codecc_default_squiz_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.MySource.name(), "codecc_default_mysource_rules");
        folderMap.put(ComConstants.PHPCSStandardCode.Generic.name(), "codecc_default_generic_rules");
        folderMap.put(ComConstants.EslintFrameworkType.react.name(), "codecc_default_react_rules");
        folderMap.put(ComConstants.EslintFrameworkType.vue.name(), "codecc_default_vue_rules");
        folderMap.put(ComConstants.EslintFrameworkType.standard.name(), "codecc_default_standard_rules");
        folderMap.put(Tool.SENSITIVE.name(), "codecc_default_rules_" + Tool.SENSITIVE.name().toLowerCase());
        folderMap.put(Tool.HORUSPY.name(), "codecc_v2_default_" + Tool.HORUSPY.name().toLowerCase());
        folderMap.put(Tool.WOODPECKER_SENSITIVE.name(),
                "codecc_v2_default_" + Tool.WOODPECKER_SENSITIVE.name().toLowerCase());
        folderMap.put(Tool.RIPS.name(), "codecc_v2_default_" + Tool.RIPS.name().toLowerCase());
        return folderMap;
    }

    @BkTimed(value = "register_task")
    @Override
    public TaskIdVO registerTask(TaskDetailVO taskDetailVO, String userName) {
        if (StringUtils.isBlank(taskDetailVO.getMultiPipelineMark())) {
            taskDetailVO.setMultiPipelineMark(null);
        }
        setCreateFrom(taskDetailVO);
        String nameCn = handleCnName(taskDetailVO.getPipelineName());
        if (StringUtils.isNotBlank(taskDetailVO.getMultiPipelineMark())) {
            nameCn = String.format("%s(%s)", nameCn, taskDetailVO.getMultiPipelineMark());
        }
        taskDetailVO.setNameCn(nameCn);
        TaskInfoEntity taskInfoEntity;
        if (taskDetailVO.getTaskId() > 0L) {
            taskInfoEntity = taskRepository.findFirstByTaskId(taskDetailVO.getTaskId());
        } else if (StringUtils.isNotEmpty(taskDetailVO.getPipelineId())) {
            //先判断，如果超过50条流水线，则报错
            Long pipelineNum = taskRepository.countByPipelineId(taskDetailVO.getPipelineId());
            Integer defaultPipelineTaskLimit = baseDataCommonCache.getDefaultPipelineTaskLimit();
            if (pipelineNum >= defaultPipelineTaskLimit) {
                //判断是否单独设置了阙值
                BaseDataEntity entity = baseDataRepository.findFirstByParamTypeAndParamCode(
                        ComConstants.KEY_PIPELINE_TASK_LIMIT, taskDetailVO.getPipelineId());
                Integer pipelineTaskLimit = entity == null || StringUtils.isEmpty(entity.getParamValue()) ? null :
                        Integer.parseInt(entity.getParamValue());
                if (pipelineTaskLimit == null || pipelineNum > pipelineTaskLimit) {
                    Integer limit = pipelineTaskLimit != null ? pipelineTaskLimit : defaultPipelineTaskLimit;
                    log.info("pipeline : {} task number : {}  is larger than {}", taskDetailVO.getPipelineId(),
                            pipelineNum, limit);
                    throw new CodeCCException(TaskMessageCode.PIPELINE_TASK_REACH_LIMIT,
                            new String[]{String.valueOf(limit)},
                            String.format("该流水线的代码分析任务数达到上限: %d，"
                                    + "请删除不必要的任务，或者联系DevOps-helper处理。", limit), null);
                }
            }
            /*
             * 添加一条流水线对应多个代码扫描任务的逻辑
             */
            taskInfoEntity = taskRepository.findFirstByPipelineIdAndMultiPipelineMark(taskDetailVO.getPipelineId(),
                    taskDetailVO.getMultiPipelineMark());
        } else {
            // 工蜂创建任务时，是没有传pipelineId的，而且工蜂的任务的流名称是根据projectId和pipelineName生成的
            String nameEn = getTaskStreamName(taskDetailVO.getProjectId(),
                    taskDetailVO.getNameCn(), taskDetailVO.getCreateFrom());
            taskDetailVO.setNameEn(nameEn);
            taskInfoEntity = taskRepository.findFirstByNameEn(nameEn);
        }

        //已注册且是工蜂来源
        if (taskInfoEntity != null && null != taskInfoEntity.getGongfengFlag()) {
            if (taskInfoEntity.getGongfengFlag()) {
                log.info("the task from Gongfeng has been registered");
                if (StringUtils.isBlank(taskDetailVO.getDevopsTools())) {
                    try {
                        taskDetailVO.setDevopsTools(JsonUtil.INSTANCE.getObjectMapper()
                                .writeValueAsString(new ArrayList<String>() {
                                    {
                                        add("SCC");
                                    }
                                }));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        log.error("serialize task tool list fail! task id: {}", taskDetailVO.getTaskId(), e);
                    }
                }
                if (null != taskDetailVO.getGongfengFlag() && !taskDetailVO.getGongfengFlag()) {
                    taskInfoEntity.setGongfengFlag(false);
                    // 更新任务信息
                    updateTaskInfo(taskDetailVO, taskInfoEntity, userName);

                    // 添加或者更新工具配置
                    upsertTools(taskDetailVO, taskInfoEntity, userName);
                }
            }
        } else if (taskInfoEntity != null) {
            //已注册
            log.info("the task has been registered");

            updateTaskInfo(taskDetailVO, taskInfoEntity, userName);

            // 添加或者更新工具配置
            upsertTools(taskDetailVO, taskInfoEntity, userName);
        } else if (taskDetailVO.getGongfengFlag() != null) {
            //来自工蜂且未注册
            log.info("begin to create task from Gongfeng");
            String nameEn = getTaskStreamName(taskDetailVO.getProjectId(),
                    taskDetailVO.getNameCn(), taskDetailVO.getCreateFrom());
            taskDetailVO.setNameEn(nameEn);
            taskInfoEntity = createTask(taskDetailVO, userName);
            if (null != taskDetailVO.getCustomProjInfo()) {
                CustomProjEntity customProjEntity = new CustomProjEntity();
                BeanUtils.copyProperties(taskDetailVO.getCustomProjInfo(), customProjEntity);
                taskInfoEntity.setCustomProjInfo(customProjEntity);
            }
            //来自工蜂的全部为全量扫描
            taskInfoEntity.setScanType(ComConstants.ScanType.FULL.code);

            //添加或者更新工具配置
            upsertTools(taskDetailVO, taskInfoEntity, userName);
        } else {
            //来自个性化触发且未注册
            log.info("begin to create task");
            if (StringUtils.isNotEmpty(taskDetailVO.getDevopsCodeLang())) {
                taskDetailVO.setCodeLang(
                        pipelineService.convertDevopsCodeLangToCodeCC(taskDetailVO.getDevopsCodeLang()));
            }
            String finalPipelineId = taskDetailVO.getPipelineId();
            if (StringUtils.isNotBlank(taskDetailVO.getMultiPipelineMark())) {
                finalPipelineId = String.format("%s_%s", finalPipelineId, taskDetailVO.getMultiPipelineMark());
            }
            String nameEn = getTaskStreamName(taskDetailVO.getProjectId(), finalPipelineId,
                    taskDetailVO.getCreateFrom());
            taskDetailVO.setNameEn(nameEn);
            taskInfoEntity = createTask(taskDetailVO, userName);
            //发送数据到数据平台
            commonKafkaClient.pushTaskDetailToKafka(taskInfoEntity);

            //添加或者更新工具配置
            upsertTools(taskDetailVO, taskInfoEntity, userName);

            // 为任务添加流水线构建完成回调
            checkAndAddPipelineFinishCallBack(taskInfoEntity.getProjectId(), taskInfoEntity.getPipelineId(), userName);

            //发送数据到数据平台
//            sendTaskDetail(taskInfoEntity);
        }
        // 前面存在已注册的的逻辑，先判断是否已有bg信息
        if (taskInfoEntity.getBgId() <= 0) {
            refreshOrgInfo(taskInfoEntity.getTaskId());
        }

        log.info("register task from pipeline successfully! task: {}", taskInfoEntity);
        return new TaskIdVO(taskInfoEntity.getTaskId(), taskInfoEntity.getNameEn());
    }

    @Override
    public Boolean updateTask(TaskDetailVO taskDetailVO, String userName) {
        long taskId = taskDetailVO.getTaskId();
        TaskInfoEntity taskInfoEntity = taskRepository.findFirstByTaskId(taskId);
        if (StringUtils.isEmpty(taskInfoEntity.getProjectId())
                || !ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()
                .equalsIgnoreCase(taskInfoEntity.getCreateFrom())) {
            log.error("task not from pipeline should be updated, task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{String.valueOf(taskId)}, null);
        }

        //更新任务信息
        updateTaskInfo(taskDetailVO, taskInfoEntity, userName);

        //添加或者更新工具配置
        upsertTools(taskDetailVO, taskInfoEntity, userName);

        log.info("update task info from pipeline successfully! task id: {}", taskId);
        return true;
    }

    /**
     * 更新任务信息
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     */
    private void updateTaskInfo(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName) {
        // 若是冷数据，则需要加热操作；不能直接跳过到ENABLE
        if (!Objects.equals(TaskStatus.COLD.value(), taskInfoEntity.getStatus())) {
            taskInfoEntity.setStatus(TaskConstants.TaskStatus.ENABLE.value());
            taskInfoEntity.setDisableTime("");
        }

        try {
            taskInfoEntity.setCodeLang(pipelineService.convertDevopsCodeLangToCodeCC(taskDetailVO.getDevopsCodeLang()));
            taskDetailVO.setCodeLang(taskInfoEntity.getCodeLang());
        } catch (StreamException e) {
            log.error("deserialize devops code lang fail! code lang info: {}", taskDetailVO);
            throw new CodeCCException(UTIL_EXECUTE_FAIL);
        }

        // 支持新老插件切换
        if (StringUtils.isEmpty(taskInfoEntity.getAtomCode()) && StringUtils.isNotEmpty(taskDetailVO.getAtomCode())) {
            taskDetailVO.setOldAtomCodeChangeToNew(true);
        }
        taskInfoEntity.setAtomCode(taskDetailVO.getAtomCode());

        //更新任务中文名
        taskInfoEntity.setNameCn(taskDetailVO.getNameCn());
        //更新蓝盾任务名
        taskInfoEntity.setProjectName(taskDetailVO.getProjectName());
        //更新编译平台
        taskInfoEntity.setCompilePlat(taskDetailVO.getCompilePlat());
        taskInfoEntity.setOsType(taskDetailVO.getOsType());
        taskInfoEntity.setProjectBuildType(taskDetailVO.getProjectBuildType());
        // 判断脚本是否更改，先不更新
        // taskInfoEntity.setProjectBuildCommand(taskDetailVO.getProjectBuildCommand());
        taskInfoEntity.setUpdatedBy(userName);
        taskInfoEntity.setUpdatedDate(System.currentTimeMillis());
        taskInfoEntity.setAutoLang(taskDetailVO.getAutoLang());
        taskInfoEntity.setCheckerSetType(taskDetailVO.getCheckerSetType() == null ? CheckerSetPackageType.NORMAL.value()
                : taskDetailVO.getCheckerSetType().value());
        taskInfoEntity.setCheckerSetEnvType(taskDetailVO.getCheckerSetEnvType());
        //流水线id也要更新
        if (StringUtils.isNotBlank(taskDetailVO.getPipelineId())) {
            taskInfoEntity.setPipelineId(taskDetailVO.getPipelineId());
        }
        taskRepository.save(taskInfoEntity);
    }

    /**
     * 为工蜂做工具注册
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     */
    private void upsertToolsForGongfeng(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName) {
        // 初始化工具列表
        Set<String> reqToolSet = new HashSet<>();
        String devopsTools = taskDetailVO.getDevopsTools();
        JSONArray newToolJsonArray = new JSONArray(devopsTools);
        for (int i = 0; i < newToolJsonArray.length(); i++) {
            if (StringUtils.isNotEmpty(newToolJsonArray.getString(i))) {
                reqToolSet.add(newToolJsonArray.getString(i));
            }
        }
        log.info("req tools: {}, {}", reqToolSet.size(), reqToolSet.toString());

        List<ToolConfigInfoVO> toolList = new ArrayList<>();
        reqToolSet.forEach(toolName -> {
            ToolConfigInfoVO toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, toolName);
            toolList.add(toolConfigInfoVO);
        });
        taskDetailVO.setToolConfigInfoList(toolList);
        List<String> forceFullScanTools = new ArrayList<>();

        // 更新保存工具，包括新添加工具、信息修改工具、停用工具、启用工具
        upsert(taskDetailVO, taskInfoEntity, userName, forceFullScanTools);

        // 设置强制全量扫描标志
        if (CollectionUtils.isNotEmpty(forceFullScanTools)) {
            log.info("set force full scan, taskId:{}, toolNames:{}", taskDetailVO.getTaskId(), forceFullScanTools);
            client.get(ServiceToolBuildInfoResource.class)
                    .setForceFullScan(taskDetailVO.getTaskId(), forceFullScanTools);
        }
    }

    /**
     * 更新工具配置信息和工具接入状态
     *
     * @param taskDetailVO
     * @param taskInfoEntity
     * @param userName
     */
    private void upsertTools(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName) {
        fillTaskDetailVOBgInfo(taskDetailVO, taskInfoEntity);
        configCheckerSetByType(taskDetailVO);
        // 如果不带有插件code，表示是旧插件接入，需要适配兼容旧插件
        if (StringUtils.isEmpty(taskDetailVO.getAtomCode())) {
            adaptV1AtomCodeCC(taskDetailVO);
        } else {
            adaptV3AtomCodeCC(taskDetailVO);
        }

        long taskId = taskInfoEntity.getTaskId();
        List<String> forceFullScanTools = new ArrayList<>();

        // 更新保存工具，包括新添加工具、信息修改工具、停用工具、启用工具
        upsert(taskDetailVO, taskInfoEntity, userName, forceFullScanTools);

        // 更新关联的规则集
        client.get(ServiceCheckerSetRestResource.class).batchRelateTaskAndCheckerSet(userName,
                taskInfoEntity.getProjectId(),
                taskId,
                taskDetailVO.getCheckerSetList(),
                Optional.ofNullable(taskDetailVO.getCreateFrom()).orElse("")
                        .equals(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()));

        // 设置强制全量扫描标志
        if (CollectionUtils.isNotEmpty(forceFullScanTools)) {
            log.info("set force full scan, taskId:{}, toolNames:{}", taskDetailVO.getTaskId(), forceFullScanTools);
            client.get(ServiceToolBuildInfoResource.class)
                    .setForceFullScan(taskDetailVO.getTaskId(), forceFullScanTools);
        }
    }

    private void fillTaskDetailVOBgInfo(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity) {
        // 判断taskInfoEntity是否包含BgInfo
        if (taskInfoEntity != null && taskInfoEntity.getBgId() > 0) {
            taskDetailVO.setBgId(taskInfoEntity.getBgId());
            if (null != taskInfoEntity.getBusinessLineId()) {
                taskDetailVO.setBusinessLineId(taskInfoEntity.getBusinessLineId());
            }
            taskDetailVO.setDeptId(taskInfoEntity.getDeptId());
            taskDetailVO.setCenterId(taskInfoEntity.getCenterId());
            taskDetailVO.setGroupId(taskInfoEntity.getGroupId());
        } else if (StringUtils.isNotBlank(taskDetailVO.getProjectId())) {
            OrgInfoVO orgInfoVO = OrgInfoUtils.getOrgInfoByProjectId(taskDetailVO.getProjectId());
            taskDetailVO.setBgId(orgInfoVO.getBgId());
            if (null != orgInfoVO.getBusinessLineId()) {
                taskDetailVO.setBusinessLineId(orgInfoVO.getBusinessLineId());
            }
            taskDetailVO.setDeptId(orgInfoVO.getDeptId());
            taskDetailVO.setCenterId(orgInfoVO.getCenterId());
            taskDetailVO.setGroupId(orgInfoVO.getGroupId());
        }
    }

    private void adaptV1AtomCodeCC(TaskDetailVO taskDetailVO) {
        // 初始化工具列表
        Set<String> reqToolSet = new HashSet<>();
        String devopsTools = taskDetailVO.getDevopsTools();
        JSONArray newToolJsonArray = new JSONArray(devopsTools);
        for (int i = 0; i < newToolJsonArray.length(); i++) {
            if (StringUtils.isNotEmpty(newToolJsonArray.getString(i))) {
                reqToolSet.add(newToolJsonArray.getString(i));
            }
        }
        log.info("req tools: {}, {}", reqToolSet.size(), reqToolSet.toString());

        List<ToolConfigInfoVO> toolList = new ArrayList<>();
        reqToolSet.forEach(toolName -> {
            if (!Tool.CLOC.name().equalsIgnoreCase(toolName)) {
                ToolConfigInfoVO toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, toolName);
                toolList.add(toolConfigInfoVO);
            }
        });
        taskDetailVO.setToolConfigInfoList(toolList);

        // 初始化规则集列表
        Set<String> hasCheckerSetTools = new HashSet<>();
        List<CheckerSetVO> checkerSetList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taskDetailVO.getToolCheckerSets())) {
            checkerSetList = taskDetailVO.getToolCheckerSets().stream()
                    .filter(toolCheckerSetVO -> reqToolSet.contains(toolCheckerSetVO.getToolName()))
                    .map(toolCheckerSetVO -> {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        checkerSetVO.setCheckerSetId(toolCheckerSetVO.getCheckerSetId());
                        checkerSetVO.setToolList(Sets.newHashSet(toolCheckerSetVO.getToolName()));
                        hasCheckerSetTools.add(toolCheckerSetVO.getToolName());
                        return checkerSetVO;
                    }).collect(Collectors.toList());
        }

        // 没有选择规则集的，且任务工具关联的规则集为空，则自动选择默认规则集，默认规则集的ID为：codecc_default_rules_toolNmae(小写)
        if (hasCheckerSetTools.size() < reqToolSet.size()) {
            Result<List<CheckerSetVO>> result = client.get(ServiceCheckerSetRestResource.class)
                    .getCheckerSets(taskDetailVO.getTaskId());
            if (result.isNotOk() || result.getData() == null) {
                log.error("query checker sets fail, result: {}", result);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            List<CheckerSetVO> existCheckerSetList = result.getData();
            Map<String, CheckerSetVO> toolCheckerSetMap = existCheckerSetList.stream()
                    .collect(Collectors.toMap(checkerSetVO -> checkerSetVO.getToolList().iterator().next(),
                            Function.identity(), (k, v) -> v));
            for (ToolConfigInfoVO toolConfigInfoVO : toolList) {
                String toolName = toolConfigInfoVO.getToolName();
                if (!hasCheckerSetTools.contains(toolName) && !toolName.equals(Tool.CCN.name())
                        && !toolName.equals(Tool.DUPC.name()) && !toolName.equals(Tool.CLOC.name())
                        && !toolName.equals(Tool.SCC.name())) {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    String checkerSetId = null;
                    // 根据工具信息获取默认规则集
                    if (toolCheckerSetMap.get(toolName) == null) {
                        checkerSetId = getDefaultCheckerSetId(toolConfigInfoVO);
                    } else {
                        checkerSetId = toolCheckerSetMap.get(toolName).getCheckerSetId();
                    }
                    checkerSetVO.setCheckerSetId(checkerSetId);
                    checkerSetVO.setToolList(Sets.newHashSet(toolName));
                    checkerSetList.add(checkerSetVO);
                }
            }
        }

        log.info("checkerSetList: {}", checkerSetList);
        taskDetailVO.setCheckerSetList(checkerSetList);
    }

    /**
     * 组装注册参数
     *
     * @param taskDetailVO
     */
    private void setCreateFrom(TaskDetailVO taskDetailVO) {
        if (taskDetailVO.getGongfengFlag() != null && taskDetailVO.getGongfengFlag()) {
            taskDetailVO.setCreateFrom(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
        } else {
            taskDetailVO.setCreateFrom(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());
        }
    }

    private String getDefaultCheckerSetId(ToolConfigInfoVO toolConfigInfoVO) {
        String checkerSetId;
        if (Tool.PHPCS.name().equals(toolConfigInfoVO.getToolName())) {
            checkerSetId = getCheckerSetId4PHPCS(toolConfigInfoVO);
        } else if (Tool.ESLINT.name().equals(toolConfigInfoVO.getToolName())) {
            checkerSetId = getCheckerSetId4ESLINT(toolConfigInfoVO);
        } else {
            checkerSetId = defaultCheckerSetMap.get(toolConfigInfoVO.getToolName());
        }

        if (StringUtils.isEmpty(checkerSetId)) {
            checkerSetId = "codecc_default_rules_" + toolConfigInfoVO.getToolName().toLowerCase();
        }

        log.info("default checkerSetId: {}", checkerSetId);
        return checkerSetId;
    }

    private String getCheckerSetId4ESLINT(ToolConfigInfoVO tool) {
        String eslintRc = null;
        String paramJson = tool.getParamJson();
        if (StringUtils.isNotEmpty(paramJson)) {
            try {
                eslintRc = new JSONObject(paramJson).getString(ComConstants.PARAM_ESLINT_RC);
            } catch (Exception e) {
                log.error("get eslint_rc error: {}", tool.getTaskId());
            }
        }

        if (StringUtils.isEmpty(eslintRc)) {
            eslintRc = ComConstants.EslintFrameworkType.standard.name();
        }
        return defaultCheckerSetMap.get(eslintRc);
    }

    private String getCheckerSetId4PHPCS(ToolConfigInfoVO tool) {
        String phpcsStandard = null;
        String paramJson = tool.getParamJson();
        if (StringUtils.isNotEmpty(paramJson)) {
            try {
                phpcsStandard = new JSONObject(paramJson).getString(ComConstants.KEY_PHPCS_STANDARD);
            } catch (Exception e) {
                log.error("get phpcs_standard error: {}", tool.getTaskId());
            }
        }

        if (StringUtils.isEmpty(phpcsStandard)) {
            phpcsStandard = ComConstants.PHPCSStandardCode.PSR2.name();
        }
        return defaultCheckerSetMap.get(phpcsStandard);
    }

    private List<CheckerSetVO> setCheckerSetsAccordingToLanguageAndType(
            List<String> languages,
            CheckerSetPackageType type,
            String checkerSetEnvType,
            OrgInfoVO orgInfo,
            BsTaskCreateFrom taskCreateFrom
    ) {
        List<CheckerSetVO> checkerSetVOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(languages)) {
            return checkerSetVOList;
        }
        List<BaseDataEntity> metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG);
        Set<OpenSourceCheckerSetType> finalOpensourceCheckerSetType =
                Collections.singleton(OpenSourceCheckerSetType.FULL);
        // EPC_SCAN 不过滤CheckerSetType 且 不自动配置其他规则
        final boolean[] otherLanguageCheckerSet = new boolean[]{CheckerSetPackageType.EPC_SCAN == type};
        final boolean skipFilterCheckerSetType = CheckerSetPackageType.EPC_SCAN == type;
        languages.forEach(it -> {
            BaseDataEntity selectedBaseData = pickSelectLanguageBaseData(metaLangList, it);
            // 如果有选中的语言，并且规则集配置不为空的话，则配置相应的规则集
            // 判断是否要用预发布的版本
            List<OpenSourceCheckerSet> selectedCheckerSet =
                    getOpenSourceCheckerSet(selectedBaseData, type, checkerSetEnvType, orgInfo, taskCreateFrom);
            if (!selectedCheckerSet.isEmpty()) {
                selectedCheckerSet.stream().filter(checkerSet ->
                        skipFilterCheckerSetType || StringUtils.isBlank(checkerSet.getCheckerSetType())
                                || finalOpensourceCheckerSetType.contains(OpenSourceCheckerSetType
                                .valueOf(checkerSet.getCheckerSetType()))
                ).forEach(checkerSet -> {
                    checkerSetVOList.add(covertOpenSourceCheckerSetToVO(checkerSet));
                });
            }

            // 如果包含有codecc不支持的语言，则配置啄木鸟-其他规则集
            if (null == selectedBaseData && !otherLanguageCheckerSet[0]) {
                BaseDataEntity otherBaseData = pickSelectLanguageBaseData(metaLangList, CodeLang.OTHERS.name());
                // 判断是否要用预发布的版本
                List<OpenSourceCheckerSet> otherSelectedCheckerSet =
                        getOpenSourceCheckerSet(otherBaseData, type, checkerSetEnvType, orgInfo, taskCreateFrom);

                if (!otherSelectedCheckerSet.isEmpty()) {
                    otherSelectedCheckerSet.forEach(checkerSet -> {
                        checkerSetVOList.add(covertOpenSourceCheckerSetToVO(checkerSet));
                        otherLanguageCheckerSet[0] = true;
                    });
                }
            }
        });
        return checkerSetVOList;
    }

    private boolean configCheckerSetByType(TaskDetailVO taskDetailVO) {
        if (taskDetailVO.getCheckerSetType() == null) {
            return configOldAtomCheckerSet(taskDetailVO);
        } else {
            return configNewAtomCheckerSet(taskDetailVO);
        }
    }

    private boolean configOldAtomCheckerSet(TaskDetailVO taskDetailVO) {
        if (taskDetailVO.getLanguages() != null) {
            OrgInfoVO orgInfo = new OrgInfoVO(taskDetailVO.getBgId(), taskDetailVO.getBusinessLineId(),
                    taskDetailVO.getDeptId(), taskDetailVO.getCenterId(), taskDetailVO.getGroupId());
            List<CheckerSetVO> checkerSetList = setCheckerSetsAccordingToLanguageAndType(taskDetailVO.getLanguages(),
                    CheckerSetPackageType.OPEN_SCAN, taskDetailVO.getCheckerSetEnvType(), orgInfo,
                    BsTaskCreateFrom.BS_PIPELINE);
            log.info("set old open scan checker set: {} {} {} {}",
                    taskDetailVO.getTaskId(),
                    taskDetailVO.getNameEn(),
                    taskDetailVO.getLanguages(),
                    checkerSetList);
            taskDetailVO.setCheckerSetList(checkerSetList);
            addSccCheckerSet(taskDetailVO);
            return true;
        } else {
            addSccCheckerSet(taskDetailVO);
            return false;
        }
    }

    private boolean configNewAtomCheckerSet(TaskDetailVO taskDetailVO) {
        List<String> languages = JsonUtil.INSTANCE.to(taskDetailVO.getDevopsCodeLang());
        List<CheckerSetVO> checkerSetList;
        log.info("before set new open scan checker set: {} {} {} {}",
                taskDetailVO.getTaskId(),
                taskDetailVO.getNameEn(),
                languages,
                taskDetailVO.getCheckerSetType());
        OrgInfoVO orgInfo = new OrgInfoVO(taskDetailVO.getBgId(), taskDetailVO.getBusinessLineId(),
                taskDetailVO.getDeptId(), taskDetailVO.getCenterId(), taskDetailVO.getGroupId());
        if (taskDetailVO.getCheckerSetType() != null
                && taskDetailVO.getCheckerSetType() != CheckerSetPackageType.NORMAL) {
            checkerSetList = setCheckerSetsAccordingToLanguageAndType(languages, taskDetailVO.getCheckerSetType(),
                    taskDetailVO.getCheckerSetEnvType(), orgInfo, BsTaskCreateFrom.BS_PIPELINE);
        } else {
            return false;
        }
        log.info("set new open scan checker set: {} {} {} {}",
                taskDetailVO.getTaskId(),
                taskDetailVO.getNameEn(),
                languages,
                checkerSetList);
        taskDetailVO.setCheckerSetList(checkerSetList);
        addSccCheckerSet(taskDetailVO);
        return true;
    }

    private void addSccCheckerSet(TaskDetailVO taskDetailVO) {
        CheckerSetVO sccCheckerSet = new CheckerSetVO();
        sccCheckerSet.setCheckerSetId("standard_scc");
        sccCheckerSet.setToolList(Collections.singleton(Tool.SCC.name()));
        sccCheckerSet.setVersion(Integer.MAX_VALUE);
        sccCheckerSet.setCodeLang(1073741824L);
        if (taskDetailVO.getCheckerSetList() == null) {
            taskDetailVO.setCheckerSetList(new ArrayList<>());
        }
        taskDetailVO.getCheckerSetList().add(sccCheckerSet);
    }


}
