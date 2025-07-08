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

package com.tencent.bk.codecc.codeccjob.consumer;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CHECKER_DEFECT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_CHECKER_DEFECT_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CHECKER_DEFECT_STAT;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.codeccjob.dao.core.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.codeccjob.dao.core.mongotemplate.CheckerSetTaskRelationshipDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CheckerDefectStatRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CheckerMisreportStatRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.OperationHistoryDao;
import com.tencent.bk.codecc.defect.model.CheckerDefectStatEntity;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerMisreportStatEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticExtEntity;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.pojo.RefreshCheckerDefectStatModel;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 规则告警统计消费者
 *
 * @version V1.0
 * @date 2020/11/13
 */
@Slf4j
@Component
public class CheckerDefectStatConsumer {

    /**
     * 定义告警状态
     */
    public static final int EXIST = ComConstants.DefectStatus.NEW.value();
    public static final int FIXED = EXIST | ComConstants.DefectStatus.FIXED.value();
    public static final int FIXED2 = FIXED | ComConstants.DefectStatus.IGNORE.value();
    public static final int FIXED3 = FIXED | ComConstants.DefectStatus.PATH_MASK.value();
    public static final int FIXED4 = FIXED | ComConstants.DefectStatus.CHECKER_MASK.value();
    public static final int IGNORE = EXIST | ComConstants.DefectStatus.IGNORE.value();
    public static final int EXCLUDE = EXIST | ComConstants.DefectStatus.PATH_MASK.value();
    public static final int EXCLUDE2 = EXIST | ComConstants.DefectStatus.CHECKER_MASK.value();
    /**
     * 规则误报base data的paramCode
     */
    private static final String CHECKER_MISREPORT = "CHECKER_MISREPORT";
    /**
     * 规则误报base data的paramType
     */
    private static final String OPERA_FUNC = "OPERA_FUNC";
    /**
     * 告警字段常量
     */
    private static final String FIELD_IGNORE_TIME = "ignore_time";
    /**
     * 定义除忽略以外的状态值
     */
    protected final Set<Integer> defectStatusSet =
            Sets.newHashSet(EXIST, FIXED, FIXED2, FIXED3, FIXED4, EXCLUDE, EXCLUDE2);
    @Autowired
    protected CheckerMisreportStatRepository checkerMisreportStatRepo;
    @Autowired
    private Client client;
    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCache;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private CheckerSetDao checkerSetDao;
    @Autowired
    private LintDefectV2Dao lintDefectDao;
    @Autowired
    private CheckerDefectStatRepository checkerDefectStatRepository;
    @Autowired
    private CheckerSetTaskRelationshipDao checkerSetTaskRelationshipDao;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private OperationHistoryDao operationHistoryDao;

    /**
     * 定时按规则统计各种状态的告警数
     *
     * @param model model
     */
    @RabbitListener(
            bindings = @QueueBinding(key = ROUTE_CHECKER_DEFECT_STAT,
                    value = @Queue(value = QUEUE_CHECKER_DEFECT_STAT),
                    exchange = @Exchange(
                            value = EXCHANGE_CHECKER_DEFECT_STAT,
                            durable = "false")
            ),
            concurrency = "1"
    )
    public void consumer(RefreshCheckerDefectStatModel model) {
        try {
            log.info("CheckerDefectStatConsumer begin, mq obj: {}", model);
            businessCore(model);
            log.info("CheckerDefectStatConsumer end, mq obj: {}", model);
        } catch (Throwable t) {
            log.error("CheckerDefectStatConsumer error, mq obj: {}", model, t);
        }
    }

