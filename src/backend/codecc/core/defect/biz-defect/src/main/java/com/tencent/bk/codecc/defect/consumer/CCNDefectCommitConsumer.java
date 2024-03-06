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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.NewCCNDefectTracingComponent;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.FileCCNRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.FileCCNDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CCNDefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.FileCCNEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.SummaryDefectGatherService;
import com.tencent.bk.codecc.defect.service.git.GitRepoApiService;
import com.tencent.bk.codecc.defect.service.impl.redline.CCNRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.CCNDefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ScanType;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

/**
 * CCN告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("ccnDefectCommitConsumer")
@Slf4j
public class CCNDefectCommitConsumer extends AbstractDefectCommitConsumer {

    @Autowired
    private FileCCNRepository fileCCNRepository;
    @Autowired
    private FileCCNDao fileCCNDao;
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private NewCCNDefectTracingComponent newCCNDefectTracingComponent;
    @Autowired
    private GitRepoApiService gitRepoApiService;
    @Autowired
    private CCNDefectStatisticServiceImpl ccnDefectStatisticService;
    @Autowired
    private CCNRedLineReportServiceImpl ccnRedLineReportServiceImpl;
    @Autowired
    private SummaryDefectGatherService summaryDefectGatherService;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private BaseDataCacheService baseDataCacheService;

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
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);

        // 读取原生（未经压缩o）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity = JsonUtil.INSTANCE.to(
                defectListJson, new TypeReference<CCNDefectJsonFileEntity<CCNDefectEntity>>() {
                });

        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId,
                toolName, buildId);

        // 判断本次是增量还是全量扫描
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 获取工具侧上报的已删除文件
        List<String> deleteFiles;
        if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            deleteFiles = toolBuildStackEntity.getDeleteFiles();
        } else {
            deleteFiles = Lists.newArrayList();
        }

        long beginTime = System.currentTimeMillis();
        long tryBeginTime = System.currentTimeMillis();
        RedisLock locker = null;
        List<CCNDefectEntity> allNewDefectList;
        List<CCNDefectEntity> allIgnoreDefectList;
        Set<String> currentFileSet;

        try {
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

            currentFileSet = parseDefectJsonFile(commitDefectVO, defectJsonFileEntity, taskVO,
                    buildEntity, fileChangeRecordsMap, codeRepoIdMap);
            log.info("parseDefectJsonFile cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId,
                    toolName,
                    buildId);

            // 查询所有状态为NEW的告警
            beginTime = System.currentTimeMillis();
            allNewDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
            log.info("find lint file list cost: {}, {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId,
                    toolName, buildId, allNewDefectList.size());

            // 2.更新告警状态
            beginTime = System.currentTimeMillis();
            updateDefectEntityStatus(allNewDefectList, currentFileSet, deleteFiles, isFullScan, buildEntity);
            log.info("updateDefectEntityStatus cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId,
                    toolName, buildId);

            beginTime = System.currentTimeMillis();
            Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
            allIgnoreDefectList = ccnDefectDao.findIgnoreDefect(taskId, buildId, historyIgnoreType,
                    isFilterPathPath(taskVO));
            log.info("get ignore defect list cost: {}, {}, {}, {}",
                    System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }

            log.info("defect commit, lock try to finally cost total: {}, {}, {}, {}",
                    System.currentTimeMillis() - tryBeginTime, taskId, toolName, buildId);
        }

        keepOnlyUnfixedDefect(allNewDefectList);

        // 3. 处理低于阈值缺陷
        beginTime = System.currentTimeMillis();
        summaryDefectGatherService.saveSummaryDefectGather(taskId, toolName, defectJsonFileEntity.getGather());
        if (defectJsonFileEntity.getGather() != null) {
            commitDefectVO.setMessage(GsonUtils.toJson(defectJsonFileEntity.getGather()));
            log.info("summaryDefectGather cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId,
                    toolName, buildId);
        }

        // 4.计算总平均圈复杂度
        float averageCCN = calculateAverageCCN(taskId, defectJsonFileEntity, isFullScan, deleteFiles);

        // 5.统计本次扫描的告警
        beginTime = System.currentTimeMillis();
        ccnDefectStatisticService.statistic(
                new DefectStatisticModel<>(
                        taskVO,
                        toolName,
                        averageCCN,
                        buildId,
                        toolBuildStackEntity,
                        allNewDefectList,
                        null,
                        null,
                        false
                )
        );
        log.info("statistic cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 6.更新构建告警快照
        beginTime = System.currentTimeMillis();
        // 查询存量告警类型的的已忽略告警

        buildSnapshotService.saveCCNBuildDefect(taskId, toolName, buildEntity, allNewDefectList, allIgnoreDefectList);
        log.info("buildSnapshotService.saveCCNBuildDefect cost: {}, {}, {}, {}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 7.保存质量红线数据
        beginTime = System.currentTimeMillis();
        RedLineExtraParams<CCNDefectEntity> redLineExtraParams = new RedLineExtraParams<>(allIgnoreDefectList);
        ccnRedLineReportServiceImpl.saveRedLineData(taskVO, ComConstants.Tool.CCN.name(), buildId, allNewDefectList,
                redLineExtraParams);
        log.info("redLineReportService.saveRedLineData cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime,
                taskId, toolName, buildId);

        // 8.回写工蜂mr信息
        beginTime = System.currentTimeMillis();
        gitRepoApiService.addCcnGitCodeAnalyzeComment(taskVO, buildEntity.getBuildId(), buildEntity.getBuildNo(),
                toolName, currentFileSet, allNewDefectList);
        log.info("gitRepoApiService.addCcnGitCodeAnalyzeComment cost: {}, {}, {}, {}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        return true;
    }

    /**
     * MR 等Diff模式 忽略只关注相关的文件
     * @param taskVO
     * @return
     */
    private boolean isFilterPathPath(TaskDetailVO taskVO) {
        return taskVO != null && taskVO.getScanType() != null
                && (taskVO.getScanType().equals(ScanType.DIFF_MODE.code)
                || taskVO.getScanType().equals(ScanType.FILE_DIFF_MODE.code)
                || taskVO.getScanType().equals(ScanType.BRANCH_DIFF_MODE.code));
    }

    /**
     * 更新告警状态
     *
     * @param allNewDefectList
     * @param currentFileSet
     * @param deleteFiles
     * @param isFullScan
     * @param buildEntity
     */
    private void updateDefectEntityStatus(List<CCNDefectEntity> allNewDefectList,
            Set<String> currentFileSet,
            List<String> deleteFiles,
            boolean isFullScan,
            BuildEntity buildEntity) {
        List<CCNDefectEntity> needUpdateDefectList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(allNewDefectList)) {
            log.info("allNewDefectList is empty. buildId:{}", buildEntity.getBuildId());
            return;
        }
        Map<String, CodeRepoEntity> repoIdMap = Maps.newHashMap();
        Map<String, CodeRepoEntity> urlMap = Maps.newHashMap();
        getCodeRepoMap(allNewDefectList.get(0).getTaskId(), isFullScan, buildEntity, repoIdMap, urlMap);
        long curTime = System.currentTimeMillis();
        allNewDefectList.forEach(defect -> {
            String filePath = defect.getFilePath();
            String relPath = defect.getRelPath();

            // 是否是本次上报的告警文件
            boolean notCurrentBuildUpload = CollectionUtils.isEmpty(currentFileSet)
                    || !currentFileSet.contains(StringUtils.isEmpty(relPath) ? filePath : relPath);

            /*
             * 1、文件已删除，则设置为已修复状态
             * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
             * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
             */
            if ((deleteFiles.contains(filePath)
                    || (StringUtils.isNotEmpty(relPath) && deleteFiles.stream().anyMatch(it -> it.endsWith(relPath)))
                    || (isFullScan && notCurrentBuildUpload))) {
                defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.FIXED.value());
                defect.setFixedTime(curTime);
                defect.setLatestDateTime(curTime);
                defect.setFixedBuildNumber(buildEntity.getBuildNo());
                needUpdateDefectList.add(defect);
            } else if (!isFullScan && notCurrentBuildUpload) {
                String newBranch = null;
                String url = defect.getUrl();
                String repoId = defect.getRepoId();
                if (StringUtils.isNotEmpty(url) && urlMap.get(url) != null) {
                    newBranch = urlMap.get(url).getBranch();
                } else if (StringUtils.isNotEmpty(repoId) && repoIdMap.get(repoId) != null) {
                    newBranch = repoIdMap.get(repoId).getBranch();
                }
                if (StringUtils.isNotEmpty(newBranch) && !newBranch.equals(defect.getBranch())) {
                    defect.setBranch(newBranch);
                    needUpdateDefectList.add(defect);
                }
            }
        });

        if (CollectionUtils.isNotEmpty(needUpdateDefectList)) {
            ccnDefectRepository.saveAll(needUpdateDefectList);
        }
    }

    /**
     * 解析工具上报的告警文件，并做告警跟踪
     *
     * @param commitDefectVO
     * @param defectJsonFileEntity
     * @param taskVO
     * @param buildEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     * @return
     */
    private Set<String> parseDefectJsonFile(CommitDefectVO commitDefectVO,
            CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity,
            TaskDetailVO taskVO,
            BuildEntity buildEntity,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        // 获取作者转换关系
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskId(
                commitDefectVO.getTaskId());
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null) {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        // 获取屏蔽路径
        Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, commitDefectVO.getToolName());

        List<CCNDefectEntity> currentDefectList = defectJsonFileEntity.getDefects();
        log.info("current all defect list: {}, {}", taskVO.getTaskId(), currentDefectList.size());

        // 填充告警的代码仓库信息
        currentDefectList.forEach(ccnDefectEntity ->
                fillDefectInfo(ccnDefectEntity, fileChangeRecordsMap, codeRepoIdMap));

        Map<String, List<CCNDefectEntity>> currentDefectGroup = currentDefectList.stream()
                .collect(Collectors.groupingBy(defect -> StringUtils.isEmpty(defect.getRelPath())
                        ? defect.getFilePath() : defect.getRelPath()));

        int chunkNo = 0;
        List<CCNDefectEntity> partDefectList = new ArrayList<>();
        Set<String> filePathSet = new HashSet<>();
        Set<String> relPathSet = new HashSet<>();
        List<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultList = new ArrayList<>();
        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch finishLatch = new CountDownLatch(1);
        Set<Long> taskList = concurrentDefectTracingConfigCache.getVipTaskSet();
        Boolean isVip = CollectionUtils.isNotEmpty(taskList) && taskList.contains(commitDefectVO.getTaskId());
        LinkedBlockingQueue<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultQueue =
                setConcurrentBlockingQueue(
                        commitDefectVO.getTaskId(),
                        commitDefectVO.getToolName(),
                        commitDefectVO.getBuildId(),
                        running,
                        finishLatch,
                        isVip
                );
        for (Map.Entry<String, List<CCNDefectEntity>> entry : currentDefectGroup.entrySet()) {
            String path = entry.getKey();
            List<CCNDefectEntity> defectList = entry.getValue();
            partDefectList.addAll(defectList);
            if (CollectionUtils.isNotEmpty(defectList)) {
                if (StringUtils.isNotBlank(defectList.get(0).getRelPath())) {
                    relPathSet.add(defectList.get(0).getRelPath());
                } else {
                    filePathSet.add(defectList.get(0).getFilePath());
                }
            }
            if (partDefectList.size() > MAX_PER_BATCH) {
                processDefects(commitDefectVO, partDefectList, taskVO, filePathSet, relPathSet,
                        transferAuthorList, filterPaths, buildEntity, chunkNo, asyncResultQueue,
                        asyncResultList, isVip);
                chunkNo++;
                partDefectList = new ArrayList<>();
                filePathSet = new HashSet<>();
                relPathSet = new HashSet<>();
            }
        }

        if (partDefectList.size() > 0) {
            processDefects(commitDefectVO, partDefectList, taskVO, filePathSet, relPathSet, transferAuthorList,
                    filterPaths, buildEntity, chunkNo, asyncResultQueue, asyncResultList, isVip);
        }
        // 直到所有的异步处理否都完成了，才继续往下走
        asyncResultList.forEach(asyncResult -> {
            try {
                Boolean clusterResult = asyncResult.get();
                if (null == clusterResult || !clusterResult) {
                    log.info("cluster result is not true!");
                }
            } catch (InterruptedException | ExecutionException e) {
                log.warn("handle file defect fail!{}", commitDefectVO, e);
            }
        });
        running.set(false);
        log.info("wait for count down latch to pass, {}, {}, {}", commitDefectVO.getTaskId(),
                commitDefectVO.getToolName(), commitDefectVO.getBuildId());
        try {
            finishLatch.await(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            log.info("finish latch await failed!");
            e.printStackTrace();
        }
        return currentDefectGroup.keySet();
    }

    private void processDefects(
            CommitDefectVO commitDefectVO,
            List<CCNDefectEntity> currentDefectList,
            TaskDetailVO taskDetailVO,
            Set<String> filePathSet,
            Set<String> relPathSet,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
            Set<String> filterPaths,
            BuildEntity buildEntity,
            int chunkNo,
            @NotNull LinkedBlockingQueue<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultQueue,
            List<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> secondResultList,
            Boolean isVip) {
        DefectClusterDTO defectClusterDTO = new DefectClusterDTO(
                commitDefectVO,
                buildEntity,
                transferAuthorList,
                "",
                ""
        );
        AsyncRabbitTemplate.RabbitConverterFuture<Boolean> clusterResult = newCCNDefectTracingComponent
                .executeCluster(defectClusterDTO,
                        taskDetailVO,
                        chunkNo,
                        currentDefectList,
                        relPathSet,
                        filePathSet,
                        filterPaths);
        log.info("ready to insert into blocking queue, task id: {}, tool name: {}, build id: {}, chunk no: {}",
                commitDefectVO.getTaskId(), commitDefectVO.getToolName(), commitDefectVO.getBuildId(), chunkNo);
        //如果不能够添加进阻塞队列，则说明阻塞队列已满，则本线程阻塞，进行等待
        try {
            int i = 0;
            while (true) {
                synchronized (asyncResultQueue) {
                    if (asyncResultQueue.offer(clusterResult)) {
                        break;
                    }
                }
                Thread.sleep(200L);
                i++;
                if (i > concurrentDefectTracingConfigCache.getTaskSendDelay(isVip)) {
                    log.info("concurrent defect tracing count is beyond limit and should be expired, task id: {}, "
                                    + "tool name: {}, build id: {}", commitDefectVO.getTaskId(),
                            commitDefectVO.getToolName(), commitDefectVO.getBuildId());
                    secondResultList.add(clusterResult);
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("blocked queue was unexpectedly interrupted, task id: {}, tool name: {}, build id: {}",
                    commitDefectVO.getTaskId(), commitDefectVO.getToolName(), commitDefectVO.getBuildId());
        }
    }

    /**
     * 计算平均圈复杂度
     *
     * @param taskId
     * @param defectJsonFileEntity
     * @param isFullScan
     * @param deleteFiles
     * @return
     */
    private float calculateAverageCCN(long taskId,
            CCNDefectJsonFileEntity<CCNDefectEntity> defectJsonFileEntity,
            boolean isFullScan,
            List<String> deleteFiles) {
        // 如果本次是全量扫描，则要清除已有的文件圈复杂度列表，如果是增量扫描，则要先清理待删除文件
        if (isFullScan) {
            fileCCNRepository.deleteByTaskId(taskId);
        } else {
            // 获取删除文件列表
            if (CollectionUtils.isNotEmpty(deleteFiles)) {
                fileCCNRepository.deleteByTaskIdIsAndFilePathIn(taskId, deleteFiles);
            }
        }

        // 保存本次上报的文件圈复杂度列表
        if (CollectionUtils.isNotEmpty(defectJsonFileEntity.getFilesTotalCCN())) {
            defectJsonFileEntity.getFilesTotalCCN().forEach(fileCCNEntity -> fileCCNEntity.setTaskId(taskId));
            fileCCNDao.upsertFileCCNList(defectJsonFileEntity.getFilesTotalCCN());
        }

        // 统计平均圈复杂度
        float averageCCN = 0;
        BigDecimal fileCount = BigDecimal.ZERO;
        BigDecimal totalCCN = BigDecimal.ZERO;
        List<FileCCNEntity> fileCCNEntities = fileCCNRepository.findByTaskId(taskId);
        for (FileCCNEntity fileCCNEntity : fileCCNEntities) {
            try {
                totalCCN = totalCCN.add(BigDecimal.valueOf(Double.parseDouble(fileCCNEntity.getTotalCCNCount())));
            } catch (Exception e) {
                log.error("totalCCNCount convert to double fail! fileCCNEntity{}",
                        JsonUtil.INSTANCE.toJson(fileCCNEntity), e);
                continue;
            }
            fileCount = fileCount.add(BigDecimal.ONE);
        }
        log.info("file count is : {}", fileCount);

        // 避免除以0报错
        if (fileCount.compareTo(BigDecimal.ZERO) > 0) {
            averageCCN = totalCCN.divide(fileCount, 2, BigDecimal.ROUND_CEILING).floatValue();
        }

        return averageCCN;
    }

    /**
     * 填充告警的代码仓库信息
     *
     * @param ccnDefectEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    private void fillDefectInfo(CCNDefectEntity ccnDefectEntity,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(ccnDefectEntity.getFilePath());
        if (fileLineAuthorInfo != null) {
            // 获取作者信息
            setAuthor(ccnDefectEntity, fileLineAuthorInfo);

            ccnDefectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
            ccnDefectEntity.setUrl(fileLineAuthorInfo.getUrl());
            ccnDefectEntity.setBranch(fileLineAuthorInfo.getBranch());
            ccnDefectEntity.setRevision(fileLineAuthorInfo.getRevision());
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType())) {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getRootUrl());
                if (null != repoSubModuleVO) {
                    ccnDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                }
            } else {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(fileLineAuthorInfo.getUrl());
                if (repoSubModuleVO != null) {
                    ccnDefectEntity.setRepoId(repoSubModuleVO.getRepoId());
                    if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                        ccnDefectEntity.setSubModule(repoSubModuleVO.getSubModule());
                    } else {
                        ccnDefectEntity.setSubModule("");
                    }
                }
            }
        }
    }

    /**
     * 获取告警作者，取函数涉及的所有行中的最新修改作者作为告警作者
     *
     * @param ccnDefectEntity
     * @param fileLineAuthorInfo
     */
    private void setAuthor(CCNDefectEntity ccnDefectEntity, ScmBlameVO fileLineAuthorInfo) {
        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        if (CollectionUtils.isNotEmpty(changeRecords)) {
            Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = getLineAuthorMap(changeRecords);

            // 获取函数涉及的所有行中的最新修改作者作为告警作者
            long functionLastUpdateTime = 0;
            for (int i = ccnDefectEntity.getStartLines(); i <= ccnDefectEntity.getEndLines(); i++) {
                if (lineAuthorMap != null) {
                    ScmBlameChangeRecordVO recordVO = lineAuthorMap.get(i);
                    if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime) {
                        functionLastUpdateTime = recordVO.getLineUpdateTime();
                        ccnDefectEntity.setAuthor(ToolParamUtils.trimUserName(recordVO.getAuthor()));
                        ccnDefectEntity.setLatestDateTime(recordVO.getLineUpdateTime());
                    }
                }
            }
        }
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.CCN.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.CCN.name().toLowerCase(Locale.ENGLISH);
    }
}
