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

import static com.tencent.devops.common.constant.CommonMessageCode.RECORD_NOT_EXITS;

import com.alibaba.fastjson.JSONReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.NewLintDefectTracingComponent;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.FileDefectGatherRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.FileDefectGatherDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.FileDefectGatherEntity;
import com.tencent.bk.codecc.defect.model.LintFileV2Entity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.service.git.GitRepoApiService;
import com.tencent.bk.codecc.defect.service.impl.redline.LintRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.LintDefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ScanType;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("lintDefectCommitConsumer")
@Slf4j
public class LintDefectCommitConsumer extends AbstractDefectCommitConsumer {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private NewLintDefectTracingComponent newLintDefectTracingComponent;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private FileDefectGatherRepository fileDefectGatherRepository;
    @Autowired
    private FileDefectGatherDao fileDefectGatherDao;
    @Autowired
    private GitRepoApiService gitRepoApiService;
    @Autowired
    private LintDefectStatisticServiceImpl lintDefectStatisticServiceImpl;
    @Autowired
    private ICheckerSetQueryBizService checkerSetQueryBizService;
    @Autowired
    private LintRedLineReportServiceImpl lintRedLineReportServiceImpl;
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

        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        String createFrom = taskVO.getCreateFrom();
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        if (buildEntity == null) {
            throw new CodeCCException(RECORD_NOT_EXITS, new String[]{"buildEntity", buildId});
        }

        // 判断本次是增量还是全量扫描
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 获取工具侧上报的已删除文件
        Set<String> deleteFiles;
        if (toolBuildStackEntity != null && CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            deleteFiles = Sets.newHashSet(toolBuildStackEntity.getDeleteFiles());
        } else {
            deleteFiles = Sets.newHashSet();
        }
        // 获取工具上报的已删除文件相对路径
        Set<String> rootPaths = toolBuildStackEntity == null
                || CollectionUtils.isEmpty(toolBuildStackEntity.getRootPaths()) ? Sets.newHashSet()
                : toolBuildStackEntity.getRootPaths();

        // 1.解析工具上报的告警文件，并做告警跟踪
        long beginTime = System.currentTimeMillis();

