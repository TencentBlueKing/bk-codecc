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

package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.impl.redline.CCNRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.CCNDefectStatisticServiceImpl;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CCNFastIncrementConsumer extends AbstractFastIncrementConsumer {

    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private CCNDefectStatisticServiceImpl ccnDefectStatisticService;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    private CCNRedLineReportServiceImpl ccnRedLineReportServiceImpl;
    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private BuildSnapshotService buildSnapshotService;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        if (null == buildEntity) {
            buildEntity = new BuildEntity();
        }

        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        String baseBuildId = getBaseBuildIdForFastIncr(toolBuildStackEntity, taskId, toolName, buildId);

        List<CCNDefectEntity> allNewDefectList;
        List<CCNDefectEntity> allIgnoreDefectList;

        if (StringUtils.isNotEmpty(baseBuildId)) {
            Pair<List<CCNDefectEntity>, List<CCNDefectEntity>> defectList =
                    getDefectListFromSnapshot(taskId, toolName, baseBuildId);
            allNewDefectList = defectList.getFirst();
            allIgnoreDefectList = defectList.getSecond();
            boolean isOlder = false;

            if (CollectionUtils.isEmpty(allNewDefectList)) {
                allNewDefectList = getDefectListFromOlderSnapshot(taskId, toolName, baseBuildId);
                isOlder = true;
            }

            log.info("ccn fast increment, all new defect from snapshot, older {}, task id {}, tool name {}, "
                            + "base build id {}, new list size {}, ignore list size: {}",
                    isOlder, taskId, toolName, baseBuildId, allNewDefectList.size(), allIgnoreDefectList.size());
        } else {
            allNewDefectList = Lists.newArrayList();
            allIgnoreDefectList = Lists.newArrayList();
        }

        // 因为代码没有变更，默认总平均圈复杂度不变，所以直接取上一个分析的平均圈复杂度
        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskVO.getTaskId(),
                baseBuildId);

        float averageCCN = 0;
        if (baseBuildCcnStatistic != null) {
            averageCCN = baseBuildCcnStatistic.getAverageCCN();
        }

        // 统计本次扫描的告警
        ccnDefectStatisticService.statistic(new DefectStatisticModel<>(taskVO,
                toolName,
                averageCCN,
                buildId,
                toolBuildStackEntity,
                allNewDefectList,
                null,
                null));

        // 更新构建告警快照
        buildSnapshotService.saveCCNBuildDefect(taskId, toolName, buildEntity, allNewDefectList, allIgnoreDefectList);

        // 保存质量红线数据
        RedLineExtraParams<CCNDefectEntity> redLineExtraParams = new RedLineExtraParams<>(allIgnoreDefectList);
        ccnRedLineReportServiceImpl.saveRedLineData(taskVO, ComConstants.Tool.CCN.name(), buildId, allNewDefectList,
                redLineExtraParams);
    }

    /**
     * 从快照表获取告警
     *
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @return
     */
    private Pair<List<CCNDefectEntity>, List<CCNDefectEntity>> getDefectListFromSnapshot(Long taskId, String toolName,
            String baseBuildId) {

        List<BuildDefectV2Entity> buildDefectList =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndToolName(taskId, baseBuildId, toolName);

        if (CollectionUtils.isEmpty(buildDefectList)) {
            return Pair.of(Lists.newArrayList(),Lists.newArrayList());
        }

        Set<String> defectIdSet = buildDefectList.stream()
                .map(BuildDefectV2Entity::getDefectId)
                .collect(Collectors.toSet());

        // 已屏蔽状态是分支间共享的，需要剔除
        List<CCNDefectEntity> ccnDefectList =
                ccnDefectRepository.findByEntityIdInAndStatusIn(defectIdSet, STATUS_NEW_FIXED_SET);
        Map<String, BuildDefectV2Entity> buildDefectMap = buildDefectList.stream()
                .collect(Collectors.toMap(BuildDefectV2Entity::getDefectId, Function.identity(), (k1, k2) -> k1));

        for (CCNDefectEntity defect : ccnDefectList) {
            BuildDefectV2Entity buildDefect = buildDefectMap.get(defect.getEntityId());
            // 在快照表的告警，均为当时"未修复"
            defect.setStatus(DefectStatus.NEW.value());
            defect.setRevision(buildDefect.getRevision());
            defect.setBranch(buildDefect.getBranch());
            defect.setSubModule(buildDefect.getSubModule());
            defect.setStartLines(buildDefect.getStartLines());
            defect.setStartLines(buildDefect.getStartLines());
            defect.setEndLines(buildDefect.getEndLines());
        }

        List<CCNDefectEntity> ccnIgnoreDefectList = ccnDefectDao.findAllIgnoreDefectForSnapshot(defectIdSet);
        for (CCNDefectEntity defect : ccnIgnoreDefectList) {
            BuildDefectV2Entity buildDefect = buildDefectMap.get(defect.getEntityId());
            defect.setRevision(buildDefect.getRevision());
            defect.setBranch(buildDefect.getBranch());
            defect.setSubModule(buildDefect.getSubModule());
            defect.setStartLines(buildDefect.getStartLines());
            defect.setStartLines(buildDefect.getStartLines());
            defect.setEndLines(buildDefect.getEndLines());
        }


        return Pair.of(ccnDefectList, ccnIgnoreDefectList);
    }

    /**
     * 从"老版本"快照表获取告警
     *
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @return
     */
    private List<CCNDefectEntity> getDefectListFromOlderSnapshot(Long taskId, String toolName, String baseBuildId) {
        List<BuildDefectEntity> buildDefectEntityList =
                buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);

        if (CollectionUtils.isEmpty(buildDefectEntityList)) {
            return Lists.newArrayList();
        }

        Set<String> defectIdSet = buildDefectEntityList.stream()
                .map(BuildDefectEntity::getDefectId)
                .collect(Collectors.toSet());

        // 已忽略、已屏蔽状态是分支间共享的，需要剔除
        List<CCNDefectEntity> allNewDefectList =
                ccnDefectRepository.findByEntityIdInAndStatusIn(defectIdSet, STATUS_NEW_FIXED_SET);

        return allNewDefectList;
    }
}
