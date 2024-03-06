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
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.service.impl.redline.LintRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.LintDefectStatisticServiceImpl;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.service.BaseDataCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class LintFastIncrementConsumer extends AbstractFastIncrementConsumer {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private LintDefectStatisticServiceImpl lintDefectStatisticServiceImpl;
    @Autowired
    private LintRedLineReportServiceImpl lintRedLineReportServiceImpl;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private BaseDataCacheService baseDataCacheService;
    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        String baseBuildId = toolBuildInfoService.getBaseBuildIdWhenDefectCommit(
                toolBuildStackEntity, taskId,
                toolName, buildId,true
        );
        List<LintDefectV2Entity> allNewDefectList;
        List<LintDefectV2Entity> allIgnoreDefectList;

        if (StringUtils.isNotEmpty(baseBuildId)) {
            // 若快照告警为空，则说明是首次使用"新版本"构建；也有可能是真的代码写得好，没有告警
            Pair<List<LintDefectV2Entity>,List<LintDefectV2Entity>> defectListFromSnapshot =
                    getDefectListFromSnapshot(taskId, toolName, baseBuildId);
            boolean isOlder = false;

            allNewDefectList = defectListFromSnapshot.getFirst();
            allIgnoreDefectList = defectListFromSnapshot.getSecond();

            // 兼容过渡：新版快照表没有数据，则从老快照查找，但可能会出现一些分支信息脏数据
            if (CollectionUtils.isEmpty(allNewDefectList)) {
                allNewDefectList = getDefectListFromOlderSnapshot(taskId, toolName, baseBuildId);
                isOlder = true;
            }
            log.info("lint fast incr, all new defect from snapshot, older {}, task id {}, tool name {}, "
                            + "base build id {}, new list size {}, ignore list size {}",
                    isOlder, taskId, toolName, baseBuildId, allNewDefectList.size(), allIgnoreDefectList.size());
        } else {
            /*
             * 这里不能直接从告警表取数据，可能会引入并发脏数据
             * 场景：#1 master、#2 dev、#3 master
             * 假定#2工具侧扫描上报数据延迟，导致写入仓库信息延迟
             * 当#3开启时，判定最后一次仓库没变化，进而走超快增量，开始生成相关快照或者报表数据；而这一刻告警表有可能已经被dev的告警上报所覆盖
             */
            allNewDefectList = Lists.newArrayList();
            allIgnoreDefectList = Lists.newArrayList();
            log.error("lint fast incr, can not match base build id, task id {}, tool name {}, build id {}",
                    taskId, toolName, buildId);
        }

        // 更新构建告警快照
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        if (null == buildEntity) {
            buildEntity = new BuildEntity();
        }
        log.info("lint fast incr, save build defect, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
        buildSnapshotService.saveLintBuildDefect(taskId, toolName, buildEntity, allNewDefectList, allIgnoreDefectList);

        log.info("lint fast incr, statistic, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
        // 统计本次扫描的告警
        lintDefectStatisticServiceImpl.statistic(
                new DefectStatisticModel<>(
                        taskVO,
                        toolName,
                        0,
                        buildId,
                        toolBuildStackEntity,
                        allNewDefectList,
                        null,
                        null,
                        Lists.newArrayList(),
                        true
                )
        );

        log.info("lint fast incr, red line, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
        // 保存质量红线数据
        RedLineExtraParams<LintDefectV2Entity> redLineExtraParams = new RedLineExtraParams<>(allIgnoreDefectList);
        lintRedLineReportServiceImpl.saveRedLineData(taskVO, toolName, buildId, allNewDefectList, redLineExtraParams);
    }

    /**
     * 从快照表获取告警
     *
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @return
     */
    private Pair<List<LintDefectV2Entity>, List<LintDefectV2Entity>> getDefectListFromSnapshot(
            Long taskId,
            String toolName,
            String baseBuildId
    ) {
        List<BuildDefectV2Entity> buildDefectList =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndToolName(taskId, baseBuildId, toolName);

        if (CollectionUtils.isEmpty(buildDefectList)) {
            return Pair.of(Lists.newArrayList(), Lists.newArrayList());
        }

        Set<String> defectIdSet = buildDefectList.stream()
                .map(BuildDefectV2Entity::getDefectId)
                .collect(Collectors.toSet());
        Map<String, BuildDefectV2Entity> buildDefectMap = buildDefectList.stream()
                .collect(Collectors.toMap(BuildDefectV2Entity::getDefectId, Function.identity(), (k1, k2) -> k1));
        buildDefectList.clear();
        // NOCC:DLS-DEAD-LOCAL-STORE-OF-NULL(设计如此:)
        buildDefectList = null;

        Pair<List<LintDefectV2Entity>, List<LintDefectV2Entity>> lintDefectList =
                getNewAndIgnoreDefectList(taskId, toolName, defectIdSet);
        List<LintDefectV2Entity> newLintDefectList = lintDefectList.getFirst();
        defectIdSet.clear();
        // NOCC:DLS-DEAD-LOCAL-STORE-OF-NULL(设计如此:)
        defectIdSet = null;

        for (LintDefectV2Entity defect : newLintDefectList) {
            BuildDefectV2Entity buildDefect = buildDefectMap.get(defect.getEntityId());
            // 在快照表的告警，均为当时"未修复"
            defect.setStatus(DefectStatus.NEW.value());
            defect.setRevision(buildDefect.getRevision());
            defect.setBranch(buildDefect.getBranch());
            defect.setSubModule(buildDefect.getSubModule());
            defect.setLineNum(buildDefect.getLineNum());
        }

        List<LintDefectV2Entity> ignoreLintDefectList = lintDefectList.getSecond();
        for (LintDefectV2Entity defect : ignoreLintDefectList) {
            BuildDefectV2Entity buildDefect = buildDefectMap.get(defect.getEntityId());
            defect.setRevision(buildDefect.getRevision());
            defect.setBranch(buildDefect.getBranch());
            defect.setSubModule(buildDefect.getSubModule());
            defect.setLineNum(buildDefect.getLineNum());
        }

        return Pair.of(newLintDefectList,ignoreLintDefectList);
    }

    /**
     * 从"老版本"快照表获取告警
     *
     * @param taskId
     * @param toolName
     * @param baseBuildId
     * @return
     */
    private List<LintDefectV2Entity> getDefectListFromOlderSnapshot(Long taskId, String toolName, String baseBuildId) {
        List<BuildDefectEntity> buildDefectEntityList =
                buildDefectRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);

        if (CollectionUtils.isEmpty(buildDefectEntityList)) {
            return Lists.newArrayList();
        }

        Set<String> defectIdSet = buildDefectEntityList.stream()
                .map(BuildDefectEntity::getFileDefectIds)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        buildDefectEntityList.clear();
        // NOCC:DLS-DEAD-LOCAL-STORE-OF-NULL(设计如此:)
        buildDefectEntityList = null;

        List<LintDefectV2Entity> allNewDefectList = getAllNewDefectList(taskId, toolName, defectIdSet);

        return allNewDefectList;
    }

    private Pair<List<LintDefectV2Entity>,
            List<LintDefectV2Entity>> getNewAndIgnoreDefectList(Long taskId, String toolName,
                                                                Set<String> lastSnapshotDefectIdSet) {
        /*
         * 1、250W告警执行in(_id)：Document size of 51416720 is larger than maximum of 16793600
         *    分页处理60W告警为一页
         * 2、实测task_id && tool_name && status 比 in(_id) 性能高30%+
         *    先执行前者，后者进行补漏
         */
        if (CollectionUtils.isEmpty(lastSnapshotDefectIdSet)) {
            return Pair.of(Lists.newArrayList(),Lists.newArrayList());
        }
        log.info("lint fast incr, getAllNewDefectList start: {}, {}, id size: {}",
                taskId, toolName, lastSnapshotDefectIdSet.size());
        List<LintDefectV2Entity> allNewDefectList =
                Lists.newArrayListWithExpectedSize(lastSnapshotDefectIdSet.size());
        List<LintDefectV2Entity> allIgnoreDefectList =
                Lists.newArrayListWithExpectedSize(lastSnapshotDefectIdSet.size());

        /*
         * 新告警处理
         */
        long beginTime = System.currentTimeMillis();
        List<LintDefectV2Entity> byStatusDefectList = lintDefectV2Repository
                .findFastIncrFieldByTaskIdAndToolNameAndStatus(taskId, toolName, DefectStatus.NEW.value());
        log.info("lint fast incr, new defect query by task and tool and status cost: {}, {}, {}",
                taskId, toolName,
                System.currentTimeMillis() - beginTime);

        for (LintDefectV2Entity defect : byStatusDefectList) {
            if (lastSnapshotDefectIdSet.size() == 0) {
                break;
            }

            if (lastSnapshotDefectIdSet.contains(defect.getEntityId())) {
                allNewDefectList.add(defect);
                lastSnapshotDefectIdSet.remove(defect.getEntityId());
            }
        }

        byStatusDefectList.clear();
        byStatusDefectList = null;

        if (lastSnapshotDefectIdSet.size() == 0) {
            return Pair.of(allNewDefectList, Lists.newArrayList());
        }

        /*
         * 忽略告警处理
         */
        beginTime = System.currentTimeMillis();
        // 需要过滤忽略类型变更的告警，如果已经不是存量告警，就不再统计
        Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
        byStatusDefectList = lintDefectV2Dao.findAllIgnoreDefectForSnapshot(taskId, toolName, historyIgnoreType);
        log.info("lint fast incr, ignore defect query by task and tool and status cost: {}, {}, {}",
                taskId, toolName,
                System.currentTimeMillis() - beginTime);

        for (LintDefectV2Entity defect : byStatusDefectList) {
            if (lastSnapshotDefectIdSet.size() == 0) {
                break;
            }

            if (lastSnapshotDefectIdSet.contains(defect.getEntityId())) {
                allIgnoreDefectList.add(defect);
                lastSnapshotDefectIdSet.remove(defect.getEntityId());
            }
        }

        byStatusDefectList.clear();
        byStatusDefectList = null;

        if (lastSnapshotDefectIdSet.size() == 0) {
            return Pair.of(allNewDefectList, allIgnoreDefectList);
        }

        int pageSize = 60_0000;
        List<List<String>> idPartitions = Lists.partition(Lists.newArrayList(lastSnapshotDefectIdSet), pageSize);
        beginTime = System.currentTimeMillis();

        for (List<String> idPartition : idPartitions) {
            List<LintDefectV2Entity> loopTempList =
                    lintDefectV2Repository.findFastIncrFieldByEntityIdIn(idPartition);
            // 已屏蔽状态是分支间共享的，需要剔除
            allNewDefectList.addAll(loopTempList.stream()
                    .filter(x -> STATUS_NEW_FIXED_SET.contains(x.getStatus()))
                    .collect(Collectors.toList()));

            allIgnoreDefectList.addAll(loopTempList.stream()
                    .filter(x -> (x.getStatus() & DefectStatus.FIXED.value()) == 0
                            && (x.getStatus() & DefectStatus.IGNORE.value()) > 0)
                    .collect(Collectors.toList()));
        }

        log.info("lint fast incr, new and ignore partition query by entity id total cost: {}, {}, {}, {}",
                taskId, toolName, lastSnapshotDefectIdSet.size(),
                System.currentTimeMillis() - beginTime);

        return Pair.of(allNewDefectList, allIgnoreDefectList);
    }

    private List<LintDefectV2Entity> getAllNewDefectList(
            Long taskId,
            String toolName,
            Set<String> lastSnapshotDefectIdSet
    ) {
        /*
         * 1、250W告警执行in(_id)：Document size of 51416720 is larger than maximum of 16793600
         *    分页处理60W告警为一页
         * 2、实测task_id && tool_name && status 比 in(_id) 性能高30%+
         *    先执行前者，后者进行补漏
         */

        if (CollectionUtils.isEmpty(lastSnapshotDefectIdSet)) {
            return Lists.newArrayList();
        }

        log.info("lint fast incr, getAllNewDefectList start: {}, {}, id size: {}",
                taskId, toolName, lastSnapshotDefectIdSet.size());

        List<LintDefectV2Entity> allNewDefectList = Lists.newArrayListWithExpectedSize(lastSnapshotDefectIdSet.size());

        long beginTime = System.currentTimeMillis();
        List<LintDefectV2Entity> byStatusDefectList = lintDefectV2Repository
                .findFastIncrFieldByTaskIdAndToolNameAndStatus(taskId, toolName, DefectStatus.NEW.value());
        log.info("lint fast incr, getAllNewDefectList query by task and tool and status cost: {}, {}, {}",
                taskId, toolName,
                System.currentTimeMillis() - beginTime);



        for (LintDefectV2Entity defect : byStatusDefectList) {
            if (lastSnapshotDefectIdSet.size() == 0) {
                break;
            }

            if (lastSnapshotDefectIdSet.contains(defect.getEntityId())) {
                allNewDefectList.add(defect);
                lastSnapshotDefectIdSet.remove(defect.getEntityId());
            }
        }

        byStatusDefectList.clear();
        // NOCC:DLS-DEAD-LOCAL-STORE-OF-NULL(设计如此:)
        byStatusDefectList = null;

        if (lastSnapshotDefectIdSet.size() == 0) {
            return allNewDefectList;
        }

        int pageSize = 60_0000;
        List<List<String>> idPartitions = Lists.partition(Lists.newArrayList(lastSnapshotDefectIdSet), pageSize);
        beginTime = System.currentTimeMillis();

        for (List<String> idPartition : idPartitions) {
            List<LintDefectV2Entity> loopTempList =
                    lintDefectV2Repository.findFastIncrFieldByEntityIdIn(idPartition);
            // 已忽略、已屏蔽状态是分支间共享的，需要剔除
            allNewDefectList.addAll(loopTempList.stream()
                    .filter(x -> STATUS_NEW_FIXED_SET.contains(x.getStatus()))
                    .collect(Collectors.toList()));
        }

        log.info("lint fast incr, getAllNewDefectList partition query by entity id total cost: {}, {}, {}, {}",
                taskId, toolName, lastSnapshotDefectIdSet.size(),
                System.currentTimeMillis() - beginTime);

        return allNewDefectList;
    }
}
