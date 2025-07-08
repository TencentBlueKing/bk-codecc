/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.DefectAuthorGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

/**
 * 圈复杂度持久代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Repository
public class CCNDefectDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 按作者维度查询统计数据
     */
    public List<DefectAuthorGroupStatisticVO> findStatisticGroupByAuthor(long taskId, int status) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").is(taskId).and("status").is(status);
        MatchOperation match = Aggregation.match(criteria);

        // 以author进行分组
        GroupOperation group = Aggregation.group("task_id", "author")
                .last("author").as("authorObj")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectAuthorGroupStatisticVO> queryResult = defectMongoTemplate
                .aggregate(agg, "t_ccn_defect", DefectAuthorGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 按告警数倒序获取skip个后面的size个任务id及告警数
     *
     * @param taskIdSet 任务id集合
     * @param skipNum 跳过多少个数
     * @param size 取多少个数
     * @return list
     */
    public List<DefectCountModel> statisticCCNDefect(Set<Long> taskIdSet, long skipNum, long size) {
        // 根据查询条件过滤
        Criteria criteria = new Criteria();
        criteria.and("task_id").in(taskIdSet).and("status").is(ComConstants.DefectStatus.NEW.value());

        MatchOperation match = Aggregation.match(criteria);

        // 以task_id进行分组
        GroupOperation group = Aggregation.group("task_id")
                .last("task_id").as("taskId")
                .count().as("defectCount");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "defectCount");
        SkipOperation skip = Aggregation.skip(skipNum);
        LimitOperation limit = Aggregation.limit(size);
        Aggregation agg = Aggregation.newAggregation(match, group, sort, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_ccn_defect", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    /**
     * 批量更新告警状态的exclude位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusExcludeBit(long taskId, List<CCNDefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CCNDefectEntity.class);
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
     * 查询符合条件的圈复杂度告警
     * 仅返回：status、exclude_time
     *
     * @param taskId
     * @param excludeStatusSet
     * @param filterPaths
     * @param pageSize
     * @param lastId
     * @return
     */
    public List<CCNDefectEntity> findDefectsByFilePath(Long taskId,
            Set<Integer> excludeStatusSet,
            Set<String> filterPaths,
            int pageSize,
            String lastId) {
        Document fieldsObj = new Document();
        fieldsObj.put("status", true);
        fieldsObj.put("exclude_time", true);

        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("status").nin(excludeStatusSet));

        Criteria orOperator = new Criteria();
        orOperator.orOperator(
                filterPaths.stream().map(file -> Criteria.where("file_path").regex(file)).toArray(Criteria[]::new));
        query.addCriteria(orOperator);

        if (StringUtils.isNotEmpty(lastId)) {
            query.addCriteria(Criteria.where(MongoPageHelper.ID).gt(new ObjectId(lastId)));
        }
        query.with(Sort.by(Sort.Direction.ASC, MongoPageHelper.ID)).limit(pageSize);
        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }

    /**
     * 按告警实体id查询告警指定字段信息
     *
     * @param taskId 任务id
     * @param defectIdSet 告警实体id
     * @return list
     */
    public List<CCNDefectEntity> findByTaskAndEntityIdSet(long taskId, Set<String> defectIdSet) {
        if (CollectionUtils.isEmpty(defectIdSet)) {
            return Collections.emptyList();
        }

        Document fieldsObj = new Document();
        fieldsObj.put("id", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        Set<ObjectId> entityIdSet = defectIdSet.stream().map(ObjectId::new).collect(Collectors.toSet());
        query.addCriteria(Criteria.where("task_id").is(taskId).and("_id").in(entityIdSet));

        return defectMongoTemplate.find(query, CCNDefectEntity.class);
    }
}
