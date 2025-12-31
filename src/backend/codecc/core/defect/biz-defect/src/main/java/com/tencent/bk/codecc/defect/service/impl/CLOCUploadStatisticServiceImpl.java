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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * cloc工具上传统计数据接口
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Deprecated
@Service
@Slf4j
public class CLOCUploadStatisticServiceImpl implements CLOCUploadStatisticService
{
    @Autowired
    private Client client;

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Autowired
    private CommonKafkaClient commonKafkaClient;

    @Override
    public Result uploadStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO)
    {
        log.info("start to upload cloc statistic info!");
        Long taskId = uploadCLOCStatisticVO.getTaskId();
        Result<TaskDetailVO> taskDetailResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskDetailResult.isNotOk() || null == taskDetailResult.getData())
        {
            log.error("query task info fail when upload cloc statistic info!");
            return null;
        }
        TaskDetailVO taskDetailVO = taskDetailResult.getData();
        //先删除该task_id下面的
        clocStatisticsDao.batchDisableClocStatistic(taskId, uploadCLOCStatisticVO.getToolName());
        //更新统计信息
        List<CLOCLanguageVO> languageCodeList = uploadCLOCStatisticVO.getLanguageCodeList();
        if (CollectionUtils.isNotEmpty(languageCodeList))
        {
            Long currentTime = System.currentTimeMillis();
            languageCodeList.forEach(clocLanguageVO ->
            {
                CLOCStatisticEntity clocStatisticEntity = new CLOCStatisticEntity();
                clocStatisticEntity.setTaskId(uploadCLOCStatisticVO.getTaskId());
                clocStatisticEntity.setToolName(uploadCLOCStatisticVO.getToolName());
                clocStatisticEntity.setLanguage(clocLanguageVO.getLanguage());
                clocStatisticEntity.setSumCode(clocLanguageVO.getCodeSum());
                clocStatisticEntity.setSumBlank(clocLanguageVO.getBlankSum());
                clocStatisticEntity.setSumComment(clocLanguageVO.getCommentSum());
                clocStatisticEntity.setCreatedDate(currentTime);
                clocStatisticEntity.setUpdatedDate(currentTime);
                log.info("start to upload cloc statistic info, task id: {}, language: {}",
                        uploadCLOCStatisticVO.getTaskId(), clocLanguageVO.getLanguage());
                clocStatisticsDao.upsertCLOCStatistic(clocStatisticEntity);
            });
        }

        return new Result(CommonMessageCode.SUCCESS, "upload CLOC analysis statistic ok");
    }

    /**
     * 新增 cloc 工具 statics 信息
     * 不再以更新的方式记录 cloc 工具的统计信息，
     * 改为：根据 build_id 来记录每次构建的统计信息，
     * 之后可根据 build_id 查询到当前任务下的扫描统计历史记录
     *
     * @param buildId               当前告警上报构建ID
     * @param streamName            流名称
     * @param clocLanguageMap       按语言划分告警记录
     * @param uploadCLOCStatisticVO cloc 视图信息
     */
    @Override
    public Result<CommonMessageCode> uploadNewStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO, Map<String, List<CLOCDefectEntity>> clocLanguageMap, String buildId, String streamName)
    {
        long taskId = uploadCLOCStatisticVO.getTaskId();
        String toolName = uploadCLOCStatisticVO.getToolName();
        final List<CLOCLanguageVO> languageCodeList = uploadCLOCStatisticVO.getLanguageCodeList();
        // 获取当前task上一次构建ID
        CLOCStatisticEntity lastClocStatisticEntity =
                clocStatisticRepository.findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(taskId, toolName);
        String lastBuildId = null;
        List<CLOCStatisticEntity> lastClocStatisticEntityList = Collections.emptyList();
        if (lastClocStatisticEntity != null && StringUtils.isNotBlank(lastClocStatisticEntity.getBuildId()))
        {
            lastBuildId = lastClocStatisticEntity.getBuildId();
        }
        else if (lastClocStatisticEntity != null && StringUtils.isBlank(lastClocStatisticEntity.getBuildId()))
        {
            // 兼容旧逻辑产生的数据中没有 build_id、create_time 字段
            lastClocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolName(taskId, toolName);
        }
        log.info("get last cloc statistic buildId! taskId: {} | buildId: {} | currBuildId: {}", taskId, lastBuildId, buildId);

        // 获取当前task上次一构建的statistic记录
        if (StringUtils.isNotBlank(lastBuildId))
        {
            log.info("begin find cloc statistic info: taskId: {}, lastBuildId: {}", taskId, lastBuildId);
            lastClocStatisticEntityList = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(taskId, toolName, lastBuildId);
        }
        log.info("get last cloc statistic buildId! taskId: {} | buildId: {} | currBuildId: {}, info: {}",
                taskId, lastBuildId, buildId, lastClocStatisticEntityList.size());

        Map<String, CLOCStatisticEntity> currStatisticMap = new HashMap<>();

        long currentTime = System.currentTimeMillis();
        lastClocStatisticEntityList.forEach(stepStatistic -> {
            CLOCStatisticEntity cloctStatistic =
                    currStatisticMap.getOrDefault(stepStatistic.getLanguage(), new CLOCStatisticEntity());
            cloctStatistic.setTaskId(taskId);
            cloctStatistic.setStreamName(streamName);
            cloctStatistic.setBuildId(buildId);
            if (stepStatistic.getCreatedDate() == null) {
                cloctStatistic.setCreatedDate(currentTime);
            } else {
                cloctStatistic.setCreatedDate(stepStatistic.getCreatedDate());
            }
            cloctStatistic.setUpdatedDate(currentTime);
            cloctStatistic.setToolName(uploadCLOCStatisticVO.getToolName());
            cloctStatistic.setLanguage(stepStatistic.getLanguage());

            // 如果本次告警没有这种语言的话，代码行数全为0，如果有的话则在下面的循环中赋值
            cloctStatistic.setSumBlank(0L);
            cloctStatistic.setSumCode(0L);
            cloctStatistic.setSumComment(0L);

            // 如果本次告警没有这种语言的话，行数变化就是（上次扫描的行数）
            cloctStatistic.setBlankChange(-(stepStatistic.getSumBlank() == null ? 0 : stepStatistic.getSumBlank()));
            cloctStatistic.setCodeChange(-(stepStatistic.getSumCode() == null ? 0 : stepStatistic.getSumCode()));
            cloctStatistic.setCommentChange(-(stepStatistic.getSumComment() == null ? 0 : stepStatistic.getSumComment()));
            cloctStatistic.setFileNum(0L);
            cloctStatistic.setFileNumChange(-(stepStatistic.getFileNum() == null ? 0 : stepStatistic.getFileNum()));
            currStatisticMap.put(stepStatistic.getLanguage(), cloctStatistic);
        });

        languageCodeList.forEach(stepLanguageVO -> {
            CLOCStatisticEntity clocStatistic =
                    currStatisticMap.getOrDefault(stepLanguageVO.getLanguage(), new CLOCStatisticEntity());
            clocStatistic.setTaskId(taskId);
            clocStatistic.setStreamName(streamName);
            clocStatistic.setBuildId(buildId);
            if (clocStatistic.getCreatedDate() == null) {
                clocStatistic.setCreatedDate(currentTime);
            }
            clocStatistic.setUpdatedDate(currentTime);
            clocStatistic.setToolName(uploadCLOCStatisticVO.getToolName());
            clocStatistic.setLanguage(stepLanguageVO.getLanguage());
            clocStatistic.setSumBlank(stepLanguageVO.getBlankSum());
            clocStatistic.setSumCode(stepLanguageVO.getCodeSum());
            clocStatistic.setSumComment(stepLanguageVO.getCommentSum());
            clocStatistic.setBlankChange(stepLanguageVO.getBlankSum() + (clocStatistic.getBlankChange() == null
                    ? 0 : clocStatistic.getBlankChange()));
            clocStatistic.setCodeChange(stepLanguageVO.getCodeSum() + (clocStatistic.getCodeChange() == null
                    ? 0 : clocStatistic.getCodeChange()));
            clocStatistic.setCommentChange(stepLanguageVO.getCommentSum() + (clocStatistic.getCommentChange() == null
                    ? 0 : clocStatistic.getCommentChange()));
            clocStatistic.setFileNum((long) clocLanguageMap.get(stepLanguageVO.getLanguage()).size());
            clocStatistic.setFileNumChange((long) clocLanguageMap.get(stepLanguageVO.getLanguage()).size()
                    + (clocStatistic.getFileNumChange() == null ? 0 : clocStatistic.getFileNumChange()));
            currStatisticMap.put(stepLanguageVO.getLanguage(), clocStatistic);
        });

        log.info("start to upload new cloc statistic info, task id: {}, build id: {}",
                taskId, buildId);

        clocStatisticsDao.batchUpsertCLOCStatistic(currStatisticMap.values(),
                ComConstants.ScanStatType.NOT_FAST_INCREMENT);

        //推送数据到数据平台
        commonKafkaClient.pushCLOCStatisticToKafka(currStatisticMap.values());
        //如果本次为首次上报，且上报语言内容为空，则插入一条其他语言的记录
        if (MapUtils.isEmpty(currStatisticMap)
                && MapUtils.isEmpty(clocLanguageMap)) {
            log.info("first upload and empty upload need to insert others language,"
                            + " task id: {}, build id: {}", taskId, buildId);
            CLOCStatisticEntity clocStatisticEntity = new CLOCStatisticEntity();
            clocStatisticEntity.setTaskId(taskId);
            clocStatisticEntity.setStreamName(streamName);
            clocStatisticEntity.setBuildId(buildId);
            clocStatisticEntity.setCreatedDate(currentTime);
            clocStatisticEntity.setUpdatedDate(currentTime);
            clocStatisticEntity.setToolName(uploadCLOCStatisticVO.getToolName());
            clocStatisticEntity.setLanguage("OTHERS");
            clocStatisticEntity.setSumBlank(0L);
            clocStatisticEntity.setSumCode(0L);
            clocStatisticEntity.setSumComment(0L);
            clocStatisticEntity.setBlankChange(0L);
            clocStatisticEntity.setCodeChange(0L);
            clocStatisticEntity.setCommentChange(0L);
            clocStatisticEntity.setFileNum(0L);
            clocStatisticEntity.setFileNumChange(0L);
            clocStatisticRepository.save(clocStatisticEntity);
        }
        return new Result(CommonMessageCode.SUCCESS, "upload new defect statistic success");
    }


}
