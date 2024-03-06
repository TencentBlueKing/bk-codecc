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
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.component.DefectConsumerRetryLimitComponent;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectConsumerType;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 告警提交消息队列的消费者抽象类
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Slf4j
public abstract class AbstractFastIncrementConsumer implements IConsumer<AnalyzeConfigInfoVO> {

    protected static final Set<Integer> STATUS_NEW_FIXED_SET = Stream.of(
            DefectStatus.NEW.value(),
            DefectStatus.NEW.value() | DefectStatus.FIXED.value()
    ).collect(Collectors.toSet());

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
    protected ScmFileInfoService scmFileInfoService;
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;
    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private DefectConsumerRetryLimitComponent defectConsumerRetryLimitComponent;

    /**
     * 告警提交
     *
     * @param analyzeConfigInfoVO
     */
    @EndReport(isOpenSource = false)
    @Override
    public void consumer(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long beginTime = System.currentTimeMillis();
        try {
            log.info("fast increment generate result! {}", analyzeConfigInfoVO);
            process(analyzeConfigInfoVO);
        } catch (Throwable e) {
            log.error("fast increment generate result fail!", e);
        }
        log.info("end fast increment generate result cost: {}", System.currentTimeMillis() - beginTime);
    }

    protected void process(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        // 检查是否已经达到限制
        Boolean checkResult = defectConsumerRetryLimitComponent.checkIfReachRetryLimit(
                analyzeConfigInfoVO.getTaskId(),
                analyzeConfigInfoVO.getBuildId(),
                analyzeConfigInfoVO.getMultiToolType(),
                DefectConsumerType.FAST_INCREMENT,
                JSONObject.toJSONString(analyzeConfigInfoVO)
        );
        if (checkResult) {
            // 达到限制，不继续
            log.error("reach retry limit. taskId:{}. buildId:{}. toolName:{}.", analyzeConfigInfoVO.getTaskId(),
                    analyzeConfigInfoVO.getBuildId(), analyzeConfigInfoVO.getMultiToolType());
            return;
        }
        // 排队开始
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.QUEUE.value(),
                ComConstants.StepFlag.PROCESSING.value(),
                System.currentTimeMillis(),
                0,
                null,
                false);

        // 排队结束
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.QUEUE.value(),
                ComConstants.StepFlag.SUCC.value(),
                0,
                System.currentTimeMillis(),
                null,
                false);

        // 下载开始
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.DOWNLOAD.value(),
                ComConstants.StepFlag.PROCESSING.value(),
                System.currentTimeMillis(),
                0,
                null,
                false);

        // 下载结束
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.DOWNLOAD.value(),
                ComConstants.StepFlag.SUCC.value(),
                0,
                System.currentTimeMillis(),
                null,
                false);

        // 扫描开始
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.SCAN.value(),
                ComConstants.StepFlag.PROCESSING.value(),
                System.currentTimeMillis(),
                0,
                null,
                false);

        // 扫描结束
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.SCAN.value(),
                ComConstants.StepFlag.SUCC.value(),
                0,
                System.currentTimeMillis(),
                null,
                false);

        // 生成问题开始
        uploadTaskLog(analyzeConfigInfoVO,
                ComConstants.Step4MutliTool.COMMIT.value(),
                ComConstants.StepFlag.PROCESSING.value(),
                System.currentTimeMillis(),
                0,
                null,
                false);

        try {
            // 生成当前遗留告警的统计信息
            generateResult(analyzeConfigInfoVO);

            // 保存代码库信息
            upsertCodeRepoInfo(analyzeConfigInfoVO);
        } catch (Throwable e) {
            e.printStackTrace();
            log.error("fast increment generate result fail!", e);
            // 发送提单失败的分析记录
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4MutliTool.COMMIT.value(),
                    ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getMessage(), false);
            return;
        }

        // 生成问题结束
        uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4MutliTool.COMMIT.value(),
                ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, true);
    }

    /**
     * 生成结果
     *
     * @param analyzeConfigInfoVO
     */
    protected abstract void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO);

    /**
     * 发送分析记录
     *
     * @param analyzeConfigInfoVO
     * @param stepFlag
     * @param msg
     */
    protected void uploadTaskLog(AnalyzeConfigInfoVO analyzeConfigInfoVO,
            int stepNum,
            int stepFlag,
            long startTime,
            long endTime,
            String msg,
            boolean isFinish) {
        UploadTaskLogStepVO uploadTaskLogStepVO = new UploadTaskLogStepVO();
        uploadTaskLogStepVO.setTaskId(analyzeConfigInfoVO.getTaskId());
        uploadTaskLogStepVO.setStreamName(analyzeConfigInfoVO.getNameEn());
        uploadTaskLogStepVO.setToolName(analyzeConfigInfoVO.getMultiToolType());
        uploadTaskLogStepVO.setStartTime(startTime);
        uploadTaskLogStepVO.setEndTime(endTime);
        uploadTaskLogStepVO.setFlag(stepFlag);
        uploadTaskLogStepVO.setMsg(msg);
        uploadTaskLogStepVO.setStepNum(stepNum);
        uploadTaskLogStepVO.setPipelineBuildId(analyzeConfigInfoVO.getBuildId());
        uploadTaskLogStepVO.setFastIncrement(true);
        uploadTaskLogStepVO.setFinish(isFinish);
        thirdPartySystemCaller.uploadTaskLog(uploadTaskLogStepVO);
    }

    protected void upsertCodeRepoInfo(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String buildId = analyzeConfigInfoVO.getBuildId();

        // 校验构建号对应的仓库信息是否已存在
        CodeRepoInfoEntity codeRepoInfo = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        if (codeRepoInfo == null) {
            // 更新仓库列表和构建ID
            List<CodeRepoEntity> codeRepoEntities = Lists.newArrayList();
            List<CodeRepoVO> codeRepoList = analyzeConfigInfoVO.getCodeRepos();
            if (CollectionUtils.isNotEmpty(codeRepoList)) {
                for (CodeRepoVO codeRepoVO : codeRepoList) {
                    CodeRepoEntity codeRepoEntity = new CodeRepoEntity();
                    BeanUtils.copyProperties(codeRepoVO, codeRepoEntity);
                    codeRepoEntities.add(codeRepoEntity);
                }
            }
            codeRepoInfo = new CodeRepoInfoEntity(taskId, buildId, codeRepoEntities,
                    analyzeConfigInfoVO.getRepoWhiteList(), null, analyzeConfigInfoVO.getRepoRelativePathList());
            Long currentTime = System.currentTimeMillis();
            codeRepoInfo.setUpdatedDate(currentTime);
            codeRepoInfo.setCreatedDate(currentTime);
            codeRepoRepository.save(codeRepoInfo);
        }
    }
}
