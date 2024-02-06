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
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.CommonDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildDefectService;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.impl.CommonAnalyzeTaskBizServiceImpl;
import com.tencent.bk.codecc.defect.service.impl.redline.CompileRedLineReportServiceImpl;
import com.tencent.bk.codecc.defect.service.statistic.CommonDefectStatisticServiceImpl;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.web.aop.annotation.EndReport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Lint告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CommonFastIncrementConsumer extends AbstractFastIncrementConsumer
{

    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private BuildDefectService buildDefectService;
    @Autowired
    private CommonStatisticRepository commonStatisticRepository;
    @Autowired
    @Qualifier("CommonAnalyzeTaskBizService")
    private CommonAnalyzeTaskBizServiceImpl commonAnalyzeTaskBizService;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private CommonDefectStatisticServiceImpl commonDefectStatisticService;
    @Autowired
    private CompileRedLineReportServiceImpl compileRedLineReportServiceImpl;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private DefectConverter defectConverter;
    @Autowired
    private BaseDataCacheService baseDataCacheService;

    /**
     * 告警提交
     *
     * @param analyzeConfigInfoVO
     */
    @EndReport(isOpenSource = false)
    @Override
    public void consumer(AnalyzeConfigInfoVO analyzeConfigInfoVO)
    {
        long beginTime = System.currentTimeMillis();
        try
        {
            log.info("fast increment generate result! {}", analyzeConfigInfoVO);

            // 构建开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 构建结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.UPLOAD.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 排队开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 排队结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.QUEUE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 扫描开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 扫描结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.ANALYZE.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 提交开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            // 提交结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.COMMIT.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, false);

            // 生成问题开始
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.PROCESSING.value(), System.currentTimeMillis(), 0, null, false);

            try
            {
                // 生成当前遗留告警的统计信息
                generateResult(analyzeConfigInfoVO);

                // 保存代码库信息
                upsertCodeRepoInfo(analyzeConfigInfoVO);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                log.error("fast increment generate result fail!", e);
                // 发送提单失败的分析记录
                uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.FAIL.value(), 0, System.currentTimeMillis(), e.getMessage(), false);
                return;
            }

            // 生成问题结束
            uploadTaskLog(analyzeConfigInfoVO, ComConstants.Step4Cov.DEFECT_SYNS.value(), ComConstants.StepFlag.SUCC.value(), 0, System.currentTimeMillis(), null, true);
        }
        catch (Throwable e)
        {
            log.error("fast increment generate result fail!", e);
        }
        log.info("end fast increment generate result cost: {}", System.currentTimeMillis() - beginTime);
    }

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String streamName = analyzeConfigInfoVO.getNameEn();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        List<CommonDefectEntity> allNewDefectList;
        boolean isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);

        if (isMigrationSuccessful) {
            allNewDefectList = defectConverter.lintToCommon(
                    lintDefectV2Repository.findNoneInstancesFieldByTaskIdAndToolNameAndStatus(
                            taskId,
                            toolName,
                            ComConstants.DefectStatus.NEW.value()
                    )
            );
        } else {
            allNewDefectList = defectRepository.findNoneInstancesFieldByTaskIdAndToolNameAndStatus(
                    taskId,
                    toolName,
                    ComConstants.DefectStatus.NEW.value()
            );
        }

        Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
        List<CommonDefectEntity> allIgnoreDefectList =
                defectDao.findIgnoreDefectForSnapshot(taskId, toolName, historyIgnoreType);

        CommonDefectStatisticModel statisticModel =
                commonDefectStatisticService.statistic(new DefectStatisticModel<>(taskVO,
                        toolName,
                        0,
                        buildId,
                        null,
                        allNewDefectList,
                        null,
                        null,
                        Lists.newArrayList(),
                        true
                )
        );

        CommonStatisticEntity statisticEntity = statisticModel.getBuilder().convert();
        statisticEntity.setCheckerStatistic(getCheckerStatistic(toolName, allNewDefectList));
        commonStatisticRepository.save(statisticEntity);

        // 将数据加入数据平台
        // commonKafkaClient.pushCommonStatisticToKafka(statisticEntity);

        // 保存本次构建遗留告警告警列表快照
        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        buildSnapshotService.saveCommonBuildDefect(taskId, toolName, buildEntity, allNewDefectList,
                allIgnoreDefectList);

        // 改由MQ汇总发送 {@link EmailNotifyServiceImpl#sendWeChatBotRemind(RtxNotifyModel, TaskInfoEntity)}
        // 发送群机器人通知
        // commonAnalyzeTaskBizService.sendBotRemind(taskVO, statisticEntity, toolName);

        // 保存质量红线数据
        compileRedLineReportServiceImpl.saveRedLineData(taskVO, toolName, buildId, allNewDefectList);
    }

    private List<CheckerStatisticEntity> getCheckerStatistic(
            String toolName, List<CommonDefectEntity> allCommonDefectEntityList) {
        // get checker map
        Set<String> checkerIds = allCommonDefectEntityList.stream()
            .map(CommonDefectEntity::getChecker).collect(Collectors.toSet());
        Map<String, CheckerDetailEntity> checkerDetailMap = new HashMap<>();
        checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerIds)
            .forEach(it -> checkerDetailMap.put(it.getCheckerKey(), it));

        // get lint checker statistic data
        Map<String, CheckerStatisticEntity> checkerStatisticEntityMap = new HashMap<>();
        for (CommonDefectEntity entity: allCommonDefectEntityList) {
            if (ComConstants.DefectStatus.NEW.value() != entity.getStatus()) {
                continue;
            }

            CheckerStatisticEntity item = checkerStatisticEntityMap.get(entity.getChecker());
            if (item == null) {
                item = new CheckerStatisticEntity();
                item.setName(entity.getChecker());

                CheckerDetailEntity checker = checkerDetailMap.get(entity.getChecker());
                if (checker != null) {
                    item.setId(checker.getEntityId());
                    item.setName(checker.getCheckerName());
                    item.setSeverity(checker.getSeverity());
                } else {
                    log.warn("not found checker for tool: {}, {}", toolName, entity.getChecker());
                }
            }
            item.setDefectCount(item.getDefectCount() + 1);
            checkerStatisticEntityMap.put(entity.getChecker(), item);
        }
        return new ArrayList<>(checkerStatisticEntityMap.values());
    }
}
