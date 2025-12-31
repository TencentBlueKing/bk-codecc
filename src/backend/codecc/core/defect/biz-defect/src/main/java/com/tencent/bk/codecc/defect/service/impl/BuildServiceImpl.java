/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CheckerSetsVersionInfoEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;


/**
 * 构建逻辑实现层
 * 
 * @date 2021/11/19
 * @version V1.0
 */
@Service
public class BuildServiceImpl implements BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildServiceImpl.class);
    @Autowired
    private Client client;
    @Autowired
    private BuildDao buildDao;
    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private ICheckerSetQueryBizService checkerSetQueryBizService;

    @Override
    public BuildVO upsertAndGetBuildInfo(BuildVO buildVO) {
        logger.info("start to upsert build info, task id,:{} build id: {}",
                buildVO.getTaskId(), buildVO.getBuildId());
        BuildEntity buildEntity = new BuildEntity();
        BeanUtils.copyProperties(buildVO, buildEntity);
        buildEntity.setBuildNo(buildVO.getBuildNum());

        // 获取当前构建任务使用的规则集和版本信息
        List<CheckerSetsVersionInfoEntity> checkerSetsVersionInfo = null;
        Long taskId = buildVO.getTaskId();
        if (taskId != null) {
            checkerSetsVersionInfo = getCheckerSetsVersionByTaskId(taskId);
            buildEntity.setCheckerSetsVersion(checkerSetsVersionInfo);
        }

        buildEntity = buildDao.upsertBuildInfo(buildEntity);
        if (null == buildEntity) {
            return null;
        }
        BuildVO resultBuildVO = new BuildVO();
        BeanUtils.copyProperties(buildEntity, resultBuildVO);
        resultBuildVO.setBuildNum(buildEntity.getBuildNo());
        return resultBuildVO;
    }

    @Override
    public BuildVO getBuildVOByBuildId(String buildId) {
        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        if (null == buildEntity) {
            return null;
        }
        BuildVO resultBuildVO = new BuildVO();
        BeanUtils.copyProperties(buildEntity, resultBuildVO);
        resultBuildVO.setBuildNum(buildEntity.getBuildNo());
        return resultBuildVO;
    }

    @Override
    public BuildEntity getBuildEntityByBuildId(String buildId) {
        return buildRepository.findFirstByBuildId(buildId);
    }

    @Override
    public BuildEntity getBuildEntityByBuildIdAndTaskId(String buildId, Long taskId) {
        return buildRepository.findFirstByBuildIdAndTaskId(buildId, taskId);
    }

    @Override
    public List<BuildEntity> getBuildEntityInBuildIds(List<String> buildIds) {
        return buildRepository.findByBuildIdIn(buildIds);
    }

    @Override
    public String getBuildInfo(String buildId, Long taskId, String toolName, TaskDetailVO taskDetailVO,
            Set<String> whitePaths) {
        String buildNum = null;
        // 先从 t_build 表查 buildNum, 因为后面还需要使用这个 buildEntitys, 可以最大化复用
        List<BuildEntity> buildEntitys = buildDao.findByBuildId(buildId);
        if (CollectionUtils.isNotEmpty(buildEntitys)) {
            // 直接取第一个 entity 的 build number 即可
            buildNum = buildEntitys.get(0).getBuildNo();
        }
        // 查不到再从 t_task_log 查
        if (buildNum == null) {
            TaskLogEntity taskLogEntity =
                    taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
            if (taskLogEntity != null) {
                buildNum = taskLogEntity.getBuildNum();
            }
        }

        // 获取白名单
        // 在新的白名单处理逻辑中, t_build 记录 task id 和 whitePaths
        Optional<BuildEntity> buildEntity = buildEntitys.stream()
                .filter(it -> it.getTaskId().equals(taskId))
                .findFirst();
        if (buildEntity.isPresent() && buildEntity.get().getWhitePaths() != null) {
            whitePaths.addAll(buildEntity.get().getWhitePaths());
        } else if (taskDetailVO != null && taskDetailVO.getWhitePaths() != null) {
            // 向后兼容. 这是以前获取白名单的方式
            whitePaths.addAll(taskDetailVO.getWhitePaths());
        }

        return buildNum;
    }

    @Override
    public Set<String> getWhitePaths(String buildId, TaskDetailVO taskVO) {
        // 新的白名单方案是从 t_build 中获取
        Set<String> whitePaths = buildDao.getWhitePathsByTaskIdAndBuildId(taskVO.getTaskId(), buildId);
        if (whitePaths == null) {
            whitePaths = new HashSet<>();
            // 向后兼容, 以前是直接使用 t_task_detail 中的白名单信息
            if (CollectionUtils.isNotEmpty(taskVO.getWhitePaths())) {
                whitePaths.addAll(taskVO.getWhitePaths());
            }
        }

        return whitePaths;
    }

    private List<CheckerSetsVersionInfoEntity> getCheckerSetsVersionByTaskId(long taskId) {
        List<CheckerSetVO> taskCheckerSetList = checkerSetQueryBizService.getCheckerSetsByTaskId(taskId);
        List<CheckerSetsVersionInfoEntity> checkerSetsVersionInfoEntityList =
                new ArrayList<>(Collections.emptyList());
        if (CollectionUtils.isNotEmpty(taskCheckerSetList)) {
            for (CheckerSetVO checkerSetVO : taskCheckerSetList) {
                CheckerSetsVersionInfoEntity checkerSetsVersionInfoEntity = new CheckerSetsVersionInfoEntity();
                BeanUtils.copyProperties(checkerSetVO, checkerSetsVersionInfoEntity);
                checkerSetsVersionInfoEntityList.add(checkerSetsVersionInfoEntity);
            }
        }
        return checkerSetsVersionInfoEntityList.isEmpty() ? Collections.emptyList() : checkerSetsVersionInfoEntityList;
    }
}
