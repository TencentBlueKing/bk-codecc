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
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticExtEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.DefectAuthorGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class LintDefectV2Dao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public void batchUpdateDefectAuthor(long taskId, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", defectEntity.getAuthor());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的exclude位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusExcludeBit(long taskId, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);
            defectList.forEach(defectEntity -> {
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("exclude_time", defectEntity.getExcludeTime());
                update.set("mask_path", defectEntity.getMaskPath());

                Query query = new Query(
                        Criteria.where("task_id").is(taskId)
                                .and("_id").is(new ObjectId(defectEntity.getEntityId()))
                );

                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public List<LintDefectV2Entity> findDefectsByFilePath(Long taskId,
                                                          String toolName,
                                                          Set<Integer> excludeStatusSet,
                                                          Set<String> filterPaths,
                                                          int pageSize,
                                                          String lastId) {
        Document fieldsObj = new Document();
        fieldsObj.put("status", true);
        fieldsObj.put("exclude_time", true);
        fieldsObj.put("severity", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").nin(excludeStatusSet));

        Criteria orOperator = new Criteria();
        orOperator.orOperator(
                filterPaths.stream().map(file -> Criteria.where("file_path").regex(file)).toArray(Criteria[]::new));
        query.addCriteria(orOperator);

        if (StringUtils.isNotEmpty(lastId)) {
            query.addCriteria(Criteria.where(MongoPageHelper.ID).gt(new ObjectId(lastId)));
        }
        query.with(Sort.by(Sort.Direction.ASC, MongoPageHelper.ID)).limit(pageSize)
                .withHint("idx_taskid_1_toolname_1_status_1_filepath_1");

        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 按维度的工具集合，文件绝对路径查询告警id
     *
     * @param taskId      任务id
     * @param toolNameSet 工具集合
     * @param status      状态
     * @param fileList    文件绝对路径列表
     * @return list
     */
    public List<LintDefectV2Entity> findDefectsByFilePath(Long taskId, Set<String> toolNameSet, Integer status,
            Set<String> fileList) {
        Document fieldsObj = new Document();
        fieldsObj.put("tool_name", true);
        fieldsObj.put("id", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").in(toolNameSet).and("status").is(status)
                .and("file_path").in(fileList));

//        Criteria orOperator = new Criteria();
//        orOperator.orOperator(
//                fileList.stream().map(file -> Criteria.where("file_path").regex(file)).toArray(Criteria[]::new));
//        query.addCriteria(orOperator);

        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 按规则统计告警数
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @param checkers  规则集合
     * @return list
     */
    public List<CheckerStatisticEntity> findStatByTaskIdAndToolChecker(Collection<Long> taskIdSet, String toolName,
                                                                       int status, Collection<String> checkers) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName)
                .and("status").is(status).and("checker").in(checkers));

        GroupOperation group = Aggregation.group("checker")
                .first("checker").as("id")
                .count().as("defect_count");
        Aggregation agg = Aggregation.newAggregation(match, group);
        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", CheckerStatisticEntity.class)
                .getMappedResults();
    }

    public List<CheckerStatisticExtEntity> findStatByTaskIdAndToolChecker(Collection<Long> taskIdSet, String toolName,
            Set<Integer> status, String checker, long statStartTime, long statEndTime) {

        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet)
                .and("tool_name").is(toolName)
                .and("status").in(status)
                .and("checker").is(checker)
                .and("create_time").gte(statStartTime).lt(statEndTime));

        GroupOperation group = Aggregation.group("status")
                .first("status").as("status")
                .count().as("defect_count");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", CheckerStatisticExtEntity.class)
                .getMappedResults();
    }


    /**
     * 按作者维度查询统计数据
     *
     * @param taskId
     * @param toolNameSet
     * @param checkerKeys
     * @return
     */
    public List<DefectAuthorGroupStatisticVO> findStatisticGroupByAuthor(
            long taskId,
            List<String> toolNameSet,
            List<String> checkerKeys
    ) {
        List<DefectAuthorGroupStatisticVO> aggList = findStatisticGroupByAuthorCore(
                taskId,
                toolNameSet,
                DefectStatus.NEW.value(),
                checkerKeys
        );

        if (CollectionUtils.isEmpty(aggList)) {
            return aggList;
        }

        // 告警作者类型可能List，需进一步拆分处理；对于List类型的告警作者，聚合出来是逗号分割的，比如："A,B"
        HashMap<String, DefectAuthorGroupStatisticVO> map = Maps.newHashMapWithExpectedSize(aggList.size());

        for (DefectAuthorGroupStatisticVO agg : aggList) {

            if (StringUtils.isEmpty(agg.getAuthorName())) {
                continue;
            }

            String authorName = agg.getAuthorName();
            int defectCount = agg.getDefectCount();

            if (authorName.contains(",")) {
                String[] authorSplitArray = agg.getAuthorName().split(",");
                for (String authorSplit : authorSplitArray) {
                    DefectAuthorGroupStatisticVO existObj =
                            map.computeIfAbsent(authorSplit, key -> new DefectAuthorGroupStatisticVO(key, 0));
                    existObj.setDefectCount(existObj.getDefectCount() + defectCount);
                }
            } else {
                DefectAuthorGroupStatisticVO existObj =
                        map.computeIfAbsent(authorName, key -> new DefectAuthorGroupStatisticVO(key, 0));
                existObj.setDefectCount(existObj.getDefectCount() + defectCount);
            }
        }

        return Lists.newArrayList(map.values());
    }

    /**
     * 按作者维度查询统计数据
     *
     * @param taskId
     * @param toolNameSet
     * @param status
     * @param checkerKeys
     * @return
     */
    private List<DefectAuthorGroupStatisticVO> findStatisticGroupByAuthorCore(
            long taskId,
            List<String> toolNameSet,
            int status,
            List<String> checkerKeys
    ) {
        Criteria criteria = Criteria.where("task_id").is(taskId)
                .and("tool_name").in(toolNameSet)
                .and("status").is(status)
                .and("checker").in(checkerKeys);

        MatchOperation match = Aggregation.match(criteria);

        // 以author进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "author")
                .last("author").as("authorObj")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectAuthorGroupStatisticVO> queryResult = mongoTemplate
                .aggregate(agg, "t_lint_defect_v2", DefectAuthorGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 按告警数倒序获取skip个后面的size个任务id及告警数
     */
    public List<DefectCountModel> statisticLintDefect(Set<Long> taskIdSet, String toolName, long skipNum,
                                                      long size) {
        // 根据查询条件过滤
        Criteria criteria = new Criteria();
        criteria.and("task_id").in(taskIdSet).and("status").is(ComConstants.DefectStatus.NEW.value());
        if (StringUtils.isNotEmpty(toolName)) {
            criteria.and("tool_name").is(toolName);
        }
        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id")
                .last("task_id").as("taskId")
                .count().as("defectCount");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "defectCount");
        SkipOperation skip = Aggregation.skip(skipNum);
        LimitOperation limit = Aggregation.limit(size);
        Aggregation agg = Aggregation.newAggregation(match, group, sort, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                mongoTemplate.aggregate(agg, "t_lint_defect_v2", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    /**
     * 按告警实体id查询告警指定字段信息
     *
     * @param taskId      任务id
     * @param defectIdSet 告警实体id
     * @return list
     */
    public List<LintDefectV2Entity> findByTaskAndEntityIdSet(long taskId, Set<String> defectIdSet) {
        if (CollectionUtils.isEmpty(defectIdSet)) {
            return Collections.emptyList();
        }

        Document fieldsObj = new Document();
        fieldsObj.put("tool_name", true);
        fieldsObj.put("id", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        Set<ObjectId> entityIdSet = defectIdSet.stream().map(ObjectId::new).collect(Collectors.toSet());
        query.addCriteria(Criteria.where("task_id").is(taskId).and("_id").in(entityIdSet));
        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 统计告警数
     */
    public Long countLintDefectByStatus(List<Long> taskIdSet, Set<String> toolNames, Integer status, String timeField,
            long timeStart, long timeEnd) {
        // 根据查询条件过滤
        Criteria criteria = Criteria.where("task_id").in(taskIdSet);

        if (CollectionUtils.isNotEmpty(toolNames)) {
            criteria.and("tool_name").in(toolNames);
        }
        if (null != status && status > 0) {
            criteria.and("status").is(status);
        }

        if (StringUtils.isNotEmpty(timeField)) {
            criteria.and(timeField).gte(timeStart).lte(timeEnd);
        }

        return mongoTemplate.count(new Query(criteria), "t_lint_defect_v2");
    }

    /**
     * 按忽略类型统计告警数
     *
     * @param startTime 告警创建时间
     * @param endTime   告警创建时间
     * @return group list
     */
    public List<LintDefectV2Entity> statIgnoreGroupByReasonType(Collection<Long> taskIds, String toolName,
            String checker, int status, long startTime, long endTime) {
        // 筛选create_time范围内的
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(toolName)
                .and("checker").is(checker)
                .and("status").is(status)
                .and("create_time").gte(startTime).lt(endTime));

        // 统计每个忽略类型的告警数量
        GroupOperation group = Aggregation.group("ignore_reason_type")
                .first("ignore_reason_type").as("ignore_reason_type")
                .count().as("line_num");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);

        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Entity.class).getMappedResults();
    }

    /**
     * 按规则、创建时间批量分组统计
     *
     * @param taskIdSet   任务id集合
     * @param toolName    有效工具
     * @param status      告警状态
     * @param timeField   状态时间字段
     * @param startTime   开始时间13位
     * @param endTime     结束时间13位
     * @return stat list
     */
    public List<LintDefectV2Entity> statByCheckerAndStatus(Set<Long> taskIdSet, String toolName, int status,
            String timeField, long startTime, long endTime) {
        // 必填项
        Criteria criteria = Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName);

        if (status > 0) {
            criteria.and("status").is(status);
        }

        if (StringUtils.isNotBlank(timeField)) {
            criteria.and(timeField).gte(startTime).lt(endTime);
        }

        MatchOperation match = Aggregation.match(criteria);

        // 统计每个工具规则的告警数量
        GroupOperation group = Aggregation.group("task_id", "checker", "create_time")
                .first("task_id").as("task_id")
                .first("checker").as("checker")
                .first("create_time").as("create_time");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);

        return mongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Entity.class).getMappedResults();
    }

    /**
     * 查询有评论的告警清单
     * @param taskIdSet 任务id集合
     * @param toolName  工具
     * @return list
     */
    public List<LintDefectV2Entity> findDefectHasComment(Set<Long> taskIdSet, String toolName) {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("checker", true);
        fieldsObj.put("code_comment", true);
        fieldsObj.put("create_time", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(
                Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName).and("code_comment").ne(null));

        return mongoTemplate.find(query, LintDefectV2Entity.class);
    }
}
