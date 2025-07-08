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

package com.tencent.bk.codecc.defect.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.component.RiskConfigCache;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DUPCDefectDao;
import com.tencent.bk.codecc.defect.model.CodeBlockEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.DefectFilePathClusterService;
import com.tencent.bk.codecc.defect.service.impl.DUPCFilterPathBizServiceImpl;
import com.tencent.bk.codecc.defect.service.impl.redline.DupcRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.DupcDefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.BsonSerializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("dupcDefectCommitConsumer")
@Slf4j
public class DUPCDefectCommitConsumer extends AbstractDefectCommitConsumer {
    @Autowired
    private RiskConfigCache riskConfigCache;
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;
    @Autowired
    private DUPCDefectDao dupcDefectDao;
    @Autowired
    private DUPCFilterPathBizServiceImpl dupcFilterPathBizService;
    @Autowired
    private DupcRedLineReportServiceImpl dupcRedLineReportServiceImpl;
    @Autowired
    private DupcDefectStatisticServiceImpl dupcDefectStatisticService;
    @Autowired
    private DefectFilePathClusterService defectFilePathClusterService;
    @Autowired
    private Client client;



    @Override
    protected boolean uploadDefects(
            CommitDefectVO commitDefectVO,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap
    ) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(commitDefectVO.getStreamName());
        String createFrom = taskVO.getCreateFrom();

        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity = JsonUtil.INSTANCE.to(
                defectListJson, new TypeReference<DUPCDefectJsonFileEntity<DUPCDefectEntity>>() {
                }
        );

        //获取风险系数值
        Map<String, String> riskConfigMap = riskConfigCache.getRiskConfig(ComConstants.Tool.DUPC.name());
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        long tryBeginTime = System.currentTimeMillis();
        RedisLock locker = null;
        List<DUPCDefectEntity> allNewDefectList;

