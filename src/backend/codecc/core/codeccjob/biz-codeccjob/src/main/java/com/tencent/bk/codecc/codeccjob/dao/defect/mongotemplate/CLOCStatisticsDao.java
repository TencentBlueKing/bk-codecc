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

package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

/**
 * cloc统计持久类
 *
 * @version V1.0
 * @date 2021/8/30
 */
@Repository
public class CLOCStatisticsDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 获取skip个后面的size个任务的每个任务最新一次构建的代码行数
     *
     * @param taskIdSet 任务id集合
     * @param buildIdSet 构建id集合
     * @param toolName 工具名
     * @param skipNum 跳过个数
     * @param size 任务个数
     * @return list
     */
    public List<DefectCountModel> statistiSCCDefect(Set<Long> taskIdSet, Set<String> buildIdSet, String toolName,
            int skipNum, int size) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteriaList.add(Criteria.where("task_id").in(taskIdSet));
        }

        // 筛选tool_name
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").in(toolName));
        }

        // 筛选build_id
        if (CollectionUtils.isNotEmpty(buildIdSet)) {
            criteriaList.add(Criteria.where("build_id").in(buildIdSet));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        GroupOperation group =
                Aggregation.group("task_id")
                        .first("task_id").as("taskId")
                        .first("build_id").as("build_id")
                        .sum("sum_code").as("defectCount");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "defectCount");
        SkipOperation skip = Aggregation.skip(skipNum);
        LimitOperation limit = Aggregation.limit(size);
        Aggregation agg = Aggregation.newAggregation(Aggregation.match(criteria), group, sort, skip, limit);
        AggregationResults<DefectCountModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_cloc_statistic", DefectCountModel.class);
        return queryResult.getMappedResults();
    }
}
