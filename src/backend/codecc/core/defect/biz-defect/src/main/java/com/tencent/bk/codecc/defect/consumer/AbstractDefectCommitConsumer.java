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

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.defect.cache.ConcurrentDefectTracingConfigCache;
import com.tencent.bk.codecc.defect.component.DefectConsumerRetryLimitComponent;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.FilterPathService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectConsumerType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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


    /**
     * 告警提交
     *
     * @param commitDefectVO
     */
    @EndReport(isOpenSource = false)
    public void commitDefect(CommitDefectVO commitDefectVO) {
        long beginTime = System.currentTimeMillis();
        log.info("commit defect! {}", commitDefectVO);

        try {
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
            }

            log.info("end commitDefect {}, {}", uploadSuccess, System.currentTimeMillis() - beginTime);
        } catch (Throwable e) {
            e.printStackTrace();
            log.error("commit defect fail!", e);
            // 发送提单失败的分析记录
            uploadTaskLog(commitDefectVO, ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(),
                    e.getLocalizedMessage());
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