        try {
            // 非工蜂项目上锁提单过程
            if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom) && !lockModeIsClosed()) {
                // 上线时，MQ可能有未消费完消息，需兼容
                if (commitDefectVO.getDefectFileSize() == null) {
                    long fileSize = scmJsonComponent.getDefectFileSize(streamName, toolName, buildId);
                    commitDefectVO.setDefectFileSize(fileSize);
                }

                Pair<Boolean, RedisLock> pair = continueWithLock(commitDefectVO);
                Boolean beContinue = pair.getFirst();
                locker = pair.getSecond();

                if (!beContinue) {
                    return false;
                }
            }

            // 检查是否超过了重试次数（5），达到限制会报错
            checkIfReachRetryLimit(commitDefectVO);

            List<DUPCDefectEntity> oldDefectList = dupcDefectRepository.findByTaskIdWithoutBlockList(taskId);
            Map<String, DUPCDefectEntity> oldDefectMap;
            if (CollectionUtils.isNotEmpty(oldDefectList)) {
                oldDefectMap = oldDefectList.stream()
                        .collect(Collectors.toMap(
                                defect -> StringUtils.isEmpty(defect.getRelPath()) ? defect.getFilePath()
                                        : defect.getRelPath(), Function.identity(), (k, v) -> v));
            } else {
                oldDefectMap = new HashMap<>();
            }

            long curTime = System.currentTimeMillis();

            List<DUPCDefectEntity> defectList = defectJsonFileEntity.getDefects();
            if (CollectionUtils.isNotEmpty(defectList)) {
                Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, toolName);
                Set<String> whitePaths = buildService.getWhitePaths(buildId, taskVO);

                Iterator<DUPCDefectEntity> iterator = defectList.iterator();
                while (iterator.hasNext()) {
                    DUPCDefectEntity dupcDefectEntity = iterator.next();
                    // 填充告警信息
                    fillDefectInfo(dupcDefectEntity, commitDefectVO, curTime, fileChangeRecordsMap, codeRepoIdMap);

                    // 更新告警状态
                    updateDefectStatus(dupcDefectEntity, oldDefectMap, filterPaths, whitePaths, curTime, m);
                }
            }



            try {
                //限制BLOCK_LIST大小
                if (CollectionUtils.isNotEmpty(defectList)) {
                    for (DUPCDefectEntity entity : defectList) {
                        if (entity != null && CollectionUtils.isNotEmpty(entity.getBlockList())
                                && entity.getBlockList().size() > ComConstants.DUPC_DEFECT_BLOCK_LIST_LIMIT) {
                            entity.setBlockList(
                                    entity.getBlockList().subList(0, ComConstants.DUPC_DEFECT_BLOCK_LIST_LIMIT)
                            );
                        }
                    }
                }
                //保存
                dupcDefectDao.upsertDupcDefect(defectList);
            } catch (BsonSerializationException e) {
                log.info("dupc defect size larger than 16M! task id: {}", commitDefectVO.getTaskId());
            }

            // 余下的是本次没有上报的，需要标志为已修复
            List<DUPCDefectEntity> fixDefectList = Lists.newArrayList();
            if (oldDefectMap.size() > 0) {
                oldDefectMap.values().forEach(oldDefect ->
                {
                    if (ComConstants.DefectStatus.NEW.value() == oldDefect.getStatus()) {
                        oldDefect.setStatus(oldDefect.getStatus() | ComConstants.DefectStatus.FIXED.value());
                        oldDefect.setFixedTime(curTime);
                        fixDefectList.add(oldDefect);
                    }
                });
            }
            dupcDefectDao.batchFixDefect(taskId, fixDefectList);

            // 保存已修复与待修复的告警路径
            List<DUPCDefectEntity> dupcDefectEntities = Lists.newArrayListWithCapacity(
                    defectList.size() + fixDefectList.size());
            dupcDefectEntities.addAll(defectList);
            dupcDefectEntities.addAll(fixDefectList);
            saveDefectPath(taskId, buildId, dupcDefectEntities);

            // 保存本次上报文件的告警数据统计数据
            dupcDefectStatisticService.statistic(
                    new DefectStatisticModel<>(
                            taskVO,
                            toolName,
                            0,
                            buildId,
                            toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId),
                            defectList,
                            riskConfigMap,
                            defectJsonFileEntity,
                            false
                    )
            );

            allNewDefectList = dupcDefectRepository.getByTaskIdAndStatus(taskId, DefectStatus.NEW.value());
        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }

            log.info("defect commit, lock try to finally cost total: {}, {}, {}, {}",
                    System.currentTimeMillis() - tryBeginTime, taskId, toolName, buildId);
        }

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 保存质量红线数据
        dupcRedLineReportServiceImpl.saveRedLineData(taskVO, toolName, buildId, allNewDefectList);

        return true;
    }

    /**
     * 更新告警状态
     *
     * @param dupcDefectEntity
     * @param oldDefectMap
     * @param filterPaths
     * @param curTime
     * @param m
     */
    private void updateDefectStatus(DUPCDefectEntity dupcDefectEntity,
            Map<String, DUPCDefectEntity> oldDefectMap,
            Set<String> filterPaths,
            Set<String> whitePaths,
            long curTime,
            float m) {
        String path = StringUtils.isEmpty(dupcDefectEntity.getRelPath()) ? dupcDefectEntity.getFilePath()
                : dupcDefectEntity.getRelPath();
        DUPCDefectEntity oldDefect = oldDefectMap.get(path);
        // 已经存在的风险文件
        if (oldDefect != null) {
            Integer oldStatus = oldDefect.getStatus();

            // 如果风险文件状态是new且风险系数低于基线，将文件置为已修复
            if (oldStatus == ComConstants.DefectStatus.NEW.value() && dupcDefectEntity.getDupRateValue() < m) {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            }
            // 如果风险文件状态是closed且风险系数高于基线，将文件置为new
            else if ((oldStatus & ComConstants.DefectStatus.FIXED.value()) > 0
                    && dupcDefectEntity.getDupRateValue() >= m) {
                dupcDefectEntity.setStatus(oldStatus - ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(null);
            } else {
                dupcDefectEntity.setStatus(oldStatus);
            }
            //获取创建时间
            Long createTime = oldDefect.getCreateTime() == null ? Long.valueOf(System.currentTimeMillis())
                    : oldDefect.getCreateTime();
            dupcDefectEntity.setCreateTime(createTime);
            dupcDefectEntity.setEntityId(oldDefect.getEntityId());

            // 余下的是本次没有上报的，需要标志为已修复
            oldDefectMap.remove(path);
        }
        // 新增的风险文件
        else {
            dupcDefectEntity.setCreateTime(curTime);
            if (dupcDefectEntity.getDupRateValue() < m) {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            } else {
                dupcDefectEntity.setStatus(ComConstants.DefectStatus.NEW.value());
            }
        }

        // 检查是否被路径屏蔽
        dupcFilterPathBizService.processBiz(
                new FilterPathInputVO(whitePaths, filterPaths, dupcDefectEntity, curTime));
    }

    private void fillDefectInfo(DUPCDefectEntity dupcDefectEntity,
            CommitDefectVO commitDefectVO,
            long curTime,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        //设置基础信息
        dupcDefectEntity.setTaskId(commitDefectVO.getTaskId());
        dupcDefectEntity.setToolName(commitDefectVO.getToolName());
        String dupRateStr = dupcDefectEntity.getDupRate();
        float dupRate = Float.valueOf(
                StringUtils.isEmpty(dupRateStr) ? "0" : dupRateStr.substring(0, dupRateStr.length() - 1));
        dupcDefectEntity.setDupRateValue(dupRate);
        dupcDefectEntity.setLastUpdateTime(curTime);

        //设置相应文件路径及代码库信息
        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(dupcDefectEntity.getFilePath());
        if (fileLineAuthorInfo != null) {
            dupcDefectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
            dupcDefectEntity.setUrl(fileLineAuthorInfo.getUrl());
            dupcDefectEntity.setFileChangeTime(fileLineAuthorInfo.getFileUpdateTime());
            dupcDefectEntity.setRevision(fileLineAuthorInfo.getRevision());
            dupcDefectEntity.setBranch(fileLineAuthorInfo.getBranch());
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType())) {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                if (null != repoSubModuleVO) {
                    dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                }
            } else {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                if (null != repoSubModuleVO) {
                    dupcDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                        dupcDefectEntity.setSubModule(repoSubModuleVO.getSubModule());
                    }
                }

            }

            // 设置作者信息
            setAuthor(dupcDefectEntity, fileLineAuthorInfo);
        }

        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList)) {
            blockList.forEach(block -> block.setSourceFile(dupcDefectEntity.getFilePath()));
        }

    }

    private void setAuthor(DUPCDefectEntity dupcDefectEntity, ScmBlameVO fileLineAuthorInfo) {
        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList) && CollectionUtils.isNotEmpty(changeRecords)) {
            // 获取各文件代码行对应的作者信息映射
            Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = getLineAuthorMap(changeRecords);
            blockList.forEach(codeBlockEntity ->
            {
                long functionLastUpdateTime = 0L;
                for (long i = codeBlockEntity.getStartLines(); i <= codeBlockEntity.getEndLines(); i++) {
                    ScmBlameChangeRecordVO recordVO = lineAuthorMap.get(Integer.valueOf(String.valueOf(i)));
                    if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime) {
                        functionLastUpdateTime = recordVO.getLineUpdateTime();
                        codeBlockEntity.setAuthor(ToolParamUtils.trimUserName(recordVO.getAuthor()));
                        codeBlockEntity.setLatestDatetime(recordVO.getLineUpdateTime());
                    }
                }
            });
            //设置作者清单
            dupcDefectEntity.setAuthorList(blockList.stream().map(CodeBlockEntity::getAuthor).
                    filter(StringUtils::isNotBlank).distinct().reduce((o1, o2) -> String.format("%s;%s", o1, o2))
                    .orElse(""));
        }
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.DUPC.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.DUPC.name().toLowerCase(Locale.ENGLISH);
    }

    /**
     * 保存告警路径
     * @param taskId
     * @param buildId
     * @param upsertList
     */
    private void saveDefectPath(Long taskId, String buildId, List<DUPCDefectEntity> upsertList) {
        if (taskId == null || StringUtils.isBlank(buildId) || CollectionUtils.isEmpty(upsertList)) {
            return;
        }
        try {
            TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
            if (taskInfo == null || BooleanUtils.isFalse(taskInfo.getFileCacheEnable())) {
                return;
            }

            Set<String> repeat = new HashSet<>();
            List<Pair<String, String>> newDefectPaths = new ArrayList<>();
            for (DUPCDefectEntity dupcDefectEntity : upsertList) {
                if (dupcDefectEntity.getStatus() != DefectStatus.NEW.value()
                        || repeat.contains(dupcDefectEntity.getFilePath())) {
                    continue;
                }
                newDefectPaths.add(Pair.of(dupcDefectEntity.getFilePath(),
                        StringUtils.isBlank(dupcDefectEntity.getRelPath()) ? "" : dupcDefectEntity.getRelPath()));
                repeat.add(dupcDefectEntity.getFilePath());
            }
            if (CollectionUtils.isNotEmpty(newDefectPaths)) {
                defectFilePathClusterService.saveBuildDefectFilePath(taskId, Tool.DUPC.name(), buildId,
                        DefectStatus.NEW, newDefectPaths);
            }

            repeat.clear();
            List<Pair<String, String>> fixedDefectPaths = new ArrayList<>();
            for (DUPCDefectEntity dupcDefectEntity : upsertList) {
                if ((dupcDefectEntity.getStatus() & DefectStatus.FIXED.value()) == 0
                        || repeat.contains(dupcDefectEntity.getFilePath())) {
                    continue;
                }
                newDefectPaths.add(Pair.of(dupcDefectEntity.getFilePath(),
                        StringUtils.isBlank(dupcDefectEntity.getRelPath()) ? "" : dupcDefectEntity.getRelPath()));
                repeat.add(dupcDefectEntity.getFilePath());
            }
            if (CollectionUtils.isNotEmpty(newDefectPaths)) {
                defectFilePathClusterService.saveBuildDefectFilePath(taskId, Tool.DUPC.name(), buildId,
                        DefectStatus.FIXED, fixedDefectPaths);
            }
        } catch (Exception e) {
            log.error("saveDefectPaths error, taskId:" + taskId + ", toolName: DUPC, buildId:" + buildId, e);
        }
    }
}
