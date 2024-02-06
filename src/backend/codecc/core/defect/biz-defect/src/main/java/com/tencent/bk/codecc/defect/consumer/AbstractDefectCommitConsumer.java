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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN;

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.defect.cache.ConcurrentDefectTracingConfigCache;
import com.tencent.bk.codecc.defect.component.DefectConsumerRetryLimitComponent;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.FilterPathService;
import com.tencent.bk.codecc.defect.service.HotColdDataSeparationService;
import com.tencent.bk.codecc.defect.service.ReallocateDefectAuthorService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.DefectConsumerType;
import com.tencent.devops.common.constant.ComConstants.TaskStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 告警提交消息队列的消费者抽象类
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Slf4j
public abstract class AbstractDefectCommitConsumer extends AbstractDefectCommitOnLock {

    /**
     * 流式分批告警处理每批最大处理数
     */
    protected static final int MAX_PER_BATCH = 15000;

    @Autowired
    public ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    public ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    public ScmJsonComponent scmJsonComponent;
    @Autowired
    public BuildService buildService;
    @Autowired
    public ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    public ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    public AuthTaskService authTaskService;
    @Autowired
    public RabbitTemplate rabbitTemplate;
    @Autowired
    public TransferAuthorRepository transferAuthorRepository;
    @Autowired
    public FilterPathService filterPathService;
    @Autowired
    protected ScmFileInfoService scmFileInfoService;
    @Autowired
    protected ConcurrentDefectTracingConfigCache concurrentDefectTracingConfigCache;
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;
    @Autowired
    private DefectConsumerRetryLimitComponent defectConsumerRetryLimitComponent;
    @Autowired
    private ReallocateDefectAuthorService reallocateDefectAuthorService;
    @Autowired
    private Client client;
    @Autowired
    private HotColdDataSeparationService hotColdDataSeparationService;

    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    @EndReport(isOpenSource = false)
    public void commitDefect(CommitDefectVO commitDefectVO) {
        if (commitDefectVO == null) {
            return;
        }

        long taskId = commitDefectVO.getTaskId();
        long beginTime = System.currentTimeMillis();
        log.info("commit defect! {}", commitDefectVO);

        boolean isReallocate = false;
        // 处理人重新分配分配中缓存，以便在任务运行op修改操作提示
        String redisKeyForAuthorReassign = String.format(
                RedisKeyConstants.IS_REALLOCATE + ":%d:%s", taskId, commitDefectVO.getToolName());
        try {
            boolean isBlock = isBlockByTaskId(taskId);
            if (isBlock) {
                uploadTaskLogForFail(commitDefectVO, "block by task id");
                return;
            }

            boolean mustWarmUpColdData = hotColdDataSeparationService.warmUpColdDataIfNecessary(taskId);
            if (mustWarmUpColdData) {
                uploadTaskLogForFail(commitDefectVO, "cold data must warm up, please try again after 10~15 minutes");
                return;
            }

            // 若是首次出队，则发送开始提单的分析记录
            if (commitDefectVO.getRecommitTimes() == null) {
                uploadTaskLog(
                        commitDefectVO,
                        ComConstants.StepFlag.PROCESSING.value(),
                        System.currentTimeMillis(),
                        0,
                        null
                );
            }

            // 获取文件作者信息
            Map<String, ScmBlameVO> fileChangeRecordsMap = getAuthorInfo(commitDefectVO);

            // 获取仓库信息
            Map<String, RepoSubModuleVO> codeRepoIdMap = getRepoInfo(commitDefectVO);

            isReallocate = reallocateDefectAuthorService.isReallocate(taskId, commitDefectVO.getToolName());

            if (isReallocate) {
                redisTemplate.opsForValue().setIfAbsent(
                        redisKeyForAuthorReassign, "reallocating", 1, TimeUnit.HOURS);
            }
            commitDefectVO.setReallocate(isReallocate);
            // 解析工具上报的告警文件并入库
            boolean uploadSuccess = uploadDefects(commitDefectVO, fileChangeRecordsMap, codeRepoIdMap);
            if (uploadSuccess) {
                // 发送提单成功的分析记录
                uploadTaskLog(
                        commitDefectVO,
                        ComConstants.StepFlag.SUCC.value(),
                        0,
                        System.currentTimeMillis(),
                        commitDefectVO.getMessage()
                );
                // 更新工具是否重新分配状态
                if (isReallocate) {
                    reallocateDefectAuthorService.updateCurrentStatus(taskId, commitDefectVO.getToolName());
                }
            }
            log.info("end commitDefect, uploadSuccess: {}, task id: {}, stream name: {}, build id: {}, cost: {}",
                    uploadSuccess, taskId, commitDefectVO.getStreamName(),
                    commitDefectVO.getBuildId(), System.currentTimeMillis() - beginTime);
        } catch (Throwable e) {
            log.error("commit defect fail, task id: {}, stream name: {}, build id: {}", taskId,
                    commitDefectVO.getStreamName(), commitDefectVO.getBuildId(), e);
            uploadTaskLogForFail(commitDefectVO, e.getLocalizedMessage());
        } finally {
            if (isReallocate) {
                redisTemplate.delete(redisKeyForAuthorReassign);
            }
        }
    }

