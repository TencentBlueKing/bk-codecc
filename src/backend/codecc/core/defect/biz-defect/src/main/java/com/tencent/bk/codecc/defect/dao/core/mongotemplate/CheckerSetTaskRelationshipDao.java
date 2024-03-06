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

package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskCountEntity;
import java.util.List;
import java.util.Set;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * 规则集与其他对象关联DAO
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Repository
public class CheckerSetTaskRelationshipDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;

    /**
     * 按规则集Id分组，统计各个规则集的使用数
     *
     * @return list
     */
    public List<CheckerSetTaskCountEntity> countCheckerSetGroupByCheckerSetId(Set<String> checkerSetIds) {
        if (CollectionUtils.isEmpty(checkerSetIds)) {
            return Lists.newArrayList();
        }

        MatchOperation match = Aggregation.match(Criteria.where("checker_set_id").in(checkerSetIds));
        GroupOperation group = Aggregation.group("checker_set_id").first("checker_set_id").as("checkerSetId")
                .count().as("taskInUseCount");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CheckerSetTaskCountEntity> results =
                defectCoreMongoTemplate.aggregate(agg, "t_checker_set_task_relationship",
                        CheckerSetTaskCountEntity.class);
        return results.getMappedResults();
    }
}
