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

package com.tencent.bk.codecc.task.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.dao.mongorepository.AnalyzeCountStatRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolElapseTimeRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.AnalyzeCountStatEntity;
import com.tencent.bk.codecc.task.model.ToolElapseTimeEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.StatisticTaskCodeLineToolVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatType;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ACTIVE_STAT;

/**
 * 活跃统计任务,该任务主要统计不区分开闭源环境的数据落地到task库上，且每天只需统计一次
 * core/task:  ActiveStatisticConsumer    任务工具相关统计
 * ext/task:   ActiveStatisticExtConsumer 针对内部的任务工具统计
 * ext/defect: OpStatisticExtConsumer     代码库相关统计
 * @version V1.0
 * @date 2020/12/10
 */

@Slf4j
@Component
@ConditionalOnProperty(name = "cluster.tag", havingValue = "prod", matchIfMissing = true)
public class ActiveStatisticConsumer {

    @Autowired
    private Client client;
    @Autowired
    private ToolMetaRepository toolMetaRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AnalyzeCountStatRepository analyzeCountStatRepository;
    @Autowired
    private ToolElapseTimeRepository toolElapseTimeRepository;

    /**
     * 定时任务统计每日数据
     * 由于代码库在defect库，故需要传参按环境和业务范围分别统计
     * @see com.tencent.devops.common.constant.ComConstants.DefectStatType
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ACTIVE_STAT,
            value = @Queue(value = QUEUE_ACTIVE_STAT, durable = "false"),
            exchange = @Exchange(value = EXCHANGE_ACTIVE_STAT, durable = "false")))
    public void consumer(StatisticTaskCodeLineToolVO statisticVO) {
        try {
            log.info("ActiveStatisticConsumer statisticVO: {}", statisticVO);
            String date = statisticVO.getDate();
            if (StringUtils.isEmpty(date)) {
                date = DateTimeUtils.getDateByDiff(-1);
            }

            long endTime = DateTimeUtils.getTimeStampEnd(date);

            // 大部分status字段为null，故认为状态不为D的都是有效的工具
            List<ToolMetaEntity> toolMetaEntities =
                    toolMetaRepository.findByStatusIsNot(ComConstants.ToolIntegratedStatus.D.name());
            String toolOrder = toolMetaEntities.stream().map(ToolMetaEntity::getName)
                    .collect(Collectors.joining(ComConstants.COMMA));

            // 获取需要统计的dataFrom
            List<ComConstants.DefectStatType> dataFromList = statisticVO.getDataFromList();
            if (CollectionUtils.isEmpty(dataFromList)) {
                log.info("dataFromList is empty, statistic all type!");
                dataFromList = Arrays.stream(DefectStatType.values()).collect(Collectors.toList());
            }

            // 2.分页获取任务ID 统计任务数、工具数、分析代码行
            pageTaskIdStatistic(endTime, date, toolOrder, dataFromList);
            log.info("pageTaskIdStatistic finish.");

            String finalDate = date;
            dataFromList.forEach(dataFrom -> {
                // 3.保存执行分析的任务ID来统计执行次数
                statisticToolAnalyzeCount(finalDate, dataFrom, toolOrder);

                // 4.统计工具分析耗时
                toolAnalyzeElapseTimeStat(finalDate, dataFrom, toolOrder);
            });

            log.info("ActiveStatistic finish.");
        } catch (Throwable e) {
            log.error("ActiveStatisticConsumer error", e);
        }
    }

    /**
     * 分页获取任务ID 定时统计任务数、工具数、分析代码行
     *
     * @param endTime   截止时间戳
     * @param date      统计日期
     * @param toolOrder 工具列表
     */
    private void pageTaskIdStatistic(long endTime, String date, String toolOrder, List<DefectStatType> dataFromList) {
        log.info("pageTaskIdStatistic endTime: [{}], date: [{}], toolOrder:[{}]", endTime, date, toolOrder);

        // 设置请求参数
        StatisticTaskCodeLineToolVO reqVO = new StatisticTaskCodeLineToolVO();
        reqVO.setEndTime(endTime);
        reqVO.setDate(date);
        reqVO.setToolOrder(toolOrder);
        reqVO.setDataFromList(dataFromList);

        // 触发异步统计任务
        client.get(ServiceTaskRestResource.class).statisticTaskCodeLineTool(reqVO);
    }

