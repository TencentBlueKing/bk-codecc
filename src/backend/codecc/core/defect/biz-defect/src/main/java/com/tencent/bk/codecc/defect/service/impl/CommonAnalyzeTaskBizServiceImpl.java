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

package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.constant.ComConstants.ACQUIRY_LOCK_EXPIRY_TIME_MILLIS;
import static com.tencent.devops.common.constant.ComConstants.COMMIT_PLATFORM_LOCK_EXPIRY_TIME_MILLIS;
import static com.tencent.devops.common.constant.ComConstants.DefectStatus;
import static com.tencent.devops.common.constant.ComConstants.REDIS_MQ_KEY_LOCK_EXPIRY_TIME_MILLIS;
import static com.tencent.devops.common.constant.RedisKeyConstants.PREFIX_QUEUE_COMMIT_PLATFORM;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CodeFileUrlDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.dto.WebsocketDTO;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.service.impl.redline.CompileRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.bk.codecc.defect.vo.CodeFileUrlVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.Step4Cov;
import com.tencent.devops.common.constant.ComConstants.StepFlag;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.redis.lock.JRedisLock;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 平台类工具分析记录上报的接口实现
 *
 * @version V1.0
 * @date 2019/10/3
 */
@Slf4j
@Service("CommonAnalyzeTaskBizService")
public class CommonAnalyzeTaskBizServiceImpl extends AbstractAnalyzeTaskBizService {
    @Autowired
    protected StatisticDao statisticDao;
    @Autowired
    protected BuildRepository buildRepository;
    @Autowired
    protected CommonStatisticRepository commonStatisticRepository;
    @Autowired
    protected CodeFileUrlDao codeFileUrlDao;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private CompileRedLineReportServiceImpl compileRedLineReportServiceImpl;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private ScmFileInfoService scmFileInfoService;
    @Autowired
    private CommonKafkaClient commonKafkaClient;
    @Autowired
    private TaskLogOverviewService taskLogOverviewService;
    @Autowired
    private BaseDataCacheService baseDataCacheService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private DefectConverter defectConverter;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;