    /**
     * 校验任务Id是否被屏蔽
     *
     * @param taskId
     * @return true为屏蔽，不应执行提单操作
     */
    private boolean isBlockByTaskId(Long taskId) {
        // taskId为null的非法数据直接block
        if (taskId == null || taskId == 0L) {
            return true;
        }

        try {
            TaskDetailVO taskDetailVO = client.getWithoutRetry(ServiceTaskRestResource.class)
                    .getTaskInfoWithoutToolsByTaskId(taskId)
                    .getData();

            // 校验黑名单列表
            String taskIdStr = redisTemplate.opsForValue().get(RedisKeyConstants.COMMIT_DEFECT_TASK_ID_BLOCK_LIST);
            if (StringUtils.isNotEmpty(taskIdStr)) {
                Set<Long> blockSet = Stream.of(taskIdStr.split(","))
                        .filter(StringUtils::isNotEmpty)
                        .map(y -> Long.valueOf(y.trim()))
                        .collect(Collectors.toSet());

                if (blockSet.contains(taskId)) {
                    return true;
                }
            }

            // 任务停用的也block
            return taskDetailVO == null;
        } catch (Throwable t) {
            log.error("isBlockByTaskId fail, task id: {}", taskId, t);

            // 校验期间异常，默认放行
            return false;
        }
    }

    private void uploadTaskLogForFail(CommitDefectVO commitDefectVO, String failMessage) {
        try {
            // 发送提单失败的分析记录
            uploadTaskLog(
                    commitDefectVO, ComConstants.StepFlag.FAIL.value(),
                    0, System.currentTimeMillis(), failMessage
            );
        } catch (Throwable t) {
            // 上报失败信息，往往已经在上层的catch的逻辑里，不应再抛出异常
            log.error("uploadTaskLogForFail fail, {}", commitDefectVO, t);
        }
    }

    protected void checkIfReachRetryLimit(CommitDefectVO commitDefectVO) {
        // 检查是否已经达到限制
        boolean checkResult = defectConsumerRetryLimitComponent.checkIfReachRetryLimit(
                commitDefectVO.getTaskId(),
                commitDefectVO.getBuildId(),
                commitDefectVO.getToolName(),
                DefectConsumerType.DEFECT_COMMIT,
                JSONObject.toJSONString(commitDefectVO)
        );
        if (checkResult) {
            // 达到限制，不继续
            log.info("reach retry limit. taskId:{}. buildId:{}. toolName:{}.", commitDefectVO.getTaskId(),
                    commitDefectVO.getBuildId(), commitDefectVO.getToolName());
            throw new CodeCCException(CommonMessageCode.REACH_LIMIT, "重试次数达到限制");
        }
    }