    /**
     * 组装分析成功次数的key
     */
    private String getToolAnalyzeSuccStatKey(String date, String dataFrom, String toolName) {
        return String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_COUNT, date, dataFrom, toolName);
    }

    /**
     * 组装分析失败次数的key
     */
    private String getToolAnalyzeFailStatKey(String date, String dataFrom, String toolName) {
        return String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_COUNT, date, dataFrom, toolName);
    }

    /**
     * 保存执行分析的任务ID来统计执行次数
     */
    private void statisticToolAnalyzeCount(String date, DefectStatType dataFrom, String toolOrder) {
        if (StringUtils.isEmpty(toolOrder)) {
            log.error("toolOrder is empty!");
            return;
        }
        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);
        // 获取分析成功次数
        saveAnalyzeCounts(date, dataFrom, toolArr, true);
        // 获取分析失败次数
        saveAnalyzeCounts(date, dataFrom, toolArr, false);
    }

    /**
     * 按工具保存分析次数
     *
     * @param isSuccess true:统计成功状态,false:统计失败状态
     */
    private void saveAnalyzeCounts(String date, DefectStatType dataFrom, @NotNull String[] toolArr, boolean isSuccess) {
        try {
            Integer currentStatStatus =
                    isSuccess ? ComConstants.ScanStatus.SUCCESS.getCode() : ComConstants.ScanStatus.FAIL.getCode();
            // 获取已统计过的数据
            Map<String, AnalyzeCountStatEntity> sourceEntityMap =
                    analyzeCountStatRepository.findByDateAndDataFromAndStatus(date, dataFrom.value(), currentStatStatus)
                            .stream().collect(Collectors.toMap(obj ->
                                    // 同date、dataFrom内的数据，工具 + status 是唯一的
                                    obj.getToolName() + obj.getStatus(), Function.identity(), (k, v) -> k));

            List<AnalyzeCountStatEntity> analyzeCountStatEntities = Lists.newArrayList();
            for (String tool : toolArr) {
                // 组装key
                String redisKey = isSuccess ? getToolAnalyzeSuccStatKey(date, dataFrom.value(), tool)
                        : getToolAnalyzeFailStatKey(date, dataFrom.value(), tool);

                List<String> taskIdStrList = Optional.ofNullable(redisTemplate.opsForList().range(redisKey, 0, -1))
                        .orElse(Collections.emptyList());

                AnalyzeCountStatEntity countStatEntity = sourceEntityMap.get(tool + currentStatStatus);
                if (countStatEntity == null) {
                    countStatEntity = new AnalyzeCountStatEntity();
                    countStatEntity.setDate(date);
                    countStatEntity.setStatus(currentStatStatus);
                    countStatEntity.setDataFrom(dataFrom.value());
                    countStatEntity.setToolName(tool);
                }

                countStatEntity.setTotalCount(taskIdStrList.size());
                countStatEntity.setTaskIdList(taskIdStrList.stream().map(Long::parseLong).collect(Collectors.toList()));
                analyzeCountStatEntities.add(countStatEntity);
            }
            analyzeCountStatRepository.saveAll(analyzeCountStatEntities);
        } catch (Throwable throwable) {
            log.error("saveAnalyzeCounts error", throwable);
        }
    }

    /**
     * 组装工具分析耗时key
     */
    private String getToolAnalyzeSuccElapseTimeKey(String date, String dataFrom, String scanStatType) {
        return String
                .format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_ELAPSE_TIME, date, dataFrom, scanStatType);
    }

    /**
     * 按是否超快增量统计分析耗时
     *
     * @param date      统计日期
     * @param dataFrom  数据来源
     * @param toolOrder 工具顺序
     */
    private void toolAnalyzeElapseTimeStat(String date, DefectStatType dataFrom, String toolOrder) {
        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);

        List<ToolElapseTimeEntity> toolElapseTimeEntities = Lists.newArrayList();
        for (ComConstants.ScanStatType scanStatType : ComConstants.ScanStatType.values()) {
            // 获取各个工具的总耗时
            String elapseTimeKey = getToolAnalyzeSuccElapseTimeKey(date, dataFrom.value(), scanStatType.getValue());
            Map<Object, Object> toolElapseTimeMap = redisTemplate.opsForHash().entries(elapseTimeKey);
            if (toolElapseTimeMap == null) {
                toolElapseTimeMap = Maps.newHashMap();
            }

            // 成功分析次数
            String succToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL, date,
                    dataFrom.value(), scanStatType.getValue());
            Map<Object, Object> toolSuccCountMap = redisTemplate.opsForHash().entries(succToolKey);
            if (toolSuccCountMap == null) {
                toolSuccCountMap = Maps.newHashMap();
            }

            // 失败分析次数
            String failToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL, date,
                    dataFrom.value(), scanStatType.getValue());
            Map<Object, Object> toolFailCountMap = redisTemplate.opsForHash().entries(failToolKey);
            if (toolFailCountMap == null) {
                toolFailCountMap = Maps.newHashMap();
            }

            Map<String, ToolElapseTimeEntity> currentToolElapseTimeMap =
                    toolElapseTimeRepository.findByDateAndDataFromAndScanStatType(date, dataFrom.value(),
                            scanStatType.getValue()).stream().collect(
                            Collectors.toMap(ToolElapseTimeEntity::getToolName, Function.identity(), (k, v) -> k));

            for (String tool : toolArr) {
                ToolElapseTimeEntity elapseTimeEntity = currentToolElapseTimeMap.get(tool);
                if (elapseTimeEntity == null) {
                    elapseTimeEntity = new ToolElapseTimeEntity();
                    elapseTimeEntity.setDate(date);
                    elapseTimeEntity.setToolName(tool);
                    elapseTimeEntity.setDataFrom(dataFrom.value());
                    elapseTimeEntity.setScanStatType(scanStatType.getValue());
                }

                String elapseTimeStr = toolElapseTimeMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setTotalElapseTime(Long.parseLong(elapseTimeStr));

                String succCount = toolSuccCountMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setSuccAnalyzeCount(Long.parseLong(succCount));

                String failCount = toolFailCountMap.getOrDefault(tool, "0").toString();
                elapseTimeEntity.setFailAnalyzeCount(Long.parseLong(failCount));

                toolElapseTimeEntities.add(elapseTimeEntity);
            }
        }

        toolElapseTimeRepository.saveAll(toolElapseTimeEntities);
    }

    /**
     * 定时任务 初始化每日新增代码库/代码分支数
     * 2025-2-7: defect库统计的数据单独统计，方便区分闭源环境和主环境
     * @see com.tencent.bk.codecc.defect.consumer.OpStatisticExtConsumer
     */
    @Deprecated
    private void codeRepoStatDaily(StatisticTaskCodeLineToolVO statisticVO) {
        List<DefectStatType> dataFromList = statisticVO.getDataFromList();
        if (CollectionUtils.isEmpty(dataFromList)) {
            log.warn("codeRepoStatDaily dataFromList is empty");
            return;
        }

        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        // 传递参数 1 表示查询前一天的数据进行初始化
        reqVO.setInitDay(1);
        // 主环境的类型
        reqVO.setCreateFrom(dataFromList.stream().map(DefectStatType::value).collect(Collectors.toList()));
        Boolean isSuccess = client.getWithoutRetry(OpDefectRestResource.class).initCodeRepoStatTrend(reqVO).getData();
        if (Boolean.FALSE.equals(isSuccess)) {
            log.error("query code repo stat daily fail!");
        }
    }
}
