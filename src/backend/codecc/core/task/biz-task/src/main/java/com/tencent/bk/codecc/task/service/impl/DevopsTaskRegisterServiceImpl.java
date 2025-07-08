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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
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

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService;
import com.tencent.bk.codecc.task.utils.CommonKafkaClient;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.auth.api.external.AuthExRegisterApi;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.audit.ActionAuditRecordContents;
import com.tencent.devops.common.constant.audit.ResourceTypes;
import com.tencent.devops.common.service.prometheus.BkTimed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 蓝盾任务注册服务实现类
 *
 * @version V1.0
 * @date 2019/5/6
 */
@Service("devopsTaskRegisterService")
@Slf4j
public class DevopsTaskRegisterServiceImpl extends AbstractTaskRegisterService {
    @Autowired
    private AuthExRegisterApi authExRegisterApi;

    @Autowired
    private CommonKafkaClient commonKafkaClient;

    @ActionAuditRecord(
            actionId = ActionIds.CREATE_TASK,
            instance = @AuditInstanceRecord(
                    resourceType = ResourceTypes.TASK,
                    instanceIds = "#$?.taskId",
                    instanceNames = "#$?.nameEn"
            ),
            content = ActionAuditRecordContents.CREATE_TASK
    )
    @BkTimed(value = "register_task")
    @Override
    public TaskIdVO registerTask(TaskDetailVO taskDetailVO, String userName) {
        // 添加 SCC 工具
        CheckerSetVO sccCheckerSet = new CheckerSetVO();
        sccCheckerSet.setCheckerSetId("standard_scc");
        sccCheckerSet.setToolList(Collections.singleton(Tool.SCC.name()));
        sccCheckerSet.setVersion(Integer.MAX_VALUE);
        sccCheckerSet.setCodeLang(1073741824L);
        taskDetailVO.getCheckerSetList().add(sccCheckerSet);
        taskDetailVO.setCodeLang(taskDetailVO.getCodeLang() | 1073741824L);

        // 注册任务
        taskDetailVO.setCreateFrom(ComConstants.BsTaskCreateFrom.BS_CODECC.value());
        String nameEn =
                getTaskStreamName(taskDetailVO.getProjectId(), taskDetailVO.getNameCn(), taskDetailVO.getCreateFrom());
        taskDetailVO.setNameEn(nameEn);
        taskDetailVO.setAtomCode(ComConstants.AtomCode.CODECC_V3.code());
        TaskInfoEntity taskInfoEntity = createTask(taskDetailVO, userName);
        //推送任务信息至数据平台
        commonKafkaClient.pushTaskDetailToKafka(taskInfoEntity);

        // 将任务注册到权限中心
        try {
            boolean registerTaskToAuth = authExRegisterApi
                    .registerCodeCCTask(userName, String.valueOf(taskInfoEntity.getTaskId()),
                            taskInfoEntity.getNameCn(), taskInfoEntity.getProjectId());
            if (!registerTaskToAuth) {
                rollbackTask(taskInfoEntity, "注册任务到权限中心失败", null);
            }
        } catch (Exception e) {
            rollbackTask(taskInfoEntity, "注册任务到权限中心失败", e);
        }

        // 关联规则集
        try {
            client.get(ServiceCheckerSetRestResource.class)
                    .batchRelateTaskAndCheckerSet(userName, taskDetailVO.getProjectId(), taskInfoEntity.getTaskId(),
                            taskDetailVO.getCheckerSetList(), false);
        } catch (Exception e) {
            rollbackTask(taskInfoEntity, "关联规则集", e);
        }

        // 注册工具
        try {
            registerTools(taskDetailVO, taskInfoEntity, userName);
        } catch (Exception e) {
            rollbackTask(taskInfoEntity, "注册工具失败", e);
        }

        // 同步刷任务的组织架构信息
        refreshOrgInfo(taskInfoEntity.getTaskId());

        // 为任务添加流水线构建完成回调
        checkAndAddPipelineFinishCallBack(taskInfoEntity.getProjectId(), taskInfoEntity.getPipelineId(), userName);

        log.info("register task from codecc successfully! task: {}", taskInfoEntity);
        return new TaskIdVO(taskInfoEntity.getTaskId(), taskInfoEntity.getNameEn());
    }

    protected void registerTools(TaskDetailVO taskDetailVO, TaskInfoEntity taskInfoEntity, String userName) {
        Set<String> toolSet = new HashSet<>();
        taskDetailVO.getCheckerSetList().forEach(checkerSetVO -> {
            if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList())) {
                toolSet.addAll(checkerSetVO.getToolList());
            }

        });

        List<ToolConfigInfoVO> toolList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolSet)) {
            toolSet.forEach(toolName -> {
                if (StringUtils.isNotBlank(toolName)) {
                    ToolConfigInfoVO toolConfigInfoVO = instBatchToolInfoModel(taskDetailVO, toolName);
                    toolList.add(toolConfigInfoVO);
                }
            });
        }

        BatchRegisterVO batchRegisterVO = new BatchRegisterVO();
        BeanUtils.copyProperties(taskDetailVO, batchRegisterVO);
        batchRegisterVO.setTools(toolList);
        toolService.registerTools(batchRegisterVO, taskInfoEntity, userName);
    }

    protected TaskIdVO rollbackTask(TaskInfoEntity taskInfoEntity, String errMsg, Exception e) {
        log.error("{}: {}", errMsg, taskInfoEntity.getPipelineId(), e);
        taskRepository.delete(taskInfoEntity);
        throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, null);
    }

    @Override
    public Boolean updateTask(TaskDetailVO taskDetailVO, String userName) {
        return null;
    }

}