    private void businessCore(RefreshCheckerDefectStatModel model) {
        Assert.notNull(model, "RefreshCheckerDefectStatModel must not be null!");

        String stopFlag = redisTemplate.opsForValue().get("CHECKER_DEFECT_STAT_CONSUMER_STOP_FLAG");
        if (StringUtils.isNotEmpty(stopFlag) && "1".equals(stopFlag)) {
            log.info("CHECKER_DEFECT_STAT_CONSUMER_STOP_FLAG is open");
            return;
        }

        // 统计规则误报率
        try {
            consumerIgnore(model);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // 如果只是统计部分数据，则结束退出
        if (StringUtils.isNotBlank(model.getTriggerPart())) {
            log.warn("TriggerPart is finished!");
            return;
        }

        String dataFrom = model.getDataFrom();
        // 判断统计数据来源
        List<String> createFrom = getCreateFromByDataFrom(dataFrom);

        // 获取工具列表
        String toolOrderStr = client.get(ServiceToolRestResource.class).findToolOrder().getData();
        Assert.notNull(toolOrderStr, "tool data must not be null!");
        List<String> toolList = Lists.newArrayList(toolOrderStr.split(ComConstants.STRING_SPLIT));

        // 暂存所有工具的规则
        Map<String, Set<String>> toolCheckerInfoMap = Maps.newHashMap();

        Map<String, CheckerDefectStatEntity> defectStatEntityMap =
                initDefectStatEntityMap(dataFrom, toolList, toolCheckerInfoMap);

        int dataSize = 0;
        int pageSize = 2000;
        // 定义重试次数
        int retryCount = 5;
        long lastTaskId = 0L;
        do {
            // 获取任务ID列表 (排除灰度项目)
            Result<List<Long>> result = client.get(ServiceTaskRestResource.class)
                    .queryTaskIdByCreateFromExcludeGray(createFrom, lastTaskId, pageSize);

            if (result.isNotOk() || result.getData() == null) {
                if (retryCount > 0) {
                    retryCount--;
                    log.warn("query task id page failed! retrying...");
                    try {
                        log.info("waiting 2s");
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        log.error("thread sleep fail!", e);
                    }
                    continue;
                }
                // 获取不到任务id 终止执行
                log.error("query task id page failed! exited.");
                return;
            }
            retryCount = 5;
            List<Long> taskIdList = result.getData();

            if (CollectionUtils.isEmpty(taskIdList)) {
                log.info("taskIdList is empty!");
                return;
            }
            lastTaskId = taskIdList.get(taskIdList.size() - 1);

            for (String toolName : toolList) {
                String toolPattern = toolMetaCache.getToolPattern(toolName);

                // 过滤掉无需统计的工具
                Set<String> checkerKeySet = toolCheckerInfoMap.get(toolName);
                if (CollectionUtils.isEmpty(checkerKeySet)) {
                    log.info("the other tool checker is no need for statistics: {}", toolName);
                    continue;
                }

                Map<String, Integer> checkerUsageMap = getCheckerUsageMap(taskIdList, checkerKeySet);

                // 分批统计各状态规则告警数
                List<CheckerStatisticEntity> existStatEntities;
                List<CheckerStatisticEntity> fixedStatEntities;
                List<CheckerStatisticEntity> ignoreStatEntities;
                List<CheckerStatisticEntity> excludeStatEntities;
                if (ToolPattern.LINT.name().equals(toolPattern)
                        || ToolPattern.getCommonPatternList().contains(toolPattern)) {
                    existStatEntities =
                            lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, EXIST, checkerKeySet);
                    fixedStatEntities =
                            lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, FIXED, checkerKeySet);
                    ignoreStatEntities =
                            lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, IGNORE, checkerKeySet);
                    excludeStatEntities =
                            lintDefectDao.findStatByTaskIdAndToolChecker(taskIdList, toolName, EXCLUDE, checkerKeySet);

                } else {
                    log.info("the other tool checker is no need for statistics: {}", toolName);
                    continue;
                }
                Map<String, Integer> existCountMap = existStatEntities.stream().collect(
                        Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
                Map<String, Integer> fixedCountMap = fixedStatEntities.stream().collect(
                        Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
                Map<String, Integer> ignoreCountMap = ignoreStatEntities.stream().collect(
                        Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));
                Map<String, Integer> excludeCountMap = excludeStatEntities.stream().collect(
                        Collectors.toMap(CheckerStatisticEntity::getId, CheckerStatisticEntity::getDefectCount));

                for (String checker : checkerKeySet) {
                    CheckerDefectStatEntity entity = defectStatEntityMap.get(toolName + checker);

                    int existCount = MapUtils.getIntValue(existCountMap, checker);
                    int fixedCount = MapUtils.getIntValue(fixedCountMap, checker);
                    int ignoreCount = MapUtils.getIntValue(ignoreCountMap, checker);
                    int excludeCount = MapUtils.getIntValue(excludeCountMap, checker);
                    int totalCount = existCount + fixedCount + ignoreCount + excludeCount;

                    entity.setOpenCheckerTaskCount(sumDefectCount(entity.getOpenCheckerTaskCount(),
                            MapUtils.getIntValue(checkerUsageMap, checker)));
                    entity.setDefectTotalCount(sumDefectCount(entity.getDefectTotalCount(), totalCount));
                    entity.setExistCount(sumDefectCount(entity.getExistCount(), existCount));
                    entity.setFixedCount(sumDefectCount(entity.getFixedCount(), fixedCount));
                    entity.setIgnoreCount(sumDefectCount(entity.getIgnoreCount(), ignoreCount));
                    entity.setExcludedCount(sumDefectCount(entity.getExcludedCount(), excludeCount));
                }
            }

            dataSize = taskIdList.size();
        } while (dataSize >= pageSize);
        checkerDefectStatRepository.saveAll(defectStatEntityMap.values());
        // 记录更新时间
        redisTemplate.opsForValue()
                .set(RedisKeyConstants.CHECKER_DEFECT_STAT_TIME, String.valueOf(System.currentTimeMillis()));
    }

    @NotNull
    private List<String> getCreateFromByDataFrom(String dataFrom) {
        List<String> createFrom;
        if (ComConstants.DefectStatType.USER.value().equals(dataFrom)) {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                    ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());
        } else {
            createFrom = Lists.newArrayList(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value());
        }
        return createFrom;
    }

    @NotNull
    private Map<String, CheckerDefectStatEntity> initDefectStatEntityMap(String dataFrom,
            @NotNull List<String> toolList, Map<String, Set<String>> toolCheckerInfoMap) {
        long todayZeroMillis = DateTimeUtils.getTodayZeroMillis();
        Map<String, CheckerDefectStatEntity> defectStatEntityMap = Maps.newHashMap();
        for (String tool : toolList) {
            String toolPattern = toolMetaCache.getToolPattern(tool);
            if (ToolPattern.LINT.name().equals(toolPattern)
                    || ToolPattern.getCommonPatternList().contains(toolPattern)) {
                Map<String, Long> checkerCreatedTimeMap = getCheckerCreatedTimeMap(tool);

                Set<String> checkerSet = checkerCreatedTimeMap.keySet();
                toolCheckerInfoMap.put(tool, checkerSet);

                for (String checker : checkerSet) {
                    CheckerDefectStatEntity entity = new CheckerDefectStatEntity();
                    entity.setDataFrom(dataFrom);
                    entity.setCheckerName(checker);
                    entity.setStatDate(todayZeroMillis);
                    entity.setToolName(tool);
                    entity.setCheckerCreatedDate(checkerCreatedTimeMap.getOrDefault(checker, 0L));

                    // 规避存在相同规则名
                    defectStatEntityMap.put(tool + checker, entity);
                }
            }
        }
        return defectStatEntityMap;
    }

    @NotNull
    private Map<String, Long> getCheckerCreatedTimeMap(String toolName) {
        // 创建时间
        Map<String, Long> checkerCreateTimeMap = Maps.newHashMap();

        List<CheckerDetailEntity> checkerDetailEntities = checkerRepository.findByToolName(toolName);
        if (CollectionUtils.isEmpty(checkerDetailEntities)) {
            log.info("tool checker detail is empty: {}", toolName);
            return checkerCreateTimeMap;
        }

        for (CheckerDetailEntity checkerDetailEntity : checkerDetailEntities) {
            Long createdDate = checkerDetailEntity.getCreatedDate();
            if (createdDate == null) {
                createdDate = 0L;
            }
            checkerCreateTimeMap.put(checkerDetailEntity.getCheckerKey(), createdDate);
        }
        return checkerCreateTimeMap;
    }

    private int sumDefectCount(int currentDefectCount, int statDefectCount) {
        return currentDefectCount + statDefectCount;
    }

    /**
     * 组装生成规则告警统计实体
     *
     * @param toolName 工具
     * @param dataFrom 统计数据来源 enum DefectStatType
     * @param checkerUsageMap 规则使用量
     * @param checkers 规则列表
     * @param existCountMap 待修复数
     * @param fixedCountMap 已修复数
     * @param ignoreCountMap 已忽略数
     * @param excludeCountMap 已屏蔽数
     * @param checkerCreateTimeMap 规则创建时间
     * @return list
     */
    @Deprecated
    @NotNull
    private List<CheckerDefectStatEntity> generateCheckerDefectStatEntities(String toolName, String dataFrom,
            Map<String, Integer> checkerUsageMap, @NotNull List<String> checkers, Map<String, Integer> existCountMap,
            Map<String, Integer> fixedCountMap, Map<String, Integer> ignoreCountMap,
            Map<String, Integer> excludeCountMap, Map<String, Long> checkerCreateTimeMap) {
        long timeMillis = System.currentTimeMillis();
        List<CheckerDefectStatEntity> dataList = Lists.newArrayList();
        for (String checker : checkers) {
            int existCount = MapUtils.getIntValue(existCountMap, checker);
            int fixedCount = MapUtils.getIntValue(fixedCountMap, checker);
            int ignoreCount = MapUtils.getIntValue(ignoreCountMap, checker);
            int excludeCount = MapUtils.getIntValue(excludeCountMap, checker);

            CheckerDefectStatEntity statEntity = new CheckerDefectStatEntity();
            statEntity.setStatDate(timeMillis);
            statEntity.setToolName(toolName);
            statEntity.setCheckerName(checker);
            statEntity.setCheckerCreatedDate(MapUtils.getLongValue(checkerCreateTimeMap, checker));
            statEntity.setDataFrom(dataFrom);
            statEntity.setOpenCheckerTaskCount(MapUtils.getIntValue(checkerUsageMap, checker));
            statEntity.setDefectTotalCount(existCount + fixedCount + ignoreCount + excludeCount);
            statEntity.setExistCount(existCount);
            statEntity.setFixedCount(fixedCount);
            statEntity.setIgnoreCount(ignoreCount);
            statEntity.setExcludedCount(excludeCount);
            dataList.add(statEntity);
        }
        return dataList;
    }

    // 批量统计各规则使用量
    @NotNull
    private Map<String, Integer> getCheckerUsageMap(List<Long> taskIdList, Set<String> checkers) {
        Set<String> checkerSetIdSet = Sets.newHashSet();
        List<CheckerSetEntity> checkerSetEntities = checkerSetDao.findByCheckerNameList(checkers);
        Map<String, Set<String>> checkerSetIdMap = Maps.newHashMap();
        for (CheckerSetEntity checkerSet : checkerSetEntities) {
            List<CheckerPropsEntity> checkerProps = checkerSet.getCheckerProps();
            checkerProps.forEach(checkerPropsEntity -> {
                String checkerKey = checkerPropsEntity.getCheckerKey();
                if (checkers.contains(checkerKey)) {
                    Set<String> checkerSetIds = checkerSetIdMap.computeIfAbsent(checkerKey, v -> Sets.newHashSet());
                    checkerSetIds.add(checkerSet.getCheckerSetId());
                    checkerSetIdSet.add(checkerSet.getCheckerSetId());
                }
            });
        }

        List<CheckerStatisticExtEntity> checkerSetTaskRelationships =
                checkerSetTaskRelationshipDao.findTaskIdByCheckerSetIds(checkerSetIdSet, taskIdList);
        Map<String, Set<Long>> checkerSetUsageMap = checkerSetTaskRelationships.stream()
                .collect(Collectors.toMap(CheckerStatisticExtEntity::getId, CheckerStatisticExtEntity::getTaskInUse));

        Map<String, Integer> checkerUsageMap = Maps.newHashMap();
        for (Map.Entry<String, Set<String>> entry : checkerSetIdMap.entrySet()) {
            String checkerKey = entry.getKey();
            Set<String> checkerSetIds = entry.getValue();
            if (CollectionUtils.isEmpty(checkerSetIds)) {
                checkerUsageMap.put(checkerKey, 0);
                log.info("checker is not in any checkerSet: {}", checkerKey);
                continue;
            }

            Set<Long> taskInUseSet = Sets.newHashSet();
            for (String checkerSetId : checkerSetIds) {
                Set<Long> taskIdSet = checkerSetUsageMap.get(checkerSetId);
                if (CollectionUtils.isNotEmpty(taskIdSet)) {
                    taskInUseSet.addAll(taskIdSet);
                }
            }

            checkerUsageMap.put(checkerKey, taskInUseSet.size());
        }
        return checkerUsageMap;
    }

    private void consumerIgnore(RefreshCheckerDefectStatModel model) {
        Assert.notNull(model, "RefreshCheckerDefectStatModel must not be null!");
        String dataFrom = model.getDataFrom();

        // 只需按开源/非开源统计,不需要统计全量。更新，不再支持统计全量
//        if (ComConstants.DefectStatType.ALL.value().equals(dataFrom)) {
//            log.warn("dataFrom is all, exist!");
//            return;
//        }
        // 判断统计数据来源
        List<String> createFrom = getCreateFromByDataFrom(dataFrom);

        // 计算出本次的统计时间范围
        long endTime = getEndTime(model.getStatDate());
        long startTime = endTime - DateTimeUtils.DAY_TIMESTAMP;

        // 定义查询所涉及的操作类型
        Set<String> operaFuncIdSet = getOperaFuncIdSet();

        // 拿到历史操作记录
        List<OperationHistoryEntity> operationHistoryEntities =
                operationHistoryDao.findByCreateDateAndFuncId(startTime, endTime, operaFuncIdSet);

        if (CollectionUtils.isEmpty(operationHistoryEntities)) {
            log.warn("ignore stat operation history is empty! start: {}, end:{}, exist!",
                    DateTimeUtils.timestamp2StringDate(startTime), DateTimeUtils.timestamp2StringDate(endTime));
            return;
        }
        // 根据来源筛选有效task id
        Set<Long> effectTaskId = filterEffectTaskIdByCreateFrom(operationHistoryEntities, createFrom);

        // 按工具分组任务id
        Map<String, Set<Long>> toolTaskIdMap = groupByTool2TaskIdSet(operationHistoryEntities, effectTaskId);

        /*
        1.拿到昨天操作（忽略、标记、评论）过告警的任务id
        2.按告警的创建时间（天）、工具、规则分组任务id
        3.遍历-获取entity 里的任务id 查询出有效的任务id、工具，没有则新建
        4.遍历-按创建时间、工具、任务id或者指定告警数
        */
        // 遍历每个工具 获取到昨日忽略的告警有那些规则，并按告警创建时间分组
        for (Map.Entry<String, Set<Long>> entry : toolTaskIdMap.entrySet()) {
            String tool = entry.getKey();
            Set<Long> taskIdSet = entry.getValue();

            String toolPattern = toolMetaCache.getToolPattern(tool);

            // 过滤无需统计的工具
            if (ToolPattern.CLOC.name().equals(toolPattern) || ToolPattern.STAT.name().equals(toolPattern)
                    || ToolPattern.DUPC.name().equals(toolPattern) || ToolPattern.CCN.name().equals(toolPattern)) {
                log.warn("the other tool checker is no need for statistics: {}", tool);
                continue;
            }
            log.info("begin to stat checker defect by tool: {}", tool);

            try {
                statIgnoreDefectCount(startTime, endTime, dataFrom, tool, taskIdSet, operaFuncIdSet);
            } catch (Exception e) {
                log.error("checker defect statistic fail! tool: {}, message:{}", tool, e.getMessage(), e);
            }
        }

        redisTemplate.opsForValue()
                .set(RedisKeyConstants.CHECKER_DEFECT_STAT_IGNORE_TIME, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 忽略告警数统计
     */
    private void statIgnoreDefectCount(long startTime, long endTime, String dataFrom, String tool, Set<Long> taskIdSet,
            Set<String> operaFuncIdSet) {
        log.info("stat time[{}|{}], dataFrom[{}] tool[{}]", startTime, endTime, dataFrom, tool);
        // 按创建时间分组，每个规则集下有哪些任务id
        Map<Long, Map<String, Set<Long>>> timeCheckerTaskIdsMap = Maps.newHashMap();

        List<LintDefectV2Entity> defectV2EntityList =
                lintDefectDao.statByCheckerAndStatus(taskIdSet, tool, IGNORE, FIELD_IGNORE_TIME, startTime, endTime);
        log.info("tool[{}] ignored defect count: {}", tool, defectV2EntityList.size());
        putDefectListIntoStatistics(timeCheckerTaskIdsMap, defectV2EntityList);

        // 统计有评论告警，纳入统计
        Map<Long, Map<String, Set<String>>> timeCheckerCommentIdsMap = Maps.newHashMap();
        if (operaFuncIdSet.contains(ComConstants.FUNC_CODE_COMMENT_ADD)) {
            statDefectByComment(startTime, endTime, tool, taskIdSet, timeCheckerTaskIdsMap, timeCheckerCommentIdsMap);
        }

        // 遍历大Map  time       checker     taskId
        for (Map.Entry<Long, Map<String, Set<Long>>> mapEntry : timeCheckerTaskIdsMap.entrySet()) {
            Long statDate = mapEntry.getKey();
            long statEndTime = statDate + DateTimeUtils.DAY_TIMESTAMP;
            Map<String, Set<Long>> checkerTaskIdsMap = mapEntry.getValue();

            Set<String> checkerIds = checkerTaskIdsMap.keySet();

            Map<String, CheckerMisreportStatEntity> checkerStatEntityMap =
                    getCheckerMisreportStatEntityMap(dataFrom, tool, statDate, checkerIds);

            Map<String, Set<String>> checkerCommentIdsMap = timeCheckerCommentIdsMap.get(statDate);

            for (Map.Entry<String, Set<Long>> setEntry : checkerTaskIdsMap.entrySet()) {
                String checkerId = setEntry.getKey();
                Set<Long> taskIds = setEntry.getValue();

                CheckerMisreportStatEntity entity = checkerStatEntityMap.computeIfAbsent(checkerId,
                        k -> new CheckerMisreportStatEntity(dataFrom, statDate, tool, checkerId));

                Set<Long> taskIdSetInDB = entity.getTaskIdSet();
                if (null == taskIdSetInDB) {
                    taskIdSetInDB = Sets.newHashSet();
                }
                // 并集，统计涵盖以前已统计的数据（全量）
                taskIdSetInDB.addAll(taskIds);
                entity.setTaskIdSet(taskIdSetInDB);

                // 拿到该checkerId规则在statDate当天 taskIdSetInDB所新增的告警数(不包括忽略状态)
                List<CheckerStatisticExtEntity> checkerStatisticEntities = lintDefectDao
                        .findStatByTaskIdAndToolChecker(taskIdSetInDB, tool, defectStatusSet, checkerId, statDate,
                                statEndTime);

                Map<Integer, Integer> statusDefectCountMap = checkerStatisticEntities.stream().collect(Collectors
                        .toMap(CheckerStatisticExtEntity::getStatus, CheckerStatisticExtEntity::getDefectCount));

                // 额外统计忽略状态的告警
                List<LintDefectV2Entity> ignoreGroupByReasonType = lintDefectDao
                        .statIgnoreGroupByReasonType(taskIdSetInDB, tool, checkerId, IGNORE, statDate, statEndTime);

                Map<Integer, Integer> ignoreCountMap = ignoreGroupByReasonType.stream().collect(
                        Collectors.toMap(LintDefectV2Entity::getIgnoreReasonType, LintDefectV2Entity::getLineNum));

                // 给实体对象赋值
                assignEntityProperties(entity, statusDefectCountMap, ignoreCountMap);
                // 评论数（累计=原有+增量）
                if (null != checkerCommentIdsMap && checkerCommentIdsMap.get(checkerId) != null) {
                    int commentDefectCount = checkerCommentIdsMap.get(checkerId).size();
                    entity.setCommentCount(entity.getCommentCount() + commentDefectCount);
                }
            }

            checkerMisreportStatRepo.saveAll(checkerStatEntityMap.values());
        }
        log.info("statistic finish");
    }

    /**
     * 把告警评论纳入统计
     *
     * @param timeCheckerTaskIdsMap 查询范围
     * @param timeCheckerCommentIdsMap 各时间各规则评论数
     */
    private void statDefectByComment(long startTime, long endTime, String tool, Set<Long> taskIdSet,
            Map<Long, Map<String, Set<Long>>> timeCheckerTaskIdsMap,
            Map<Long, Map<String, Set<String>>> timeCheckerCommentIdsMap) {
        List<LintDefectV2Entity> defectHasComment = lintDefectDao.findDefectHasComment(taskIdSet, tool);
        if (CollectionUtils.isNotEmpty(defectHasComment)) {
            List<LintDefectV2Entity> commentDefectEntityList = Lists.newArrayList();

            for (LintDefectV2Entity entity : defectHasComment) {
                CodeCommentEntity codeComment = entity.getCodeComment();
                if (codeComment == null || codeComment.getCreatedDate() < startTime
                        || codeComment.getCreatedDate() > endTime
                        // 筛掉评论已删除
                        || CollectionUtils.isEmpty(codeComment.getCommentList())) {
                    continue;
                }
                commentDefectEntityList.add(entity);

                // 将评论按创建日期、规则分组统计
                long createTime = DateTimeUtils.getTimeStampStart(entity.getCreateTime());
                if (createTime <= 0) {
                    log.warn("defect createTime is invalid! entityId: {}", entity.getEntityId());
                    continue;
                }

                Map<String, Set<String>> checkerTaskIdsMap =
                        timeCheckerCommentIdsMap.computeIfAbsent(createTime, k -> Maps.newHashMap());
                checkerTaskIdsMap.computeIfAbsent(entity.getChecker(), k -> new HashSet<>())
                        .add(codeComment.getEntityId());
            }
            putDefectListIntoStatistics(timeCheckerTaskIdsMap, commentDefectEntityList);
        }
    }

    /**
     * 纳入统计的告警
     *
     * @param timeCheckerTaskIdsMap 统计数据关系映射
     * @param defectV2EntityList 需纳入统计的告警
     */
    private void putDefectListIntoStatistics(Map<Long, Map<String, Set<Long>>> timeCheckerTaskIdsMap,
            List<LintDefectV2Entity> defectV2EntityList) {
        if (CollectionUtils.isNotEmpty(defectV2EntityList)) {
            for (LintDefectV2Entity entity : defectV2EntityList) {
                // 告警创建时间统一转为当天0点 方便汇总统计
                long createTime = DateTimeUtils.getTimeStampStart(entity.getCreateTime());
                if (createTime <= 0) {
                    log.warn("defect createTime is invalid! entityId: {}", entity.getEntityId());
                    continue;
                }

                Map<String, Set<Long>> checkerTaskIdsMap =
                        timeCheckerTaskIdsMap.computeIfAbsent(createTime, k -> Maps.newHashMap());
                checkerTaskIdsMap.computeIfAbsent(entity.getChecker(), k -> Sets.newHashSet()).add(entity.getTaskId());
            }
        }
    }

    @NotNull
    private Map<String, CheckerMisreportStatEntity> getCheckerMisreportStatEntityMap(String dataFrom, String tool,
            Long statDate, Set<String> checkerIds) {
        // 批量查该statDate创建时间所涉及的规则
        List<CheckerMisreportStatEntity> statEntityList = checkerMisreportStatRepo
                .findByDataFromAndStatDateAndToolNameAndCheckerNameIn(dataFrom, statDate, tool, checkerIds);

        // 遍历原有数据,并集任务id后刷新所有状态的告警数
        return statEntityList.stream().collect(
                Collectors.toMap(CheckerMisreportStatEntity::getCheckerName, Function.identity(), (k, v) -> v));
    }

    /**
     * 给实体对象赋值
     *
     * @param entity 实体对象
     * @param statusDefectCountMap 状态告警Map
     */
    private void assignEntityProperties(@NotNull CheckerMisreportStatEntity entity,
            @NotNull Map<Integer, Integer> statusDefectCountMap, Map<Integer, Integer> ignoreCountMap) {
        entity.setExistCount(statusDefectCountMap.getOrDefault(EXIST, 0));

        // 已修复的告警有多种状态
        int fixedCount = statusDefectCountMap.getOrDefault(FIXED, 0)
                + statusDefectCountMap.getOrDefault(FIXED2, 0)
                + statusDefectCountMap.getOrDefault(FIXED3, 0)
                + statusDefectCountMap.getOrDefault(FIXED4, 0);
        entity.setFixedCount(fixedCount);

        Integer excludePath = statusDefectCountMap.getOrDefault(EXCLUDE, 0);
        Integer excludeChecker = statusDefectCountMap.getOrDefault(EXCLUDE2, 0);
        entity.setExcludedCount(excludePath + excludeChecker);

        entity.setIgnoreErrorDefectCount(
                ignoreCountMap.getOrDefault(ComConstants.IgnoreReasonType.ERROR_DETECT.value(), 0));
        entity.setIgnoreCount(
                ignoreCountMap.values().stream().filter(Objects::nonNull).mapToInt(value -> value).sum());
    }

    @NotNull
    private Set<Long> filterEffectTaskIdByCreateFrom(@NotNull List<OperationHistoryEntity> operationHistoryEntities,
            List<String> createFrom) {
        List<Long> allTaskIdList = operationHistoryEntities.stream().map(OperationHistoryEntity::getTaskId).distinct()
                .collect(Collectors.toList());

        List<Object> taskIdCreateFrom = redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (Long taskId : allTaskIdList) {
                String redisKey = AuthExConstantsKt.PREFIX_TASK_INFO + taskId;
                try {
                    conn.hGet(redisKey.getBytes(StandardCharsets.UTF_8.name()),
                            AuthExConstantsKt.KEY_CREATE_FROM.getBytes(StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage(), e);
                }
            }
            return null;
        });

        Set<Long> effectTaskId = Sets.newHashSet();
        for (int i = 0; i < allTaskIdList.size(); i++) {
            String from = (String) taskIdCreateFrom.get(i);
            if (createFrom.contains(from)) {
                Long taskId = allTaskIdList.get(i);
                effectTaskId.add(taskId);
            }
        }
        return effectTaskId;
    }

    @NotNull
    private Map<String, Set<Long>> groupByTool2TaskIdSet(@NotNull List<OperationHistoryEntity> operationHistoryEntities,
            Set<Long> effectTaskId) {
        Map<String, Set<Long>> toolTaskIdMap = Maps.newHashMap();
        for (OperationHistoryEntity entity : operationHistoryEntities) {
            String toolName = entity.getToolName();
            if (StringUtils.isEmpty(toolName)) {
                continue;
            }

            long taskId = entity.getTaskId();
            if (!effectTaskId.contains(taskId)) {
                continue;
            }

            String[] toolArr = toolName.split(ComConstants.STRING_SPLIT);
            for (String tool : toolArr) {
                toolTaskIdMap.computeIfAbsent(tool, k -> Sets.newHashSet()).add(taskId);
            }
        }
        return toolTaskIdMap;
    }

    /**
     * 全量任务太多，仅统计有指定操作类型的任务
     *
     * @return set
     */
    private Set<String> getOperaFuncIdSet() {
        Set<String> operaFuncIdSet;
        final List<BaseDataVO> baseDataVOS =
                client.get(ServiceBaseDataResource.class).getInfoByTypeAndCode(CHECKER_MISREPORT, OPERA_FUNC).getData();
        if (CollectionUtils.isNotEmpty(baseDataVOS)) {
            operaFuncIdSet = baseDataVOS.stream().map(BaseDataVO::getParamValue).collect(Collectors.toSet());
        } else {
            operaFuncIdSet = Sets.newHashSet(ComConstants.FUNC_DEFECT_IGNORE);
        }
        return operaFuncIdSet;
    }

    private long getEndTime(String statDateReq) {
        long endTime;
        if (StringUtils.isNotBlank(statDateReq)) {
            log.info("statDate is {}", statDateReq);
            endTime = DateTimeUtils.getTimeStampEnd(statDateReq);
        } else {
            endTime = DateTimeUtils.getTimeStampStart(System.currentTimeMillis());
        }
        return endTime;
    }

    @NotNull
    private Map<String, Set<String>> getToolCheckerIdSetMap(String separator, Map<String, Integer> mapEntryValue) {
        Map<String, Set<String>> toolCheckerMap = Maps.newHashMap();
        if (mapEntryValue == null || mapEntryValue.isEmpty()) {
            return toolCheckerMap;
        }
        mapEntryValue.forEach((key, value) -> {
            String[] strArr = key.split(separator);
            Set<String> checkerSet = toolCheckerMap.computeIfAbsent(strArr[0], k -> Sets.newHashSet());
            checkerSet.add(strArr[1]);
        });
        return toolCheckerMap;
    }

    @NotNull
    private Map<Long, Map<String, Integer>> groupByTimeToolCheckerMap(String separator,
            List<LintDefectV2Entity> defectEntityList) {
        // timestamp tool~checker, count
        Map<Long, Map<String, Integer>> checkerDataTableMap = Maps.newHashMap();

        if (CollectionUtils.isEmpty(defectEntityList)) {
            log.warn("groupByTimeToolCheckerMap defect entity list is empty!");
            return checkerDataTableMap;
        }

        for (LintDefectV2Entity entity : defectEntityList) {

            // 告警创建时间统一转为当天0点 方便汇总统计
            long createTime = DateTimeUtils.getTimeStampStart(entity.getCreateTime());
            if (createTime <= 0) {
                log.warn("defect createTime is invalid! entityId: {}", entity.getEntityId());
                continue;
            }

            // 告警数
            int defectCount = entity.getLineNum();

            // 累加告警数
            Map<String, Integer> checkerCountMap =
                    checkerDataTableMap.computeIfAbsent(createTime, k -> Maps.newHashMap());

            // 避免不同工具有同名规则
            String keyFlag =
                    String.format("%s%s%s", entity.getToolName(), separator, entity.getChecker());
            int count = checkerCountMap.getOrDefault(keyFlag, 0);
            checkerCountMap.put(keyFlag, count + defectCount);
        }
        return checkerDataTableMap;
    }

}