    /**
     * 解析工具上报的告警文件并入库
     *
     * @param commitDefectVO
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    protected abstract boolean uploadDefects(
            CommitDefectVO commitDefectVO,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap
    );

    protected Map<String, ScmBlameVO> getAuthorInfo(CommitDefectVO commitDefectVO) {
        return scmFileInfoService.loadAuthorInfoMap(
                commitDefectVO.getTaskId(),
                commitDefectVO.getStreamName(),
                commitDefectVO.getToolName(),
                commitDefectVO.getBuildId());
    }

    protected Map<String, RepoSubModuleVO> getRepoInfo(CommitDefectVO commitDefectVO) {
        return scmFileInfoService.loadRepoInfoMap(
                commitDefectVO.getStreamName(),
                commitDefectVO.getToolName(),
                commitDefectVO.getBuildId());
    }

    @NotNull
    public Map<Integer, ScmBlameChangeRecordVO> getLineAuthorMap(List<ScmBlameChangeRecordVO> changeRecords) {
        Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = new HashMap<>();
        for (ScmBlameChangeRecordVO changeRecord : changeRecords) {
            List<Object> lines = changeRecord.getLines();
            if (CollectionUtils.isNotEmpty(lines)) {
                for (Object line : lines) {
                    if (line instanceof Integer) {
                        lineAuthorMap.put((int) line, changeRecord);
                    } else {
                        if (line instanceof List) {
                            List<Integer> lineScope = (List<Integer>) line;
                            for (int i = lineScope.get(0); i <= lineScope.get(1); i++) {
                                lineAuthorMap.put(i, changeRecord);
                            }
                        }
                    }
                }
            }
        }
        return lineAuthorMap;
    }

    /**
     * 发送分析记录
     *
     * @param commitDefectVO
     * @param stepFlag
     * @param msg
     */
    protected void uploadTaskLog(
            CommitDefectVO commitDefectVO,
            int stepFlag,
            long startTime,
            long endTime,
            String msg
    ) {
        UploadTaskLogStepVO reqVO = new UploadTaskLogStepVO();
        reqVO.setTaskId(commitDefectVO.getTaskId());
        reqVO.setStreamName(commitDefectVO.getStreamName());
        reqVO.setToolName(commitDefectVO.getToolName());
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        reqVO.setFlag(stepFlag);
        reqVO.setMsg(msg);
        reqVO.setStepNum(ComConstants.Step4MutliTool.COMMIT.value());
        reqVO.setPipelineBuildId(commitDefectVO.getBuildId());
        reqVO.setTriggerFrom(commitDefectVO.getTriggerFrom());
        reqVO.setRecommitTimes(commitDefectVO.getRecommitTimes());

        thirdPartySystemCaller.uploadTaskLog(reqVO);
    }

    protected void getCodeRepoMap(long taskId, boolean isFullScan, BuildEntity buildEntity,
            Map<String, CodeRepoEntity> repoIdMap, Map<String, CodeRepoEntity> urlMap) {
        if (!isFullScan) {
            // 校验构建号对应的仓库信息是否已存在
            CodeRepoInfoEntity codeRepoInfo = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId,
                    buildEntity.getBuildId());
            if (codeRepoInfo != null && CollectionUtils.isNotEmpty(codeRepoInfo.getRepoList())) {
                repoIdMap.putAll(codeRepoInfo.getRepoList().stream().collect(Collectors.toMap(CodeRepoEntity::getRepoId,
                        Function.identity(), (k, v) -> v)));
                urlMap.putAll(codeRepoInfo.getRepoList().stream().collect(Collectors.toMap(CodeRepoEntity::getUrl,
                        Function.identity(), (k, v) -> v)));
            }
        }
    }

    /**
     * 获取限制并发的阻塞队列
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param running
     * @param isVip
     * @return
     */
    protected LinkedBlockingQueue<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> setConcurrentBlockingQueue(
            Long taskId,
            String toolName,
            String buildId,
            AtomicBoolean running,
            CountDownLatch finishLatch,
            Boolean isVip
    ) {
        LinkedBlockingQueue<AsyncRabbitTemplate.RabbitConverterFuture<Boolean>> asyncResultQueue =
                new LinkedBlockingQueue<>(concurrentDefectTracingConfigCache.getConcurrentLimit(isVip));
        new Thread(() -> {
            try {
                //标志位为真或队列不为空时，会一直get
                while (true) {
                    if (!running.get() && null == asyncResultQueue.peek()) {
                        log.info("task from blocking queue finished, {}, {}, {}", taskId, toolName, buildId);
                        break;
                    }
                    try {
                        synchronized (asyncResultQueue) {
                            AsyncRabbitTemplate.RabbitConverterFuture<Boolean> clusterResult = asyncResultQueue.poll();
                            if (null != clusterResult) {
                                /*
                                 * 对取出的异步聚类结果进行判断
                                 * 1. 如果该任务已经完成，则判断返回结果
                                 * 2. 如果该任务还未完成，则重新塞入阻塞队列
                                 */
                                if (clusterResult.isDone()) {
                                    if (null == clusterResult.get() || !clusterResult.get()) {
                                        log.info("cluster result is not true!");
                                    }
                                } else {
                                    asyncResultQueue.put(clusterResult);
                                }
                            }
                        }
                        Thread.sleep(150L);
                    } catch (InterruptedException | ExecutionException e) {
                        log.info("poll element from blocking queue fail, {}, {}, {}", taskId, toolName, buildId);
                        e.printStackTrace();
                    }
                }
            } finally {
                finishLatch.countDown();
            }
        }).start();
        return asyncResultQueue;
    }

    /**
     * 仅保留待修复告警
     *
     * @param defectList 告警列表
     * @param <T>
     */
    protected <T extends DefectEntity> void keepOnlyUnfixedDefect(List<T> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        Iterator<T> iterator = defectList.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next.getStatus() != ComConstants.DefectStatus.NEW.value()) {
                iterator.remove();
            }
        }
    }
}
