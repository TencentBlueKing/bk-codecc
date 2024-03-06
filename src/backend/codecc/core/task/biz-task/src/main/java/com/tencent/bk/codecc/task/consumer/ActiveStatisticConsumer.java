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

package com.tencent.bk.codecc.task.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.AnalyzeCountStatRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolElapseTimeRepository;
import com.tencent.bk.codecc.task.model.AnalyzeCountStatEntity;
import com.tencent.bk.codecc.task.model.ToolElapseTimeEntity;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.StatisticTaskCodeLineToolVO;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatType;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ACTIVE_STAT;

/**
 * 活跃统计任务
 *
 * @version V1.0
 * @date 2020/12/10
 */

@Slf4j
@Component
public class ActiveStatisticConsumer {

    @Autowired
    private Client client;
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AnalyzeCountStatRepository analyzeCountStatRepository;
    @Autowired
    private ToolElapseTimeRepository toolElapseTimeRepository;

    /**
     * 定时任务统计每日数据
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ACTIVE_STAT,
            value = @Queue(value = QUEUE_ACTIVE_STAT, durable = "false"),
            exchange = @Exchange(value = EXCHANGE_ACTIVE_STAT, durable = "false")))
    public void consumer() {
        try {
            String date = DateTimeUtils.getDateByDiff(-1);
            log.info("ActiveStatistic begin date: {}", date);
            long endTime = DateTimeUtils.getTimeStampEnd(date);

            String toolOrder = commonDao.getToolOrder();

            // 分页获取任务ID 统计任务数、工具数、分析代码行
            pageTaskIdStatistic(endTime, date, toolOrder);
            log.info("pageTaskIdStatistic finish.");

            // 保存执行分析的任务ID来统计执行次数
            statisticToolAnalyzeCount(date, DefectStatType.GONGFENG_SCAN, toolOrder);
            statisticToolAnalyzeCount(date, DefectStatType.USER, toolOrder);

            // 统计工具分析耗时
            toolAnalyzeElapseTimeStat(date, DefectStatType.GONGFENG_SCAN.value(), toolOrder);
            toolAnalyzeElapseTimeStat(date, DefectStatType.USER.value(), toolOrder);
            log.info("toolAnalyzeElapseTimeStat finish.");

            // 统计每日新增代码库/代码分支数
            codeRepoStatDaily();
            log.info("codeRepoStatDaily finish.");
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
    private void pageTaskIdStatistic(long endTime, String date, String toolOrder) {
        log.info("pageTaskIdStatistic endTime: [{}], date: [{}], toolOrder:[{}]", endTime, date, toolOrder);
        // 设置来源范围
        List<DefectStatType> dataFromList = new ArrayList<>();
        dataFromList.add(DefectStatType.GONGFENG_SCAN);
        dataFromList.add(DefectStatType.USER);

        // 设置请求参数
        StatisticTaskCodeLineToolVO reqVO = new StatisticTaskCodeLineToolVO();
        reqVO.setEndTime(endTime);
        reqVO.setDate(date);
        reqVO.setToolOrder(toolOrder);
        reqVO.setDataFromList(dataFromList);

        // 防止异常影响后续任务
        try {
            log.info("start task statisticTaskCodeLineTool");
            client.get(ServiceTaskRestResource.class).statisticTaskCodeLineTool(reqVO);
            log.info("end task statisticTaskCodeLineTool, time :{}", System.currentTimeMillis());
        } catch (RuntimeException e) {
            log.error("pageTaskIdStatistic execute failure");
            e.printStackTrace();
        }

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
        List<Object> succObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeSuccStatKey(date, dataFrom.value(), tool);
                conn.lRange(redisKey.getBytes(), 0, -1);
            }
            return null;
        });
        // 获取分析失败次数
        List<Object> failObjects = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (String tool : toolArr) {
                String redisKey = getToolAnalyzeFailStatKey(date, dataFrom.value(), tool);
                try {
                    conn.lRange(redisKey.getBytes(StandardCharsets.UTF_8.name()), 0, -1);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage(), e);
                }
            }
            return null;
        });
        // 保存分析成功数据
        saveToolAnalyzeCountEntities(date, dataFrom, toolArr, succObjects, ComConstants.ScanStatus.SUCCESS.getCode());
        // 保存分析失败数据
        saveToolAnalyzeCountEntities(date, dataFrom, toolArr, failObjects, ComConstants.ScanStatus.FAIL.getCode());
    }

    /**
     * 按工具保存分析次数
     *
     * @param succObjects taskId list in redis
     * @param code        scanStatus
     */
    private void saveToolAnalyzeCountEntities(String date, DefectStatType dataFrom, @NotNull String[] toolArr,
            List<Object> succObjects, int code) {
        List<AnalyzeCountStatEntity> analyzeCountStatEntities = Lists.newArrayList();
        for (int i = 0; i < toolArr.length; i++) {
            Object listObj = succObjects.get(i);
            List<Long> taskIdList;
            if (listObj != null) {
                String listStr = listObj.toString();
                taskIdList = JsonUtil.INSTANCE.to(listStr, new TypeReference<List<Long>>() {
                });
            } else {
                taskIdList = Lists.newArrayList();
            }

            AnalyzeCountStatEntity countStatEntity = new AnalyzeCountStatEntity();
            countStatEntity.setDate(date);
            countStatEntity.setStatus(code);
            countStatEntity.setDataFrom(dataFrom.value());
            countStatEntity.setToolName(toolArr[i]);
            countStatEntity.setTaskIdList(taskIdList);

            analyzeCountStatEntities.add(countStatEntity);
        }

        analyzeCountStatRepository.saveAll(analyzeCountStatEntities);
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
    private void toolAnalyzeElapseTimeStat(String date, String dataFrom, String toolOrder) {


        String[] toolArr = toolOrder.split(ComConstants.STRING_SPLIT);

        List<ToolElapseTimeEntity> toolElapseTimeEntities = Lists.newArrayList();
        for (ComConstants.ScanStatType scanStatType : ComConstants.ScanStatType.values()) {
            // 获取各个工具的总耗时
            String elapseTimeKey = getToolAnalyzeSuccElapseTimeKey(date, dataFrom, scanStatType.getValue());
            Map<Object, Object> toolElapseTimeMap = redisTemplate.opsForHash().entries(elapseTimeKey);
            if (toolElapseTimeMap == null) {
                toolElapseTimeMap = Maps.newHashMap();
            }

            // 成功分析次数
            String succToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_SUCC_TOOL, date, dataFrom,
                    scanStatType.getValue());
            Map<Object, Object> toolSuccCountMap = redisTemplate.opsForHash().entries(succToolKey);
            if (toolSuccCountMap == null) {
                toolSuccCountMap = Maps.newHashMap();
            }

            // 失败分析次数
            String failToolKey = String.format("%s%s:%s:%s", RedisKeyConstants.PREFIX_ANALYZE_FAIL_TOOL, date, dataFrom,
                    scanStatType.getValue());
            Map<Object, Object> toolFailCountMap = redisTemplate.opsForHash().entries(failToolKey);
            if (toolFailCountMap == null) {
                toolFailCountMap = Maps.newHashMap();
            }

            for (String tool : toolArr) {
                ToolElapseTimeEntity elapseTimeEntity = new ToolElapseTimeEntity();
                elapseTimeEntity.setDate(date);
                elapseTimeEntity.setToolName(tool);
                elapseTimeEntity.setDataFrom(dataFrom);
                elapseTimeEntity.setScanStatType(scanStatType.getValue());

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
        log.info("toolAnalyzeElapseTimeStat finish.");
    }

    /**
     * 定时任务 初始化每日新增代码库/代码分支数
     */
    private void codeRepoStatDaily() {
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        // 传递参数 2 表示查询前一天的数据进行初始化
        reqVO.setInitDay(2);
        Boolean isSuccess = client.getWithoutRetry(OpDefectRestResource.class).initCodeRepoStatTrend(reqVO).getData();
        if (!isSuccess) {
            log.error("query code repo stat daily fail!");
        }
    }
}
