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

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工具构建信息Dao
 *
 * @version V1.0
 * @date 2021/12/29
 */

@Slf4j
@Repository
public class ToolBuildInfoDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 查询最新build id
     *
     * @param taskIdSet 任务id集合
     * @return list
     */
    public List<ToolBuildInfoEntity> findLatestBuildIdByTaskIdSet(Collection<Long> taskIdSet) {
        long start = System.currentTimeMillis();

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteria.and("task_id").in(taskIdSet);
        }
//        criteria.and("updated_date").exists(true);

        MatchOperation match = Aggregation.match(criteria);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updated_date");

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("defect_base_build_id").as("defect_base_build_id");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);
        AggregationResults<ToolBuildInfoEntity> results =
                mongoTemplate.aggregate(agg, "t_tool_build_info", ToolBuildInfoEntity.class);

        log.info("task id count: {} ,elapse time: {}ms", taskIdSet.size(), System.currentTimeMillis() - start);
        return results.getMappedResults();
    }
}
