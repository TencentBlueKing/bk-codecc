/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.codeccjob.consumer;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SMOKE_CHECK;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SMOKE_TRIGGER_ANALYZE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_SMOKE_CHECK;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SMOKE_CHECK;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SMOKE_TRIGGER_ANALYZE;

import com.tencent.bk.codecc.codeccjob.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.SmokeCheckDetailRepository;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.SmokeCheckLogRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.TaskLogOverviewDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.api.ServiceToolBuildInfoResource;
import com.tencent.bk.codecc.defect.dto.SmokeCheckDTO;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.admin.SmokeCheckDetailEntity;
import com.tencent.bk.codecc.defect.model.admin.SmokeCheckLogEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.bk.codecc.defect.vo.admin.SmokeParam;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 冒烟检查消费者
 *
 * @version V1.0
 * @date 2021/6/1
 */

@Slf4j
@Component
public class SmokeCheckConsumer {

    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private Client client;
    @Autowired
    private SmokeCheckLogRepository smokeCheckLogRepository;
    @Autowired
    private SmokeCheckDetailRepository smokeCheckDetailRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TaskLogDao taskLogDao;
    @Autowired
    private TaskLogOverviewDao taskLogOverviewDao;
    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;


    /**
     * 冒烟检查统计队列消费者
     *
     * @param smokeCheckDTOStr 入参
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_SMOKE_CHECK,
            value = @Queue(value = QUEUE_SMOKE_CHECK),
            exchange = @Exchange(value = EXCHANGE_SMOKE_CHECK, durable = "false")))
    public void doSmoke(String smokeCheckDTOStr) {
        SmokeCheckDTO smokeCheckDTO = JsonUtil.INSTANCE.to(smokeCheckDTOStr, SmokeCheckDTO.class);

        String toolName = smokeCheckDTO.getToolName();
        if (StringUtils.isBlank(toolName) || StringUtils.isBlank(smokeCheckDTO.getBelongToId())
                || CollectionUtils.isEmpty(smokeCheckDTO.getTaskIdSet())) {
            log.error("doSmoke abort! smokeCheckDTO is not available: \n{}", smokeCheckDTO);
            return;
        }
        log.info("received doSmoke SmokeCheckDTO: toolName {}, belongToId {}, taskIdSet size:{}", toolName,
                smokeCheckDTO.getBelongToId(), smokeCheckDTO.getTaskIdSet().size());

        try {
            String toolPattern = toolMetaCache.getToolPattern(toolName);
            List<DefectCountModel> defectCountModels;

            Set<Long> taskIdSet = smokeCheckDTO.getTaskIdSet();

            Set<String> buildIdSet = null;
            // toolName为SCC时 查询最新构建id
            if (ToolPattern.CLOC.name().equals(toolPattern)) {
                List<ToolBuildInfoEntity> latestBuildIdEntities =
                        toolBuildInfoDao.findLatestBuildIdByTaskIdSet(taskIdSet);
                if (CollectionUtils.isNotEmpty(latestBuildIdEntities)) {
                    buildIdSet = latestBuildIdEntities.stream().map(ToolBuildInfoEntity::getDefectBaseBuildId)
                            .collect(Collectors.toSet());
                }
            }

            Set<SmokeParam> smokeParamSet = smokeCheckDTO.getSmokeParamSet();
            for (SmokeParam smokeParam : smokeParamSet) {
                int skip = smokeParam.getSkip();
                int size = smokeParam.getSize();

                if (ToolPattern.LINT.name().equals(toolPattern)) {
                    defectCountModels = lintDefectV2Dao.statisticLintDefect(taskIdSet, toolName, skip, size);
                } else if (ToolPattern.CCN.name().equals(toolPattern)) {
                    defectCountModels = ccnDefectDao.statisticCCNDefect(taskIdSet, skip, size);
                } else if (ToolPattern.COVERITY.name().equals(toolPattern)
                        || ToolPattern.KLOCWORK.name().equals(toolPattern)
                        || ToolPattern.PINPOINT.name().equals(toolPattern)) {
                    defectCountModels = defectDao.statisticCommonDefect(taskIdSet, toolName, skip, size);
                } else if (ToolPattern.CLOC.name().equals(toolPattern)) {
                    defectCountModels =
                            clocStatisticsDao.statistiSCCDefect(taskIdSet, buildIdSet, toolName, skip, size);
                } else {
                    log.warn("the tool does not support doSmoke: {}", toolName);
                    continue;
                }

                if (CollectionUtils.isEmpty(defectCountModels)) {
                    log.warn("DefectCountModelList is empty! toolName: {}", toolName);
                    continue;
                }
                List<Long> taskIds =
                        defectCountModels.stream().map(DefectCountModel::getTaskId).collect(Collectors.toList());

                List<TaskLogEntity> taskLogList =
                        taskLogDao.findElapseTimeByTaskIdInAndToolName(taskIds, toolName);

                Map<Long, Long> taskLogMap = taskLogList.stream()
                        .collect(Collectors.toMap(TaskLogEntity::getTaskId,
                                TaskLogEntity::getElapseTime, (k, v) -> v));

                // 保存冒烟前的任务告警数及绑定所属的冒烟id
                saveTaskIdDefectAndBelongToId(toolName, smokeCheckDTO.getBelongToId(), defectCountModels, taskLogMap);

                // 设置强制全量扫描
                QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
                reqVO.setTaskIds(taskIdSet);
                reqVO.setToolName(toolName);
                Result<Boolean> result = client.get(ServiceToolBuildInfoResource.class).batchSetForceFullScan(reqVO);
                if (result.isNotOk() || result.getData() == null || !result.getData()) {
                    log.error("SmokeCheck abort! batchSetForceFullScan fail! result:{}", result);
                    return;
                }

                // 触发分析
                Set<Long> triggerAnalyzeTaskIdSet =
                        defectCountModels.stream().map(DefectCountModel::getTaskId).collect(Collectors.toSet());
                rabbitTemplate.convertAndSend(EXCHANGE_SMOKE_TRIGGER_ANALYZE, ROUTE_SMOKE_TRIGGER_ANALYZE,
                        triggerAnalyzeTaskIdSet);
            }

        } catch (Exception e) {
            log.error("SmokeCheckConsumer exception: {} \n{}", e.getMessage(), e);
        }
    }

    /**
     * 更新和保存冒烟信息
     */
    private void saveTaskIdDefectAndBelongToId(String toolName, String belongToId, List<DefectCountModel> modelList,
            Map<Long, Long> taskLogMap) {
        if (CollectionUtils.isEmpty(modelList)) {
            log.warn("DefectCountModelList is empty! toolName: {}", toolName);
            return;
        }

        // 更新冒烟检查记录信息
        SmokeCheckLogEntity smokeCheckLogEntity = smokeCheckLogRepository.findFirstByEntityId(belongToId);
        int taskIdCount = smokeCheckLogEntity.getTaskIdCount();
        smokeCheckLogEntity.setTaskIdCount(taskIdCount + modelList.size());
        smokeCheckLogRepository.save(smokeCheckLogEntity);

        List<SmokeCheckDetailEntity> smokeCheckDetailEntityList = new ArrayList<>(modelList.size());
        for (DefectCountModel defectCountModel : modelList) {
            SmokeCheckDetailEntity detailEntity = new SmokeCheckDetailEntity();
            detailEntity.setBelongToId(belongToId);
            detailEntity.setToolName(toolName);
            detailEntity.setTaskId(defectCountModel.getTaskId());
            detailEntity.setBeforeDefectCount(defectCountModel.getDefectCount());
            detailEntity.setBeforeElapseTime(taskLogMap.getOrDefault(defectCountModel.getTaskId(), 0L));
            smokeCheckDetailEntityList.add(detailEntity);
        }

        smokeCheckDetailRepository.saveAll(smokeCheckDetailEntityList);
    }

}
