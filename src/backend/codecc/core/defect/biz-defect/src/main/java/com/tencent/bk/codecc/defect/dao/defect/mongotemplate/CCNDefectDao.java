/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeStatModel;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.vo.CCNDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.util.DateTimeUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

/**
 * 圈复杂度持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
@Repository
public class CCNDefectDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    public List<CCNDefectEntity> findByTaskIdAndStatus(long taskId, int status) {
        return findByTaskIdAndAuthorAndStatusAndRelPaths(taskId, null, status, null);
    }

    public List<CCNDefectEntity> findByTaskIdAndAuthorAndRelPaths(long taskId, String author, Set<String> fileList) {
        return findByTaskIdAndAuthorAndStatusAndRelPaths(taskId, author, null, fileList);
    }

    /**
     * 根据任务ID，作者和路径列表查询
     *
     * @param taskId
     * @param author
     * @param status
     * @param fileList
     * @return
     */
    public List<CCNDefectEntity> findByTaskIdAndAuthorAndStatusAndRelPaths(long taskId, String author, Integer status,
            Set<String> fileList) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));

        //作者过滤
        if (StringUtils.isNotEmpty(author)) {
            query.addCriteria(Criteria.where("author").is(author));
        }

        //路径过滤
        List<Criteria> criteriaList = new ArrayList<>();
        List<Criteria> orCriteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileList)) {
            fileList.forEach(file ->
                    criteriaList.add(Criteria.where("rel_path").regex(file))
            );
            orCriteriaList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
            query.addCriteria(new Criteria().andOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        // 状态过滤
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        //查询总的数量，并且过滤计数
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<CCNStatisticEntity> findFirstByTaskIdOrderByTime(long taskId, Set<String> toolSet) {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId)
                .and("tool_name").in(toolSet));
        //根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("tool_name")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("analysis_version").as("analysis_version")
                .first("time").as("time")
                .first("defect_count").as("defect_count")
                .first("defect_change").as("defect_change")
                .first("last_defect_count").as("last_defect_count")
                .first("average_ccn").as("average_ccn")
                .first("last_average_ccn").as("last_average_ccn")
                .first("average_ccn_change").as("average_ccn_change")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count")
                .first("low_count").as("low_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<CCNStatisticEntity> queryResult = defectMongoTemplate.aggregate(agg, "t_ccn_statistic",
                CCNStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    public void batchMarkDefect(long taskId, List<CCNDefectEntity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    public void batchUpdateDefectAuthor(long taskId, List<CCNDefectEntity> defectList, String newAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", newAuthor);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量获取最新分析统计数据
     *
     * @param taskIdSet 任务ID集合
     * @param toolName 工具名称
     * @return list
     */
    public List<CCNStatisticEntity> batchFindByTaskIdInAndTool(Collection<Long> taskIdSet, String toolName) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName));
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "time");
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("time").as("time")
                .first("average_ccn").as("average_ccn")
                .first("last_average_ccn").as("last_average_ccn")
                .first("average_ccn_change").as("average_ccn_change")
                .first("super_high_count").as("super_high_count")
                .first("high_count").as("high_count")
                .first("medium_count").as("medium_count");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<CCNStatisticEntity> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_statistic", CCNStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 批量统计任务待修复的告警个数
     *
     * @param taskIdSet 任务id集合
     * @return list
     */
    public List<DefectCountModel> statisticCCNDefect(Set<Long> taskIdSet) {
        // 根据查询条件过滤
        Criteria criteria = new Criteria();
        criteria.and("task_id").in(taskIdSet).and("status").is(ComConstants.DefectStatus.NEW.value());

        MatchOperation match = Aggregation.match(criteria);

        // 以task_id进行分组
        GroupOperation group = Aggregation.group("task_id")
                .last("task_id").as("taskId")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    /**
     * 统计指定忽略类型的告警数
     */
    public List<IgnoreTypeStatModel> statisticIgnoreDefect(Set<Long> taskCCNSet, int ignoreTypeId, int ignoreStatus) {
        if (CollectionUtils.isEmpty(taskCCNSet)) {
            return Collections.emptyList();
        }

        Criteria criteria =
                Criteria.where("task_id").in(taskCCNSet).and("status").is(ignoreStatus).and("ignore_reason_type")
                        .is(ignoreTypeId);

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id", "ignore_author")
                .first("task_id").as("taskId")
                .first("ignore_author").as("ignoreAuthor")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 统计指定忽略类型的告警作者
     */
    public List<IgnoreTypeStatModel> findIgnoreDefectAuthor(Set<Long> taskCCNSet, int ignoreTypeId, int ignoreStatus) {
        if (CollectionUtils.isEmpty(taskCCNSet)) {
            return Collections.emptyList();
        }

        Criteria criteria =
                Criteria.where("task_id").in(taskCCNSet).and("status").is(ignoreStatus).and("ignore_reason_type")
                        .is(ignoreTypeId);

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("author")
                .first("author").as("ignoreAuthor");

        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    public List<CCNDefectEntity> findHistoryNewDefectByPageNoSkip(Long taskId, Long newDefectJudgeTime,
            Integer pageSize) {
        List<Criteria> criList = new ArrayList<>();
        criList.add(Criteria.where("task_id").is(taskId)
                .and("status").is(ComConstants.DefectStatus.NEW.value()));
        Criteria historyCri = new Criteria().orOperator(
                Criteria.where("latest_datetime").lt(newDefectJudgeTime),
                Criteria.where("latest_datetime").exists(false),
                Criteria.where("latest_datetime").is(null));
        criList.add(historyCri);
        Query query = Query.query(new Criteria().andOperator(criList.toArray(new Criteria[]{})));
        query.limit(pageSize);
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    public List<CCNDefectEntity> findIgnoreDefectByPageNoSkip(Long taskId, Integer ignoreReasonType, Integer pageSize) {
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("status").is(ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())
                .and("ignore_reason_type").is(ignoreReasonType));
        query.limit(pageSize);
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }


    public void batchIgnoreDefect(long taskId, List<CCNDefectEntity> defectList,
            int ignoreReasonType, String ignoreReason, String ignoreAuthor) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }
        List<String> ids = defectList.stream().map(CCNDefectEntity::getEntityId).collect(Collectors.toList());
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("_id").in(ids));
        long curTime = System.currentTimeMillis();
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value());
        update.set("ignore_reason_type", ignoreReasonType);
        update.set("ignore_reason", ignoreReason);
        update.set("ignore_time", curTime);
        update.set("ignore_author", ignoreAuthor);
        defectMongoTemplate.updateMulti(query, update, CCNDefectEntity.class);
    }

    public void batchRollbackIgnoreDefect(long taskId, List<CCNDefectEntity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }
        List<String> ids = defectList.stream().map(CCNDefectEntity::getEntityId).collect(Collectors.toList());
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("_id").in(ids));
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value());
        update.unset("ignore_reason_type");
        update.unset("ignore_reason");
        update.unset("ignore_time");
        update.unset("ignore_author");
        defectMongoTemplate.updateMulti(query, update, CCNDefectEntity.class);
    }


    /**
     * 计算指定忽略类型的圈复杂度总和
     *
     * @param taskId
     * @param ignoreReasonType
     * @return
     */
    public Long calcSumOfCCNByIgnoreReasonType(long taskId, int ignoreReasonType) {
        Criteria criteria = new Criteria().andOperator(Criteria.where("task_id").in(taskId)
                        .and("ignore_reason_type").is(ignoreReasonType),
                Criteria.where("status").bits().allSet(ComConstants.DefectStatus.IGNORE.value()),
                Criteria.where("status").bits().allClear(ComConstants.DefectStatus.FIXED.value()));
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id")
                .sum("ccn").as("sum");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", DefectCountModel.class);
        List<DefectCountModel> defectCountModels = queryResult.getMappedResults();
        if (CollectionUtils.isEmpty(defectCountModels)) {
            return 0L;
        }
        return defectCountModels.get(0) == null ? 0L : defectCountModels.get(0).getSum();
    }

    /**
     * 根据 taskID buildId 与
     *
     * @param taskId
     * @param ignoreBuildId
     * @param ignoreReasonType
     * @return
     */
    public List<CCNDefectEntity> findIgnoreDefect(Long taskId, String ignoreBuildId, Integer ignoreReasonType,
            Boolean isFilterPath) {
        Criteria cri = getIgnoreTypeCri(taskId, ignoreReasonType, null);
        //增量
        if (isFilterPath != null && isFilterPath) {
            cri.and("ignore_build_id").is(ignoreBuildId);
        }
        Query query = Query.query(cri);
        query.fields().include("entityId", "task_id", "status", "ignore_reason_type", "severity", "ccn",
                "revision", "branch", "sub_module", "start_lines", "end_lines");
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    private Criteria getIgnoreTypeCri(Long taskId, Integer ignoreReasonType, Criteria otherCri) {
        Criteria cri = new Criteria();
        if (otherCri != null) {
            cri.andOperator(Criteria.where("task_id").is(taskId)
                            .and("ignore_reason_type").is(ignoreReasonType),
                    Criteria.where("status").bits().allSet(ComConstants.DefectStatus.IGNORE.value()),
                    Criteria.where("status").bits().allClear(ComConstants.DefectStatus.FIXED.value()),
                    otherCri);
        } else {
            cri.andOperator(Criteria.where("task_id").is(taskId)
                            .and("ignore_reason_type").is(ignoreReasonType),
                    Criteria.where("status").bits().allSet(ComConstants.DefectStatus.IGNORE.value()),
                    Criteria.where("status").bits().allClear(ComConstants.DefectStatus.FIXED.value()));
        }
        return cri;
    }

    public List<CCNDefectEntity> findAllIgnoreDefectForSnapshot(Set<String> defectIds, Integer ignoreReasonType) {
        if (CollectionUtils.isEmpty(defectIds)) {
            return Collections.emptyList();
        }
        Criteria cri = new Criteria();
        cri.andOperator(Criteria.where("entityId").in(defectIds)
                        .and("ignore_reason_type").is(ignoreReasonType),
                Criteria.where("status").bits().allSet(ComConstants.DefectStatus.IGNORE.value()),
                Criteria.where("status").bits().allClear(ComConstants.DefectStatus.FIXED.value()));
        Query query = Query.query(cri);
        query.fields().include("entityId", "task_id", "status", "ignore_reason_type", "severity", "ccn",
                "revision", "branch", "sub_module", "start_lines", "end_lines");
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    /**
     * 分页查询CCN
     *
     * @param taskIdList
     * @param author
     * @param status
     * @param fileList
     * @param ccnThresholds
     * @param defectIds
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @param buildId
     * @param startTimeStr
     * @param endTimeStr
     * @return first-> 告警数据, second-> 总数
     */
    public Pair<List<CCNDefectEntity>, Long> findDefectList(
            List<Long> taskIdList, String author, Set<Integer> status,
            Set<String> fileList, Set<Map.Entry<Integer, Integer>> ccnThresholds,
            Set<String> defectIds, Integer pageNum, Integer pageSize,
            String sortField, Sort.Direction sortType, String buildId,
            String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr,
            Set<Integer> ignoreReasonTypes
    ) {
        boolean isSnapshotQuery = StringUtils.isNotEmpty(buildId);
        Criteria criteria = getQueryCriteria(
                taskIdList, author, status,
                fileList, ccnThresholds,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr,
                startFixTimeStr, endFixTimeStr,
                ignoreReasonTypes
        );

        Query query = Query.query(criteria);
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        long totalCount = defectMongoTemplate.count(query, CCNDefectEntity.class);

        if ("create_build_number".equals(sortField)) {
            sortField = "create_time";
        }
        query.skip((long) pageNum * pageSize).limit(pageSize);
        query.with(Sort.by(sortType, sortField));
        List<CCNDefectEntity> defectList = defectMongoTemplate.find(query, CCNDefectEntity.class);

        return Pair.of(defectList, totalCount);
    }

    /**
     * 根据条件查询CCN告警（不分页）
     *
     * @param taskIdList
     * @param author
     * @param status
     * @param fileList
     * @param ccnThresholds
     * @param defectIds
     * @param buildId
     * @param startTimeStr
     * @param endTimeStr
     * @param ignoreReasonTypes
     * @param filedMap
     * @return
     */
    public List<CCNDefectEntity> findDefectByCondition(
            List<Long> taskIdList, String author, Set<Integer> status,
            Set<String> fileList, Set<Map.Entry<Integer, Integer>> ccnThresholds,
            Set<String> defectIds, String buildId, String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr,
            Set<Integer> ignoreReasonTypes, Map<String, Boolean> filedMap
    ) {
        boolean isSnapshotQuery = StringUtils.isNotEmpty(buildId);
        Criteria criteria = getQueryCriteria(
                taskIdList, author, status,
                fileList, ccnThresholds,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr,
                startFixTimeStr, endFixTimeStr,
                ignoreReasonTypes
        );
        Query query = new Query();
        if (MapUtils.isNotEmpty(filedMap)) {
            Document fieldsObj = new Document();
            filedMap.forEach((filed, isNeedReturn) -> fieldsObj.put(filed, isNeedReturn));
            query = new BasicQuery(new Document(), fieldsObj);
        }
        query.addCriteria(criteria);
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    /**
     * 根据条件查询CCN告警（分页）
     *
     * @param taskIdList
     * @param author
     * @param status
     * @param fileList
     * @param ccnThresholds
     * @param defectIds
     * @param buildId
     * @param startTimeStr
     * @param endTimeStr
     * @param ignoreReasonTypes
     * @param startFilePath
     * @param skip
     * @param pageSize
     * @param filedMap
     * @return
     */
    public List<CCNDefectEntity> findDefectByConditionWithFilePathPage(
            List<Long> taskIdList, String author, Set<Integer> status,
            Set<String> fileList, Set<Map.Entry<Integer, Integer>> ccnThresholds,
            Set<String> defectIds, String buildId, String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr,
            Set<Integer> ignoreReasonTypes, String startFilePath, Long skip, Integer pageSize,
            Map<String, Boolean> filedMap
    ) {
        boolean isSnapshotQuery = StringUtils.isNotEmpty(buildId);
        Criteria criteria = getQueryCriteria(
                taskIdList, author, status,
                fileList, ccnThresholds,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr,
                startFixTimeStr, endFixTimeStr,
                ignoreReasonTypes
        );
        if (startFilePath != null) {
            criteria.and("rel_path").gte(startFilePath);
        }
        Query query = new Query();
        if (MapUtils.isNotEmpty(filedMap)) {
            Document fieldsObj = new Document();
            filedMap.forEach((filed, isNeedReturn) -> fieldsObj.put(filed, isNeedReturn));
            query = new BasicQuery(new Document(), fieldsObj);
        }
        query.addCriteria(criteria);
        int pageSizeValue = pageSize == null || pageSize <= 0 ? 100 : pageSize;
        long skipValue = skip == null || skip < 0 ? 0L : skip;
        query.with(Sort.by(Sort.Direction.ASC, "rel_path"));
        query.limit(pageSizeValue);
        if (skipValue > 0) {
            query.skip(skipValue);
        }
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    private Criteria getQueryCriteria(
            List<Long> taskIdList, String author, Set<Integer> status,
            Set<String> fileList, Set<Map.Entry<Integer, Integer>> ccnThresholds,
            Set<String> defectIds, boolean isSnapshotQuery,
            String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr,
            Set<Integer> ignoreReasonTypes
    ) {
        Criteria magicEmptyCriteria = Criteria.where("task_id").is(-1L);

        if (isSnapshotQuery && CollectionUtils.isEmpty(defectIds)) {
            return magicEmptyCriteria;
        }

        List<Criteria> rootCriteriaList = new LinkedList<>();
        rootCriteriaList.add(Criteria.where("task_id").in(taskIdList));

        // 路径过滤
        if (CollectionUtils.isNotEmpty(fileList)) {
            List<Criteria> innerOrCriteriaList = new ArrayList<>();
            fileList.forEach(file ->
                    innerOrCriteriaList.add(Criteria.where("rel_path").regex(file))
            );
            rootCriteriaList.add(new Criteria().orOperator(innerOrCriteriaList.toArray(new Criteria[0])));
        }

        // 状态过滤
        if (CollectionUtils.isNotEmpty(status)) {
            Criteria statusCriteria = getStatusCriteria(status, isSnapshotQuery, ignoreReasonTypes);
            if (statusCriteria != null) {
                rootCriteriaList.add(statusCriteria);
            } else {
                return magicEmptyCriteria;
            }
        }

        // 快照
        if (CollectionUtils.isNotEmpty(defectIds)) {
            List<ObjectId> objectIdList = defectIds.stream().map(ObjectId::new).collect(Collectors.toList());
            rootCriteriaList.add(Criteria.where("_id").in(objectIdList));
        }

        // 作者过滤
        if (StringUtils.isNotEmpty(author)) {
            rootCriteriaList.add(Criteria.where("author").is(author));
        }

        // 圈复杂度过滤
        if (CollectionUtils.isNotEmpty(ccnThresholds)) {
            List<Criteria> thresholdFilter = new LinkedList<>();

            for (Map.Entry<Integer, Integer> ccnThreshold : ccnThresholds) {
                Integer lThreshold = ccnThreshold.getKey();
                Integer hThreshold = ccnThreshold.getValue();
                if (hThreshold != null && lThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").gte(lThreshold).lt(hThreshold));
                } else if (hThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").lt(hThreshold));
                } else if (lThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").gte(lThreshold));
                }
            }

            if (CollectionUtils.isNotEmpty(thresholdFilter)) {
                rootCriteriaList.add(new Criteria().orOperator(thresholdFilter.toArray(new Criteria[0])));
            }
        }

        // 按创建日期过滤
        Criteria creatTimeCri = getStartEndTimeStampCri("latest_datetime",
                startTimeStr, endTimeStr);
        if (creatTimeCri != null) {
            rootCriteriaList.add(creatTimeCri);
        }

        //按修复日期过滤
        Criteria fixTimeCri = getStartEndTimeStampCri("fixed_time",
                startFixTimeStr, endFixTimeStr);
        if (fixTimeCri != null) {
            rootCriteriaList.add(fixTimeCri);
        }

        return new Criteria().andOperator(rootCriteriaList.toArray(new Criteria[0]));
    }

    /**
     * 根据查询字段和时间范围定制查询规则
     *
     * @param field        查询字段
     * @param startTimeStr 查询起始时间
     * @param endTimeStr   查询结束时间
     * @return 查询规则
     */
    private Criteria getStartEndTimeStampCri(String field, String startTimeStr, String endTimeStr) {
        long minTime = 0L;
        long maxTime = 0L;
        if (StringUtils.isNotEmpty(startTimeStr)) {
            minTime = DateTimeUtils.getTimeStamp(startTimeStr + " 00:00:00");
            maxTime = StringUtils.isEmpty(endTimeStr) ? System.currentTimeMillis()
                    : DateTimeUtils.getTimeStamp(endTimeStr + " 23:59:59");
        }

        if (minTime != 0 && maxTime == 0) {
            return new Criteria().and(field).gte(minTime);
        } else if (minTime == 0 && maxTime != 0) {
            return new Criteria().and(field).lt(maxTime).gt(0);
        } else if (minTime != 0 && maxTime != 0) {
            return new Criteria().and(field).lt(maxTime).gte(minTime);
        }
        return null;
    }

    /**
     * 状态后置处理
     *
     * @param statusFilter 前端传入的值是1、2、4、8
     * @param isSnapshotQuery 是否快照查询
     * @param ignoreReasonTypes 忽略类型
     * @return
     */
    private Criteria getStatusCriteria(
            Set<Integer> statusFilter,
            boolean isSnapshotQuery,
            Set<Integer> ignoreReasonTypes
    ) {
        if (CollectionUtils.isEmpty(statusFilter)) {
            return new Criteria();
        }

        // 若是快照查，选中待修复就必须补偿上已修复；真实状态会在postHandleCCNDefect()后置处理
        if (isSnapshotQuery) {
            Integer newStatusStr = DefectStatus.NEW.value();
            Integer fixedStatusStr = DefectStatus.FIXED.value();
            if (statusFilter.contains(newStatusStr)) {
                statusFilter.add(newStatusStr);
                statusFilter.add(fixedStatusStr);
            } else {
                // 快照查，不存在已修复
                statusFilter.remove(fixedStatusStr);
            }
        }

        if (CollectionUtils.isEmpty(statusFilter)) {
            return null;
        }

        LinkedList<Criteria> rootCriteriaList = Lists.newLinkedList();

        // 忽略类型处理
        boolean hasIgnore = statusFilter.contains(DefectStatus.IGNORE.value());

        if (hasIgnore) {
            if (CollectionUtils.isNotEmpty(ignoreReasonTypes)) {
                rootCriteriaList.add(
                        Criteria.where("status").bits().allSet(DefectStatus.IGNORE.value())
                                .and("ignore_reason_type").in(ignoreReasonTypes)
                );
            } else {
                rootCriteriaList.add(
                        Criteria.where("status").bits().allSet(DefectStatus.IGNORE.value())
                );
            }

            statusFilter.remove(DefectStatus.IGNORE.value());
            statusFilter.remove(DefectStatus.IGNORE.value() | DefectStatus.NEW.value());
        }

        if (CollectionUtils.isNotEmpty(statusFilter)) {
            Set<Integer> condStatusSet = statusFilter.stream()
                    .map(x -> x | DefectStatus.NEW.value())
                    .collect(Collectors.toSet());

            rootCriteriaList.add(Criteria.where("status").in(condStatusSet));
        }

        return rootCriteriaList.size() == 1 ? rootCriteriaList.get(0)
                : new Criteria().orOperator(rootCriteriaList.toArray(new Criteria[]{}));
    }

    /**
     * 统计任务的缺陷状态分布
     */
    public List<CCNDefectGroupStatisticVO> statisticDefectCountByStatus(
            List<Long> taskIdList, String author, Set<String> fileList,
            Set<String> defectIds, boolean isSnapshotQuery,
            String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr

    ) {
        Criteria criteria = getQueryCriteria(
                taskIdList, author, null, fileList, null,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr,
                startFixTimeStr, endFixTimeStr,
                null
        );

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("status")
                .first("status").as("status")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<CCNDefectGroupStatisticVO> queryResult = defectMongoTemplate.aggregate(agg,
                "t_ccn_defect", CCNDefectGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }

    public long countByCondition(
            List<Long> taskIdList, String author, Set<Integer> statusSet, Set<String> fileList,
            Set<Map.Entry<Integer, Integer>> ccnThresholds,
            Set<String> defectIds, boolean isSnapshotQuery,
            String startTimeStr, String endTimeStr,
            String startFixTimeStr, String endFixTimeStr
    ) {
        Criteria criteria = getQueryCriteria(
                taskIdList, author, statusSet, fileList, ccnThresholds,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr,
                startFixTimeStr, endFixTimeStr,
                null
        );

        return defectMongoTemplate.count(Query.query(criteria), CCNDefectEntity.class);
    }

    public List<Long> findTaskIdsByEntityIds(Set<String> entityIds) {
        Criteria cri = Criteria.where("_id").in(entityIds.stream().map(ObjectId::new).collect(Collectors.toSet()));
        MatchOperation match = Aggregation.match(cri);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CCNDefectEntity> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", CCNDefectEntity.class);
        if (CollectionUtils.isEmpty(queryResult.getMappedResults())) {
            return Collections.emptyList();
        }
        return queryResult.getMappedResults().stream().map(CCNDefectEntity::getTaskId).collect(Collectors.toList());
    }

    /**
     * 统计指定忽略类型的告警数
     */
    public Long getIgnoreTypeDefectCount(Set<Long> taskIds, Integer ignoreTypeId, Integer ignoreStatus) {
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("status").is(ignoreStatus)
                .and("ignore_reason_type").is(ignoreTypeId);
        Query query = Query.query(criteria);
        // 指定索引
        query.withHint("idx_taskid_1_status_1_ignore_reason_type_1");
        return defectMongoTemplate.count(query, CCNDefectEntity.class);
    }

    public List<IgnoreTypeStatModel> statisticIgnoreDefectByIgnoreTypeId(Set<Long> taskIds,
            Set<Integer> ignoreTypeIds, Integer ignoreStatus) {
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("status").is(ignoreStatus)
                .and("ignore_reason_type").in(ignoreTypeIds);
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("ignore_reason_type")
                .first("ignore_reason_type").as("ignoreTypeId")
                .addToSet("task_id").as("taskIdSet")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    public List<Long> statisticTaskIdByNewDefect(Set<Long> taskIds) {
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("status").is(DefectStatus.NEW.value());
        return statisticTaskIdByCri(criteria);
    }

    public List<Long> statisticTaskIdByNewDefectId(Set<String> entityIds) {
        Criteria criteria = Criteria.where("_id").in(entityIds.stream().map(ObjectId::new).collect(Collectors.toSet()))
                .and("status").is(DefectStatus.NEW.value());
        return statisticTaskIdByCri(criteria);
    }

    private List<Long> statisticTaskIdByCri(Criteria criteria) {
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("taskId");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", IgnoreTypeStatModel.class);
        List<IgnoreTypeStatModel> results = queryResult.getMappedResults();
        if (CollectionUtils.isEmpty(results)) {
            return Collections.emptyList();
        }
        return results.stream().map(IgnoreTypeStatModel::getTaskId).collect(Collectors.toList());
    }

    /**
     * 根据传入条件统计文件数
     *
     * @param taskId
     * @param statusSet
     * @param author
     * @return
     */
    public long countFileByCondition(
            long taskId,
            Set<Integer> statusSet,
            String author,
            Set<Map.Entry<Integer, Integer>> ccnThresholdSet
    ) {
        List<Criteria> rootCriteriaList = Lists.newArrayList();
        rootCriteriaList.add(Criteria.where("task_id").is(taskId));

        if (CollectionUtils.isNotEmpty(statusSet)) {
            rootCriteriaList.add(Criteria.where("status").in(statusSet));
        }

        if (CollectionUtils.isNotEmpty(ccnThresholdSet)) {
            List<Criteria> thresholdFilter = new LinkedList<>();

            for (Map.Entry<Integer, Integer> ccnThreshold : ccnThresholdSet) {
                Integer lThreshold = ccnThreshold.getKey();
                Integer rThreshold = ccnThreshold.getValue();
                if (rThreshold != null && lThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").gte(lThreshold).lt(rThreshold));
                } else if (rThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").lt(rThreshold));
                } else if (lThreshold != null) {
                    thresholdFilter.add(Criteria.where("ccn").gte(lThreshold));
                }
            }

            if (CollectionUtils.isNotEmpty(thresholdFilter)) {
                rootCriteriaList.add(new Criteria().orOperator(thresholdFilter.toArray(new Criteria[0])));
            }
        }

        if (StringUtils.isNotEmpty(author)) {
            rootCriteriaList.add(Criteria.where("author").is(author));
        }

        MatchOperation match = Aggregation.match(new Criteria().andOperator(rootCriteriaList.toArray(new Criteria[0])));
        GroupOperation group = Aggregation.group("task_id", "file_path");
        CountOperation count = Aggregation.count().as("defectCount");
        AggregationOptions allowDiskUse = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group, count).withOptions(allowDiskUse);
        AggregationResults<DefectCountModel> result =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", DefectCountModel.class);

        if (CollectionUtils.isNotEmpty(result.getMappedResults())) {
            return result.getMappedResults().get(0).getDefectCount();
        }

        return 0L;
    }

    /**
     * 根据指定条件获取忽略状态的告警列表
     *
     * @param taskId
     * @param pageable
     * @param fieldMap
     * @return
     */
    public List<CCNDefectEntity> findIgnoreDefects(
            Long taskId, Pageable pageable,
            Map<String, Boolean> fieldMap
    ) {
        Query query;
        if (MapUtils.isNotEmpty(fieldMap)) {
            Document fieldsObj = new Document();
            fieldsObj.putAll(fieldMap);
            query = new BasicQuery(new Document(), fieldsObj);
        } else {
            query = new Query();
        }

        query.addCriteria(
                Criteria.where("task_id").is(taskId)
                        .and("status").is(DefectStatus.NEW.value() | DefectStatus.IGNORE.value())
        );
        query.with(pageable);

        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    public long batchExcludeToolNewDefect(Long taskId, String toolName) {
        if (taskId == null || StringUtils.isBlank(toolName)) {
            return 0L;
        }
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("status").is(DefectStatus.NEW.value()));
        long curTime = System.currentTimeMillis();
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value() | DefectStatus.CHECKER_MASK.value());
        update.set("exclude_time", curTime);
        update.set("updated_date", curTime);
        return defectMongoTemplate.updateMulti(query, update, CCNDefectEntity.class).getModifiedCount();
    }
}
