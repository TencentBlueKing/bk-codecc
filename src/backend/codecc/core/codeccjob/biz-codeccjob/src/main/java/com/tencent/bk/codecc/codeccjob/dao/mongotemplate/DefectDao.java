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

package com.tencent.bk.codecc.codeccjob.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 告警持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */

@Repository
public class DefectDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 按规则统计指定状态告警数
     *
     * @param taskIdSet 任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @param checkers  规则集合
     * @return list
     */
    public List<CheckerStatisticEntity> findStatByTaskIdAndToolChecker(Collection<Long> taskIdSet, String toolName,
                                                                       List<Integer> status, Collection<String> checkers) {
        // 索引筛选
        MatchOperation matchIdx = Aggregation
                .match(Criteria.where("task_id").in(taskIdSet).and("tool_name").is(toolName).and("status").in(status));
        // 普通筛选
        MatchOperation matchAft = Aggregation.match(Criteria.where("checker_name").in(checkers));

        GroupOperation group =
                Aggregation.group("checker_name").first("checker_name").as("id").count().as("defect_count");
        Aggregation agg = Aggregation.newAggregation(matchIdx, matchAft, group);
        return mongoTemplate.aggregate(agg, "t_defect", CheckerStatisticEntity.class).getMappedResults();
    }

    /**
     * 按告警数倒序获取skip个后面的size个任务id及告警数
     */
    public List<DefectCountModel> statisticCommonDefect(Set<Long> taskIdSet, String toolName, long skipNum,
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
                mongoTemplate.aggregate(agg, "t_defect", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    /**
     * 批量更新告警状态的exclude位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusExcludeBit(long taskId, List<CommonDefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
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

    /**
     * 查询符合条件的缺陷告警
     * 仅返回：status、exclude_time
     *
     * @param taskId
     * @param excludeStatusSet
     * @param filterPaths
     * @param pageSize
     * @param lastId
     * @return
     */
    public List<CommonDefectEntity> findDefectsByFilePath(Long taskId,
                                                          String toolName,
                                                          Set<Integer> excludeStatusSet,
                                                          Set<String> filterPaths,
                                                          int pageSize,
                                                          String lastId) {
        Document fieldsObj = new Document();
        fieldsObj.put("status", true);
        fieldsObj.put("exclude_time", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").nin(excludeStatusSet));

        Criteria orOperator = new Criteria();
        orOperator.orOperator(
                filterPaths.stream().map(file -> Criteria.where("file_path_name").regex(file))
                        .toArray(Criteria[]::new));
        query.addCriteria(orOperator);

        if (StringUtils.isNotEmpty(lastId)) {
            query.addCriteria(Criteria.where(MongoPageHelper.ID).gt(new ObjectId(lastId)));
        }
        query.with(Sort.by(Sort.Direction.ASC, MongoPageHelper.ID)).limit(pageSize);

        return mongoTemplate.find(query, CommonDefectEntity.class);
    }

    /**
     * 按告警实体id查询告警指定字段信息
     *
     * @param taskId      任务id
     * @param defectIdSet 告警实体id
     * @return list
     */
    public List<CommonDefectEntity> findByTaskAndEntityIdSet(long taskId, Set<String> defectIdSet) {
        if (CollectionUtils.isEmpty(defectIdSet)) {
            return Collections.emptyList();
        }

        Document fieldsObj = new Document();
        fieldsObj.put("id", true);
        fieldsObj.put("tool_name", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        Set<ObjectId> entityIdSet = defectIdSet.stream().map(ObjectId::new).collect(Collectors.toSet());
        query.addCriteria(Criteria.where("task_id").is(taskId).and("_id").in(entityIdSet));

        return mongoTemplate.find(query, CommonDefectEntity.class);
    }

    /**
     * 统计告警数
     */
    public Long countCommonDefectByStatus(List<Long> taskIdSet, Set<String> toolNames, Integer status, String timeField,
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

        return mongoTemplate.count(new Query(criteria), "t_defect");
    }
}
