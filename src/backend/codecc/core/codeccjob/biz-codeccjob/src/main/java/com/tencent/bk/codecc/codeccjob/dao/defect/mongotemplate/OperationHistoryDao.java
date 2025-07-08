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

package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

/**
 * 操作记录Dao
 *
 * @version V1.0
 * @date 2022/3/30
 */

@Repository
public class OperationHistoryDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;


    /**
     * 筛选指定范围内，指定操作类型的任务id
     *
     * @param createStart 创建时间开始
     * @param createEnd 创建日期结束
     * @param funcIds 指定操作类型
     * @return taskIds
     */
    public List<OperationHistoryEntity> findByCreateDateAndFuncId(long createStart, long createEnd,
            Set<String> funcIds) {
        Criteria criteria = new Criteria();

        // 指定时间范围内的
        if (createStart > 0 && createEnd > 0) {
            criteria.and("create_date").gte(createStart).lte(createEnd);
        }

        // 指定操作类型的
        if (CollectionUtils.isNotEmpty(funcIds)) {
            criteria.and("func_id").in(funcIds);
        }

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id", "tool_name")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name");
        Aggregation agg = Aggregation.newAggregation(match, group);

        return defectMongoTemplate.aggregate(agg, "t_operation_history", OperationHistoryEntity.class)
                .getMappedResults();
    }

    public List<OperationHistoryEntity> findByTaskIdInAndTimeGreaterThan(Collection<Long> taskIds, long gtTime) {
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("time").gt(gtTime);

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("time").as("time");
        Aggregation agg = Aggregation.newAggregation(match, group);

        return defectMongoTemplate.aggregate(agg, "t_operation_history", OperationHistoryEntity.class)
                .getMappedResults();
    }
}