        // 1.1 获取规则列表
        // 获取当前任务规则集列表
        List<CheckerSetVO> checkerSetsOfTask = checkerSetQueryBizService.getTaskCheckerSets(taskVO.getProjectId(),
                taskId,
                toolName,
                "",
                true);
        // 取出规则集中的规则ID
        Set<String> checkerKeyList = checkerSetsOfTask.stream()
                .flatMap(it -> it.getCheckerProps().stream())
                .filter(it -> toolName.equalsIgnoreCase(it.getToolName()))
                .map(CheckerPropVO::getCheckerKey)
                .map(checkerKey -> {
                    if (checkerKey.contains("-tosa")) {
                        return checkerKey.replaceAll("-tosa", "");
                    }
                    return checkerKey;
                })
                .collect(Collectors.toSet());
        List<CheckerDetailEntity> checkerDetailEntityList =
                checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerKeyList);

        Map<String, Integer> checkerSeverityMap =
                checkerDetailEntityList.stream().collect(Collectors.toMap(CheckerDetailEntity::getCheckerKey,
                        CheckerDetailEntity::getSeverity));
        log.info("parseDefectJsonFile checker data ready cost: {}, {}, {}, {}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        List<LintFileV2Entity> gatherFileList = new ArrayList<>();
        beginTime = System.currentTimeMillis();

        RedisLock locker = null;
        List<LintDefectV2Entity> allNewDefectList;
        List<LintDefectV2Entity> allIgnoreDefectList;
        Set<String> currentFileSet;
        long tryBeginTime = System.currentTimeMillis();

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

            // 1.分批解析处理
            currentFileSet = parseDefectJsonFile(
                    commitDefectVO, taskVO, buildEntity, fileChangeRecordsMap,
                    codeRepoIdMap, gatherFileList, deleteFiles, checkerSeverityMap
            );
            log.info("parseDefectJsonFile cost: {}, {}, {}, {}",
                    System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

            // 2.处理告警收敛
            processFileDefectGather(
                    commitDefectVO, gatherFileList, fileChangeRecordsMap, currentFileSet, isFullScan, deleteFiles);

            // 查询所有告警
            beginTime = System.currentTimeMillis();
            allNewDefectList = lintDefectV2Repository.findNoneInstancesFieldByTaskIdAndToolNameAndStatus(
                    taskId,
                    toolName,
                    ComConstants.DefectStatus.NEW.value()
            );

            log.info("find lint file list cost: {}, {}, {}, {}, {}",
                    System.currentTimeMillis() - beginTime, taskId, toolName, buildId, allNewDefectList.size());

            // 3.更新文件状态
            beginTime = System.currentTimeMillis();
            updateFileEntityInfo(
                    taskId,
                    toolName,
                    allNewDefectList,
                    currentFileSet,
                    deleteFiles,
                    rootPaths,
                    isFullScan,
                    buildEntity,
                    checkerSeverityMap,
                    toolBuildStackEntity
            );
            log.info("update lint file list cost: {}, {}, {}, {}",
                    System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

            // 查询所有的已忽略告警-存量告警
            beginTime = System.currentTimeMillis();
            Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
            allIgnoreDefectList = lintDefectV2Dao.findIgnoreDefect(
                    taskId,
                    toolName,
                    buildId,
                    historyIgnoreType,
                    isFilterPathPath(taskVO)
            );
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

        String buildNum = buildEntity.getBuildNo();
        List<String> newCountCheckerList = StringUtils.isEmpty(buildNum)
                ? Lists.newArrayList()
                : allNewDefectList.stream()
                        .filter(x -> buildNum.equals(x.getCreateBuildNumber()))
                        .map(LintDefectV2Entity::getChecker)
                        .collect(Collectors.toList());

        // 4.统计本次扫描的告警
        beginTime = System.currentTimeMillis();
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
                        newCountCheckerList,
                        false
                )
        );
        log.info("statistic cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        // 5.更新构建告警快照
        beginTime = System.currentTimeMillis();
        buildSnapshotService.saveLintBuildDefect(taskId, toolName, buildEntity, allNewDefectList, allIgnoreDefectList);
        log.info("saveBuildDefect cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime, taskId, toolName,
                buildId);

        // 6.保存质量红线数据
        beginTime = System.currentTimeMillis();
        RedLineExtraParams<LintDefectV2Entity> redLineExtraParams = new RedLineExtraParams<>(allIgnoreDefectList);
        lintRedLineReportServiceImpl.saveRedLineData(taskVO, toolName, buildId, allNewDefectList, redLineExtraParams);
        log.info("redLineReportService.saveRedLineData cost: {}, {}, {}, {}", System.currentTimeMillis() - beginTime,
                taskId, toolName, buildId);

        // 7.回写工蜂mr信息
        beginTime = System.currentTimeMillis();
        gitRepoApiService.addLintGitCodeAnalyzeComment(taskVO, buildEntity.getBuildId(), buildEntity.getBuildNo(),
                toolName, allNewDefectList);
        log.info("gitRepoApiService.addLintGitCodeAnalyzeComment cost: {}, {}, {}, {}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildId);

        return true;
    }

    /**
     * MR 等Diff模式 忽略只关注相关的文件
     *
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
     * 处理文件告警收敛
     *
     * @param commitDefectVO
     * @param gatherFileList
     * @param fileChangeRecordsMap
     * @param currentFileSet
     * @param isFullScan
     * @param deleteFiles
     */
    private void processFileDefectGather(CommitDefectVO commitDefectVO,
            List<LintFileV2Entity> gatherFileList,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Set<String> currentFileSet, boolean isFullScan, Set<String> deleteFiles) {
        long taskId = commitDefectVO.getTaskId();
        String toolName = commitDefectVO.getToolName();
        List<FileDefectGatherEntity> allGatherEntityList = fileDefectGatherRepository.findByTaskIdAndToolName(taskId,
                toolName);
        Map<String, FileDefectGatherEntity> allGatherEntityMap = allGatherEntityList.stream().collect(Collectors.toMap(
                gather -> StringUtils.isEmpty(gather.getRelPath()) ? gather.getFilePath() : gather.getRelPath(),
                Function.identity(), (k, v) -> v));
        List<FileDefectGatherEntity> needUpsertGatherEntityList = new ArrayList<>();

        long curTime = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(gatherFileList)) {
            FileDefectGatherVO fileDefectGatherVO = new FileDefectGatherVO();
            List<FileDefectGatherVO.GatherFile> gatherFileVOs = new ArrayList<>();
            int totalGatherDefects = 0;
            int totalGatherFiles = 0;
            for (LintFileV2Entity lintFileEntity : gatherFileList) {
                FileDefectGatherEntity gather = lintFileEntity.getGather();
                if (gather.isGatherDetail()) {
                    fileDefectGatherVO.setFileName(lintFileEntity.getFile());
                } else {
                    needUpsertGatherEntityList.add(gather);
                    String filePath = lintFileEntity.getFile();
                    ScmBlameVO fileLineAuthorInfo = fileChangeRecordsMap.get(lintFileEntity.getFile());
                    String relPath = fileLineAuthorInfo != null ? fileLineAuthorInfo.getFileRelPath() : null;
                    gather.setTaskId(taskId);
                    gather.setToolName(toolName);
                    gather.setStatus(ComConstants.DefectStatus.NEW.value());
                    gather.setFilePath(filePath);
                    gather.setRelPath(relPath);
                    FileDefectGatherEntity oldGather = allGatherEntityMap.get(StringUtils.isEmpty(relPath)
                            ? filePath : relPath);
                    if (null == oldGather) {
                        gather.setCreatedDate(curTime);
                    } else {
                        gather.setCreateTime(oldGather.getCreateTime());
                        gather.setUpdatedDate(curTime);
                    }

                    FileDefectGatherVO.GatherFile gatherFile = new FileDefectGatherVO.GatherFile();
                    BeanUtils.copyProperties(gather, gatherFile);
                    gatherFileVOs.add(gatherFile);

                    totalGatherDefects += gather.getTotal();
                    totalGatherFiles++;

                    currentFileSet.add(StringUtils.isEmpty(gather.getRelPath()) ? gather.getFilePath() :
                            gather.getRelPath());

                    allGatherEntityMap.remove(StringUtils.isEmpty(relPath) ? filePath : relPath);
                }
            }
            fileDefectGatherVO.setGatherFileList(gatherFileVOs);
            fileDefectGatherVO.setDefectCount(totalGatherDefects);
            fileDefectGatherVO.setFileCount(totalGatherFiles);
            commitDefectVO.setMessage(GsonUtils.toJson(fileDefectGatherVO));
        }

        /**
         * 只处理打开状态的收敛文件：
         * 1、文件已删除，则设置为已修复状态
         * 2、全量扫描，且此次分析中没有上报，则设置为已修复状态
         * 3、如果是增量扫描，且此次分析中没有上报，则不做处理
         */
        allGatherEntityMap.forEach((file, gatherEntity) ->
        {
            if (gatherEntity.getStatus() == ComConstants.DefectStatus.NEW.value()
                    && (deleteFiles.contains(gatherEntity.getFilePath()) || isFullScan)) {
                gatherEntity.setStatus(ComConstants.DefectStatus.FIXED.value());
                gatherEntity.setFixedTime(curTime);
                needUpsertGatherEntityList.add(gatherEntity);
            }
        });
        fileDefectGatherDao.upsertGatherFileListByPath(needUpsertGatherEntityList);
    }

    /**
     * lint类告警json文件格式：
     * [
     * {
     * "filePath": "/data/landun/workspace/game-lab-build/src/components/Ide/src/BlockEditor/spritelib/Actor.js",
     * "defectList":
     * [
     * {
     * "checker": "space-before-blocks",
     * "message": "Missing space before opening brace.",
     * "linenum": 26,
     * "pinpointHash": "6:XY7ATX3lBo7AElcG1C7cQ6cGzMcNGNAiGrQWM/4h25DehIacGACFILnQrWQGZ+Jf
     * :hb3bhr2Q3ncU1HWMRChIXrLQrx3KZa"
     * }
     * ]
     * },
     * {
     * "filePath": "/data/landun/workspace/game-lab-build/src/components/Ide/src/BlockEditor/spritelib/Actor2.js",
     * "gather":
     * {
     * "total": 10000
     * }
     * }
     * ]
     *
     * @param commitDefectVO
     * @param taskVO
     * @param buildEntity
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     * @param deleteFiles
     * @param checkerSeverityMap
     * @return
     */
    private Set<String> parseDefectJsonFile(
            CommitDefectVO commitDefectVO,
            TaskDetailVO taskVO,
            BuildEntity buildEntity,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap,
            List<LintFileV2Entity> gatherFileList,
            Set<String> deleteFiles,
            Map<String, Integer> checkerSeverityMap) {

        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 获取告警文件地址
        String fileIndex = scmJsonComponent.getDefectFileIndex(streamName, toolName, buildId);
        if (StringUtils.isEmpty(fileIndex)) {
            log.warn("Can not find raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format(
                    "找不到的告警文件: %s, %s, %s", streamName, toolName, buildId), null);
        }

        File defectFile = new File(fileIndex);
        if (!defectFile.exists()) {
            log.warn("文件不存在: {}", fileIndex);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format("找不到告警文件: %s",
                    fileIndex), null);
        }

        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskId(taskId);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null) {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, toolName);

        Set<String> currentFileSet = new HashSet<>();
        // 通过流式读json文件
        try (FileInputStream fileInputStram = new FileInputStream(defectFile);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStram, StandardCharsets.UTF_8);
                JSONReader reader = new JSONReader(inputStreamReader)) {
            reader.startArray();
            int cursor = 0;
            int chunkNo = 0;
            Set<String> eachBatchFilePathSet = new HashSet<>();
            Set<String> eachBatchRelPathSet = new HashSet<>();
            List<LintDefectV2Entity> lintDefectList = new ArrayList<>();
            //用于存储因为阻塞队列满了以后无法塞入队列的发送结果
            List<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultList = new ArrayList<>();
            AtomicBoolean running = new AtomicBoolean(true);
            CountDownLatch finishLatch = new CountDownLatch(1);
            Set<Long> taskList = concurrentDefectTracingConfigCache.getVipTaskSet();
            Boolean isVip = CollectionUtils.isNotEmpty(taskList) && taskList.contains(taskId);
            LinkedBlockingQueue<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultQueue =
                    setConcurrentBlockingQueue(
                            taskId,
                            toolName,
                            buildId,
                            running,
                            finishLatch,
                            isVip
                    );

            while (reader.hasNext()) {
                LintFileV2Entity lintFileEntity = reader.readObject(LintFileV2Entity.class);

                if (CollectionUtils.isNotEmpty(lintFileEntity.getDefects())) {
                    ScmBlameVO scmBlameVO = fileChangeRecordsMap.get(lintFileEntity.getFile());
                    // 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
                    List<LintDefectV2Entity> tmpDefectList = lintFileEntity.getDefects().stream()
                            .filter(defect -> fillDefectInfo(taskVO, toolName, defect, lintFileEntity.getFile(),
                                    scmBlameVO, codeRepoIdMap,
                                    checkerSeverityMap))
                            .collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(tmpDefectList)) {
                        log.warn("file defectList is empty after filter. {}, {}, {}", taskId, toolName,
                                lintFileEntity.getFile());
                        deleteFiles.add(lintFileEntity.getFile());
                        continue;
                    }

                    if (StringUtils.isNotBlank(tmpDefectList.get(0).getRelPath())) {
                        eachBatchRelPathSet.add(tmpDefectList.get(0).getRelPath());
                    }
                    eachBatchFilePathSet.add(lintFileEntity.getFile());

                    String filePath = StringUtils.isEmpty(tmpDefectList.get(0).getRelPath()) ? lintFileEntity.getFile()
                            : tmpDefectList.get(0).getRelPath();
                    currentFileSet.add(filePath);

                    lintDefectList.addAll(tmpDefectList);
                    cursor += tmpDefectList.size();
                    if (cursor > MAX_PER_BATCH) {
                        // 分批处理告警文件
                        processFileDefect(commitDefectVO, lintDefectList, taskVO, eachBatchFilePathSet,
                                eachBatchRelPathSet, filterPaths, buildEntity, chunkNo, transferAuthorList,
                                asyncResultQueue, asyncResultList, isVip);
                        cursor = 0;
                        lintDefectList = new ArrayList<>();
                        eachBatchFilePathSet = new HashSet<>();
                        eachBatchRelPathSet = new HashSet<>();
                        chunkNo++;
                    }
                } else if (lintFileEntity.getGather() != null) {
                    gatherFileList.add(lintFileEntity);
                } else {
                    log.warn("file defectList is empty. {}, {}, {}", taskId, toolName, lintFileEntity.getFile());
                }
            }
            reader.endArray();

            if (lintDefectList.size() > 0) {
                processFileDefect(commitDefectVO, lintDefectList, taskVO, eachBatchFilePathSet,
                        eachBatchRelPathSet, filterPaths, buildEntity, chunkNo, transferAuthorList,
                        asyncResultQueue, asyncResultList, isVip);
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
            log.info("wait for count down latch to pass, {}, {}, {}", taskId, toolName, buildId);
            try {
                finishLatch.await(2, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                log.info("finish latch await failed!");
                e.printStackTrace();
            }
        } catch (IOException e) {
            log.warn("Read defect file exception: {}", fileIndex, e);
        }

        return currentFileSet;
    }

    /**
     * 更新告警状态
     *
     * @param allDefectEntityList
     * @param currentFileSet
     * @param deleteFiles
     * @param isFullScan
     * @param buildEntity
     * @param checkerSeverityMap
     */
    private void updateFileEntityInfo(long taskId,
            String toolName,
            List<LintDefectV2Entity> allDefectEntityList,
            Set<String> currentFileSet,
            Set<String> deleteFiles,
            Set<String> rootPaths,
            boolean isFullScan,
            BuildEntity buildEntity,
            Map<String, Integer> checkerSeverityMap,
            ToolBuildStackEntity toolBuildStackEntity) {
        if (CollectionUtils.isEmpty(allDefectEntityList)) {
            log.info("allNewDefectList is empty. buildId:{}", buildEntity.getBuildId());
            return;
        }
        //获取已删除文件的相对路径
        Set<String> deleteRelFiles = new HashSet<>();
        if (CollectionUtils.isNotEmpty(rootPaths) && CollectionUtils.isNotEmpty(deleteFiles)) {
            deleteFiles.forEach(deleteFile -> rootPaths.forEach(
                    rootPath -> {
                        if (deleteFile.startsWith(rootPath)) {
                            deleteRelFiles.add(deleteFile.replaceFirst(rootPath, ""));
                        }
                    }
            ));
        }

        Map<String, CodeRepoEntity> repoIdMap = Maps.newHashMap();
        Map<String, CodeRepoEntity> urlMap = Maps.newHashMap();
        getCodeRepoMap(allDefectEntityList.get(0).getTaskId(), isFullScan, buildEntity, repoIdMap, urlMap);

        List<LintDefectV2Entity> needUpdateDefectList = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long earliestTime;
        long minLineUpdateTime = Long.MAX_VALUE;
        if (toolBuildStackEntity == null || toolBuildStackEntity.getCommitSince() == null) {
            earliestTime = 0L;
        } else {
            earliestTime = toolBuildStackEntity.getCommitSince();
        }

        for (LintDefectV2Entity defect : allDefectEntityList) {
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
            Integer defectCheckerSeverity = checkerSeverityMap.get(defect.getChecker());
            if (defectCheckerSeverity == null) {
                defectCheckerSeverity = defect.getSeverity();
            }
            boolean needUpdate = false;
            if (!toolName.equals(ComConstants.Tool.WOODPECKER_COMMITSCAN.name())) {
                if (deleteFiles.contains(filePath)
                        || (StringUtils.isNotEmpty(relPath)
                        && deleteRelFiles.stream().anyMatch(it -> it.equals(relPath)))
                        || (isFullScan && notCurrentBuildUpload)) {
                    defect.setStatus(defect.getStatus() | ComConstants.DefectStatus.FIXED.value());
                    defect.setFixedTime(currentTime);
                    defect.setFixedBuildNumber(buildEntity.getBuildNo());
                    needUpdate = true;
                } else if (!isFullScan && notCurrentBuildUpload) {
                    String newBranch = null;
                    String url = defect.getUrl();
                    String repoId = defect.getRepoId();
                    if (StringUtils.isNotEmpty(url) && urlMap.get(url) != null) {
                        newBranch = urlMap.get(url).getBranch();
                    } else if (StringUtils.isNotEmpty(repoId) && repoIdMap.get(repoId) != null) {
                        newBranch = repoIdMap.get(repoId).getBranch();
                    }
                    if (StringUtils.isNotEmpty(newBranch)
                            && !newBranch.equals(defect.getBranch())) {
                        defect.setBranch(newBranch);
                        needUpdate = true;
                    }
                }
            } else {
                needUpdate = incrementFixDefect(
                        earliestTime,
                        currentTime,
                        buildEntity,
                        defect,
                        currentFileSet);
                if (defect.getStatus() == ComConstants.DefectStatus.NEW.value()
                        && defect.getLineUpdateTime() < minLineUpdateTime) {
                    minLineUpdateTime = defect.getLineUpdateTime();
                }
            }

            if (defectCheckerSeverity != defect.getSeverity()) {
                defect.setSeverity(defectCheckerSeverity);
                needUpdate = true;
            }

            if (needUpdate) {
                needUpdateDefectList.add(defect);
            }
        }

        if (toolName.equals(ComConstants.Tool.WOODPECKER_COMMITSCAN.name())) {
            ToolBuildInfoEntity toolBuildInfo = toolBuildInfoRepository.findFirstByTaskIdAndToolName(
                    taskId, toolName);

            if (minLineUpdateTime == Long.MAX_VALUE) {
                minLineUpdateTime = 0L;
            }
            toolBuildInfo.setCommitSince(minLineUpdateTime);
            toolBuildInfoRepository.save(toolBuildInfo);
        }

        if (CollectionUtils.isNotEmpty(needUpdateDefectList)) {
            lintDefectV2Dao.batchUpdateByFile(needUpdateDefectList);
        }
    }

    /**
     * 按commit时间增量的工具修复未上报文件告警(WOODPECKER_COMMITSCAN)
     *
     * @param defect
     * @param buildEntity
     * @param currentFileSet
     * @param currentTime
     * @param earliestTime
     */
    private boolean incrementFixDefect(
            long earliestTime,
            long currentTime,
            BuildEntity buildEntity,
            LintDefectV2Entity defect,
            Set<String> currentFileSet) {
        // 所有本次增量扫描时间点之后的"待修复"告警中筛选出本次未上报的告警文件，将状态置为"已修复"
        if (defect.getLineUpdateTime() >= earliestTime
                && !currentFileSet.contains(defect.getFilePath())) {
            defect.setStatus(ComConstants.DefectStatus.NEW.value()
                    | ComConstants.DefectStatus.FIXED.value());
            defect.setFixedTime(currentTime);
            defect.setFixedBuildNumber(buildEntity.getBuildNo());
            return true;
        }
        return false;
    }

    /**
     * 对于用阻塞队列判断消息的优先级的机制：
     * 1. 配置定长的阻塞队列，长度为一个扫描任务最大能够占有消息队列处理的数量
     * 2. 对于处理完的消息会推入阻塞队列，采用循环判断无阻塞推入结果的方式（推入过程加锁）
     * （2-1）如果推入成功，则说明阻塞队列还有空余位置，正常往下走
     * （2-2）如果推入失败，则说明阻塞队列已满，则会继续尝试推入，知道固定的时间（默认为10分钟），从而减缓推入的速度，达到限流的效果
     * 3. 另起线程，实时消费阻塞队列中推入的结果，获取的方式为，取出一个，判断是否完成，完成则继续取，没有完成则重新推入队列（该过程加同样的锁）
     * <p>
     * <p>
     * 优点：
     * 1. 通过正常下发聚类任务的过程加了推入阻塞队列的方式，达到限流的效果
     * 2. 取出队列判断的过程，可以解决了队列中有序的任务完成顺序为无序时，取出判断不会阻塞在某个特定的慢速任务上，达到谁先完成，谁先出队列的效果
     *
     * @param commitDefectVO
     * @param currentDefectEntityList
     * @param taskDetailVO
     * @param filePathSet
     * @param relPathSet
     * @param filterPathSet
     * @param buildEntity
     * @param chunkNo
     * @param transferAuthorList
     * @param asyncResultQueue
     * @return
     */
    private void processFileDefect(
            CommitDefectVO commitDefectVO,
            List<LintDefectV2Entity> currentDefectEntityList,
            TaskDetailVO taskDetailVO,
            Set<String> filePathSet,
            Set<String> relPathSet,
            Set<String> filterPathSet,
            BuildEntity buildEntity,
            int chunkNo,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
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
        AsyncRabbitTemplate.RabbitConverterFuture<Boolean> clusterResult = newLintDefectTracingComponent
                .executeCluster(defectClusterDTO,
                        taskDetailVO,
                        chunkNo,
                        currentDefectEntityList,
                        relPathSet,
                        filePathSet,
                        filterPathSet);
        log.info("ready to insert into blocking queue, task id: {}, tool name: {}, build id: {}, chunk no: {}",
                commitDefectVO.getTaskId(), commitDefectVO.getToolName(), commitDefectVO.getBuildId(), chunkNo);
        //如果不能够添加进阻塞队列，则说明阻塞队列已满，则本线程阻塞，进行等待,等待10分钟，如果还不行，再进行下发
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
     * 填充文件内的告警的信息，其中如果告警的规则不属于已录入平台的规则，则移除告警
     *
     * @param defectEntity
     * @param filePath
     * @param fileLineAuthorInfo
     * @param codeRepoIdMap
     * @param checkerSeverityMap
     * @return
     */
    private boolean fillDefectInfo(
            TaskDetailVO taskDetailVO,
            String toolName,
            LintDefectV2Entity defectEntity,
            String filePath,
            ScmBlameVO fileLineAuthorInfo,
            Map<String, RepoSubModuleVO> codeRepoIdMap,
            Map<String, Integer> checkerSeverityMap
    ) {
        defectEntity.setFilePath(filePath);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        defectEntity.setFileName(filePath.substring(fileNameIndex + 1));
        Integer severity = checkerSeverityMap.get(defectEntity.getChecker());
        if (severity == null) {
            log.warn("Checker invalid! checker: {}", defectEntity.getChecker());
            return false;
        }
        defectEntity.setSeverity(severity);

        if (fileLineAuthorInfo != null) {
            setFileInfo(defectEntity, fileLineAuthorInfo, codeRepoIdMap);
            setAuthor(defectEntity, fileLineAuthorInfo);
        } else {
            log.warn("fileLineAuthorInfo is null, {}, {}", filePath, defectEntity.getLineNum());
        }

        if (toolName.equals(ComConstants.Tool.BLACKDUCK.name())) {
            defectEntity.setAuthor(Lists.newArrayList(taskDetailVO.getCreatedBy()));
        }

        // scm_blame能匹配出具体行，但作者字段可能为null
        if (CollectionUtils.isEmpty(defectEntity.getAuthor())) {
            defectEntity.setAuthor(Lists.newArrayList("unassigned"));
        } else {
            List<String> finalAuthorList = defectEntity.getAuthor().stream()
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(finalAuthorList)) {
                defectEntity.setAuthor(Lists.newArrayList("unassigned"));
            } else {
                defectEntity.setAuthor(finalAuthorList);
            }
        }

        if (StringUtils.isEmpty(defectEntity.getSubModule())) {
            defectEntity.setSubModule("");
        }

        return true;
    }

    private void setFileInfo(LintDefectV2Entity defectEntity, ScmBlameVO fileLineAuthorInfo, Map<String,
            RepoSubModuleVO> codeRepoIdMap) {
        defectEntity.setFileUpdateTime(fileLineAuthorInfo.getFileUpdateTime());
        defectEntity.setRevision(fileLineAuthorInfo.getRevision());
        // 兼容处理错误的历史数据: devops-virtual-https://xxx.yyy/zzz.git
        String urlFinal = fileLineAuthorInfo.getUrl();
        String urlFlag = "devops-virtual-";
        if (StringUtils.isNotEmpty(urlFinal) && urlFinal.startsWith(urlFlag)) {
            urlFinal = urlFinal.replaceFirst(urlFlag, "");
        }
        defectEntity.setUrl(urlFinal);
        defectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
        defectEntity.setBranch(fileLineAuthorInfo.getBranch());
        if (MapUtils.isNotEmpty(codeRepoIdMap)) {
            RepoSubModuleVO repoSubModuleVO = null;
            String url = null;
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType())
                    || ComConstants.CodeHostingType.PERFORCE.name().equalsIgnoreCase(fileLineAuthorInfo.getScmType())) {
                //如果是svn或perforce用rootUrl关联
                url = fileLineAuthorInfo.getRootUrl();
            } else {
                url = defectEntity.getUrl();
            }
            repoSubModuleVO = codeRepoIdMap.get(url);

            if (repoSubModuleVO != null) {
                defectEntity.setRepoId(repoSubModuleVO.getRepoId());
                if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                    defectEntity.setSubModule(repoSubModuleVO.getSubModule());
                }
            }
        }
    }

    private void setAuthor(LintDefectV2Entity defectEntity, ScmBlameVO fileLineAuthorInfo) {
        List<ScmBlameChangeRecordVO> changeRecords = fileLineAuthorInfo.getChangeRecords();
        if (CollectionUtils.isNotEmpty(changeRecords)) {
            int defectLine = defectEntity.getLineNum();
            // 告警中的行号为0的改成1
            if (defectLine == 0) {
                defectLine = 1;
                defectEntity.setLineNum(defectLine);
            }
            for (ScmBlameChangeRecordVO changeRecord : changeRecords) {
                boolean isFound = false;
                List<Object> lines = changeRecord.getLines();
                if (lines != null && lines.size() > 0) {
                    for (Object line : lines) {
                        if (line instanceof Integer && defectLine == (int) line) {
                            isFound = true;
                        } else if (line instanceof List) {
                            List<Integer> lineScope = (List<Integer>) line;
                            if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1) {
                                if (lineScope.get(0) <= defectLine
                                        && lineScope.get(lineScope.size() - 1) >= defectLine) {
                                    isFound = true;
                                }
                            }
                        }
                        if (isFound) {
                            String author = ToolParamUtils.trimUserName(changeRecord.getAuthor());
                            defectEntity.setAuthor(Lists.newArrayList(author));
                            long lineUpdateTime = DateTimeUtils.getThirteenTimestamp(changeRecord.getLineUpdateTime());
                            defectEntity.setLineUpdateTime(lineUpdateTime);
                            break;
                        }
                    }
                }
                if (isFound) {
                    break;
                }
            }
        }
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.LINT.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.LINT.name().toLowerCase(Locale.ENGLISH);
    }
}