    @Override
    protected void postHandleDefectsAndStatistic(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO) {
        log.info("begin postHandleDefectsAndStatistic...");
        if (uploadTaskLogStepVO.isFastIncrement()) {
            log.info("fast increment need not postHandleDefectsAndStatistic");
            return;
        }
        String streamName = uploadTaskLogStepVO.getStreamName();
        String toolName = uploadTaskLogStepVO.getToolName();
        int stepNum = uploadTaskLogStepVO.getStepNum();
        int flag = uploadTaskLogStepVO.getFlag();
        String buildId = uploadTaskLogStepVO.getPipelineBuildId();
        long taskId = uploadTaskLogStepVO.getTaskId();
        // 工具分析步骤执行成功，则开始启动将告警提交到platform
        if (stepNum == ComConstants.Step4Cov.ANALYZE.value() && flag == ComConstants.StepFlag.SUCC.value()) {
            log.info("begin commit defect to platform.");
            boolean sendSuccess = sendCommitPlatformMsg(taskVO, streamName, toolName, buildId);

            // 如果发送commit platform消息失败，则终止本次分析
            if (!sendSuccess) {
                uploadTaskLogStepVO.setFlag(ComConstants.StepFlag.FAIL.value());
                uploadTaskLogStepVO.setMsg("发送commit platform消息失败，终止本次分析，请联系CodeCC助手");
                TaskLogEntity lastTaskLogEntity =
                        taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskVO.getTaskId(), toolName, buildId);
                updateTaskLog(lastTaskLogEntity, uploadTaskLogStepVO, taskVO);
            }
        } else if (stepNum == ComConstants.Step4Cov.COMMIT.value() && flag == ComConstants.StepFlag.SUCC.value()) {
            // 如果工具缺陷提交到自带platform成功，则开始启动将告警提交到codecc
            log.info("begin commit defect to codecc.");
            // 通过消息队列通知coverity服务提单
            CommitDefectVO commitDefectVO = new CommitDefectVO();
            commitDefectVO.setTaskId(taskId);
            commitDefectVO.setStreamName(streamName);
            commitDefectVO.setToolName(toolName);
            commitDefectVO.setBuildId(uploadTaskLogStepVO.getPipelineBuildId());
            commitDefectVO.setTriggerFrom(uploadTaskLogStepVO.getTriggerFrom());
            commitDefectVO.setCreateFrom(taskVO.getCreateFrom());

            boolean isMigrationDone = commonDefectMigrationService.isMigrationDone(taskId);

            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom())) {
                // 若所有开关均为关闭或已执行过迁移逻辑，则直接进入业务逻辑，不进"数据迁移"队列
                if (isMigrationDone) {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(),
                            ConstantsKt.PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(),
                            commitDefectVO
                    );
                } else {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON_OPENSOURCE,
                            ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON_OPENSOURCE,
                            commitDefectVO
                    );
                }
            } else {
                // 若所有开关均为关闭或已执行过迁移逻辑，则直接进入业务逻辑，不进"数据迁移"队列
                if (isMigrationDone) {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + toolName.toLowerCase(),
                            ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + toolName.toLowerCase(),
                            commitDefectVO
                    );
                } else {
                    rabbitTemplate.convertAndSend(
                            ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON,
                            ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON,
                            commitDefectVO
                    );
                }
            }
        } else if (stepNum == getSubmitStepNum() && flag == ComConstants.StepFlag.SUCC.value()) {
            log.info("begin statistic defect count.");
            handleSubmitSuccess(uploadTaskLogStepVO, taskVO);
        }

        /* 满足以下情况需要释放commit时加的锁，
         * 1. commit步骤失败
         * 2. submit结束
         */
        if ((stepNum == ComConstants.Step4Cov.COMMIT.value() && flag == ComConstants.StepFlag.FAIL.value())
                || (stepNum == getSubmitStepNum() && flag != ComConstants.StepFlag.PROCESSING.value())) {
            String lockKey = String.format("%s:%s:%s", RedisKeyConstants.CONCURRENT_COMMIT_LOCK, streamName, toolName);
            JRedisLock lock = new JRedisLock(redisTemplate, lockKey, COMMIT_PLATFORM_LOCK_EXPIRY_TIME_MILLIS, buildId);
            lock.releaseDiffClientLock();
        }
    }

    /**
     * 通过消息队列排队等待schedule调用分析服务器commit platform
     *
     * @param taskVO
     * @param streamName
     * @param toolName
     * @param buildId
     * @return
     */
    private boolean sendCommitPlatformMsg(TaskDetailVO taskVO, String streamName, String toolName, String buildId) {
        PushVO pushVO = new PushVO();
        pushVO.setStreamName(streamName);
        pushVO.setToolName(toolName);
        pushVO.setBuildId(buildId);
        pushVO.setProjectId(taskVO.getProjectId());
        pushVO.setCreateFrom(taskVO.getCreateFrom());
        RLock lock = null;
        try {
            /* 获取消息前需要先加锁，两个目的：
             * 1.确保一个队列同一个时间只有一个客户端能取出消息，避免多个客户端同时取出消息后获取不到commit锁再塞回去，导致commit顺序错乱
             * 2.确保消息队列为空时，删除队列的同时，生产端往队列里面写消息，最终导致消息被误删
             */
            String redisMQKey = String.format("%s:%s:%s", PREFIX_QUEUE_COMMIT_PLATFORM, streamName, toolName);
            lock = redissonClient.getLock(RedisKeyConstants.PREFIX_REDIS_MQ_KEY_LOCK + redisMQKey);

            // 最多等待10秒，上锁以后5秒自动解锁
            if (lock != null && lock.tryLock(ACQUIRY_LOCK_EXPIRY_TIME_MILLIS, REDIS_MQ_KEY_LOCK_EXPIRY_TIME_MILLIS,
                    TimeUnit.MILLISECONDS)) {

                Long size = redisTemplate.opsForList().leftPush(redisMQKey, JsonUtil.INSTANCE.toJson(pushVO));

                // 如果list为1，表示首次创建队列，需要将队列名插入队列汇总set
                if (size == 1) {
                    redisTemplate.opsForSet().add(RedisKeyConstants.CUSTOM_REDIS_MQ_KEYS, redisMQKey);
                } else if (size > ComConstants.COMMON_NUM_10L) {
                    // 如果排队任务数超过10，打印告警日志
                    log.warn("Please note that the concurrent commit platform exceeds 10: {}, {}, {}, {}",
                            taskVO.getTaskId(), streamName, toolName, size);
                }
                return true;
            }
        } catch (Exception e) {
            log.error("commit defect to platform fail: {}", pushVO, e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 处理提单成功
     *
     * @param uploadTaskLogStepVO
     * @param taskVO
     */
    @Override
    protected void handleSubmitSuccess(UploadTaskLogStepVO uploadTaskLogStepVO, TaskDetailVO taskVO) {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();
        String buildId = uploadTaskLogStepVO.getPipelineBuildId();
        CommonStatisticEntity statisticEntity = CommonStatisticEntity.constructByZeroVal();
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(toolName);
        statisticEntity.setTime(System.currentTimeMillis());
        statisticEntity.setBuildId(buildId);

        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        statisticDao.getAndClearDefectStatistic(statisticEntity, buildEntity.getBuildNo());

        statisticEntity = commonStatisticRepository.save(statisticEntity);

        // 异步统计非new状态的告警数
        if (BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(taskVO.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_OPENSOURCE, statisticEntity);
        } else {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_CLOSE_DEFECT_STATISTIC, ROUTE_CLOSE_DEFECT_STATISTIC, statisticEntity);
        }

        // 将数据加入数据平台
        commonKafkaClient.pushCommonStatisticToKafka(statisticEntity);

        // 保存首次分析成功时间
        saveFirstSuccessAnalyszeTime(taskId, toolName);

        // 保存本次构建遗留告警告警列表快照
        boolean migrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);
        List<CommonDefectEntity> snapshotNewDefectList;
        if (migrationSuccessful) {
            snapshotNewDefectList = defectConverter.lintToCommon(
                    lintDefectV2Repository.findSnapshotRedLineFieldByTaskIdAndToolNameAndStatus(
                            taskId,
                            toolName,
                            DefectStatus.NEW.value()
                    )
            );
        } else {
            snapshotNewDefectList = defectRepository.findSnapshotRedLineFieldByTaskIdAndToolNameAndStatus(
                    taskId,
                    toolName,
                    DefectStatus.NEW.value()
            );
        }
        Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
        List<CommonDefectEntity> snapshotIgnoreDefectList =
                defectDao.findIgnoreDefectForSnapshot(
                        taskId,
                        toolName,
                        historyIgnoreType
                );
        log.info("common snapshot task id: {}, new size: {}, ignore size: {}", taskId, snapshotNewDefectList.size(),
                snapshotIgnoreDefectList.size());
        buildSnapshotService.saveCommonBuildDefect(taskId, toolName, buildEntity, snapshotNewDefectList,
                snapshotIgnoreDefectList);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        // 保存质量红线数据
        compileRedLineReportServiceImpl.saveRedLineData(taskVO, toolName, buildId, snapshotNewDefectList);

        // 处理md5.json
        scmFileInfoService.parseFileInfo(uploadTaskLogStepVO.getTaskId(),
                uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getToolName(),
                uploadTaskLogStepVO.getPipelineBuildId());

        // 清除强制全量扫描标志
        clearForceFullScan(taskId, toolName);

        // 设置当前工具执行完成
        uploadTaskLogStepVO.setFinish(true);
    }

    @Override
    protected void updateCodeRepository(UploadTaskLogStepVO uploadTaskLogStepVO, TaskLogEntity taskLogEntity) {
        if (uploadTaskLogStepVO.getStepNum() == ComConstants.Step4Cov.COMMIT.value()
                && uploadTaskLogStepVO.getFlag() == ComConstants.StepFlag.SUCC.value()) {

            Locale locale = AbstractI18NResponseAspect.getLocale();

            ThreadPoolUtil.addRunnableTask(() -> {
                /*
                    工具侧整理输出的文件路径格式跟工具无关，如下：
                    window：D:/workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    linux:  /data/landun/workspace/test/parallel/test-string-decoder-fuzz.js
                    后台存入t_code_file_url表中时，做了如下处理：
                    window下的路径将盘号去掉，变成
                    /workspace/svnauth_svr/app/utils/jgit_proxy/SSHProxySessionFactory.java
                    另外url转换成标准http格式
                 */
                saveCodeFileUrl(uploadTaskLogStepVO);

                // 保存代码仓信息
                saveCodeRepoInfo(uploadTaskLogStepVO, locale);
            });

        }
    }

    /**
     * 保存代码文件的URL信息
     *
     * @param uploadTaskLogStepVO
     */
    private void saveCodeFileUrl(UploadTaskLogStepVO uploadTaskLogStepVO) {
        String scmUrlJsonStr = scmJsonComponent.loadRepoFileUrl(uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getToolName(), uploadTaskLogStepVO.getPipelineBuildId());
        if (StringUtils.isNotEmpty(scmUrlJsonStr)) {
            CodeFileUrlVO codeFileUrlVO = JsonUtil.INSTANCE.to(scmUrlJsonStr, CodeFileUrlVO.class);
            String fileListStr = codeFileUrlVO.getFileList();
            String codeFileURLJson = CompressionUtils.decodeBase64AndDecompress(fileListStr);

            List<CodeFileUrlEntity> codeFileUrlEntityList = JsonUtil.INSTANCE.to(codeFileURLJson,
                    new TypeReference<List<CodeFileUrlEntity>>() {
                    });
            long currTime = System.currentTimeMillis();
            codeFileUrlEntityList.forEach(codeFileUrlEntity -> {
                String filePath = codeFileUrlEntity.getFile();
                if (StringUtils.isNotEmpty(filePath)) {
                    filePath = PathUtils.trimWinPathPrefix(filePath);
                    codeFileUrlEntity.setFile(filePath);
                }
                codeFileUrlEntity.setUrl(PathUtils.formatFileRepoUrlToHttp(codeFileUrlEntity.getUrl()));
                codeFileUrlEntity.setUpdatedDate(currTime);
            });

            codeFileUrlDao.upsert(uploadTaskLogStepVO.getTaskId(), codeFileUrlEntityList);
        }
    }

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4Cov.DEFECT_SYNS.value();
    }

    @Override
    public int getCodeDownloadStepNum() {
        return ComConstants.Step4Cov.UPLOAD.value();
    }

    /**
     * 发送websocket信息
     *
     * @param toolConfigBaseVO
     * @param uploadTaskLogStepVO
     * @param taskId
     * @param toolName
     */
    @Override
    protected void sendWebSocketMsg(
            ToolConfigBaseVO toolConfigBaseVO, UploadTaskLogStepVO uploadTaskLogStepVO,
            TaskLogEntity taskLogEntity, TaskDetailVO taskDetailVO, long taskId, String toolName) {
        // 1. 推送消息至任务详情首页面
        TaskOverviewVO.LastAnalysis lastAnalysis =
                assembleAnalysisResult(toolConfigBaseVO, uploadTaskLogStepVO, toolName);
        //获取告警数量信息
        if (Step4Cov.COMPLETE.value() == uploadTaskLogStepVO.getStepNum()
                && StepFlag.SUCC.value() == uploadTaskLogStepVO.getFlag()) {
            ToolLastAnalysisResultVO toolLastAnalysisResultVO = new ToolLastAnalysisResultVO();
            toolLastAnalysisResultVO.setTaskId(taskId);
            toolLastAnalysisResultVO.setToolName(toolName);
            BaseLastAnalysisResultVO lastAnalysisResultVO =
                    taskLogService.getLastAnalysisResult(toolLastAnalysisResultVO, toolName);
            lastAnalysis.setLastAnalysisResult(lastAnalysisResultVO);
        }

        TaskLogVO taskLogVO = new TaskLogVO();
        BeanUtils.copyProperties(taskLogEntity, taskLogVO, "stepArray");
        List<TaskLogEntity.TaskUnit> stepArrayEntity = taskLogEntity.getStepArray();
        List<TaskLogVO.TaskUnit> stepArrayVO = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(stepArrayEntity)) {
            stepArrayVO = stepArrayEntity.stream().map(taskUnit -> {
                TaskLogVO.TaskUnit taskUnitVO = new TaskLogVO.TaskUnit();
                BeanUtils.copyProperties(taskUnit, taskUnitVO);
                return taskUnitVO;
            }).collect(Collectors.toList());
        }
        taskLogVO.setStepArray(stepArrayVO);
        TaskLogOverviewVO taskLogOverviewVO = taskLogOverviewService.getTaskLogOverview(taskId,
                uploadTaskLogStepVO.getPipelineBuildId(),
                null);

        List<TaskLogVO> taskLogVOList = new ArrayList<>();
        BaseDataVO orderToolIds = baseDataCacheService.getToolOrder();
        List<String> toolOrderList = orderToolIds != null && StringUtils.isNotBlank(orderToolIds.getParamValue())
                ? Arrays.asList(orderToolIds.getParamValue().split(",")) : new ArrayList<>();
        if (taskLogOverviewVO != null && taskLogOverviewVO.getTaskLogVOList() != null) {
            taskLogVOList = taskLogOverviewVO.getTaskLogVOList();
            // 工具展示顺序排序
            taskLogOverviewVO.getTaskLogVOList()
                    .sort(Comparator.comparingInt(it -> toolOrderList.contains(it.getToolName())
                            ? toolOrderList.indexOf(it.getToolName()) : Integer.MAX_VALUE));
        }
        taskLogVOList.removeIf(it -> it.getToolName().equals(taskLogVO.getToolName()));
        taskLogVOList.add(taskLogVO);

        assembleTaskInfo(uploadTaskLogStepVO, taskDetailVO, taskLogEntity);

        WebsocketDTO websocketDTO = new WebsocketDTO(taskLogVO, lastAnalysis, taskDetailVO, taskLogOverviewVO);
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_CODECCJOB_TASKLOG_WEBSOCKET, "",
                websocketDTO);
    }


    /**
     * 保存快照信息
     *
     * @param uploadTaskLogStepVO
     */
    @Deprecated
    public void saveBuildDefects(UploadTaskLogStepVO uploadTaskLogStepVO) {
        long taskId = uploadTaskLogStepVO.getTaskId();
        String toolName = uploadTaskLogStepVO.getToolName();
        List<CommonDefectEntity> defectList = defectRepository.findIdByTaskIdAndToolNameAndStatus(
                taskId, toolName, DefectStatus.NEW.value());

        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        List<BuildDefectEntity> insertList = Lists.newArrayList();

        for (CommonDefectEntity commonDefectEntity : defectList) {
            BuildDefectEntity buildDefectEntity = new BuildDefectEntity();
            buildDefectEntity.setTaskId(taskId);
            buildDefectEntity.setToolName(toolName);
            buildDefectEntity.setBuildId(uploadTaskLogStepVO.getPipelineBuildId());
            buildDefectEntity.setDefectId(commonDefectEntity.getId());
            insertList.add(buildDefectEntity);
        }

        buildDefectRepository.saveAll(insertList);
    }
}
