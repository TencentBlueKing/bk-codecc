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

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 分析记录复杂查询持久代码
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Repository
public class CommonStatisticDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<CommonStatisticEntity> findFirstByTaskIdOrderByStartTime(long taskId, Set<String> toolSet) {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId).and("tool_name").in(toolSet));
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
                .first("new_count").as("new_count")
                .first("exist_count").as("exist_count")
                .first("fixed_count").as("fixed_count")
                .first("exclude_count").as("exclude_count")
                .first("close_count").as("close_count")
                .first("exist_prompt_count").as("exist_prompt_count")
                .first("exist_normal_count").as("exist_normal_count")
                .first("exist_serious_count").as("exist_serious_count")
                .first("new_prompt_count").as("new_prompt_count")
                .first("new_normal_count").as("new_normal_count")
                .first("new_serious_count").as("new_serious_count")
                .first("new_authors").as("new_authors")
                .first("exist_authors").as("exist_authors");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);

        AggregationResults<CommonStatisticEntity> queryResult = defectMongoTemplate.aggregate(agg, "t_statistic",
                CommonStatisticEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 获取各工具相同构建Id的最后一次统计
     *
     * @param taskId
     * @param toolNames
     * @param buildId
     * @return
     */
    public List<CommonStatisticEntity> getLatestStatisticForCluster(
            Long taskId,
            List<String> toolNames,
            String buildId
    ) {
        MatchOperation match = Aggregation.match(
                Criteria.where("task_id").is(taskId)
                        .and("tool_name").in(toolNames)
                        .and("build_id").is(buildId)
        );
        SortOperation sort = Aggregation.sort(Sort.by(Direction.DESC, "time"));
        GroupOperation group = Aggregation.group("tool_name")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id")
                .first("tool_name").as("tool_name")
                .first("dimension_statistic").as("dimension_statistic")
                .first("time").as("time");

        Aggregation aggregation = Aggregation.newAggregation(match, sort, group);
        AggregationResults<CommonStatisticEntity> queryResult = defectMongoTemplate.aggregate(aggregation,
                "t_statistic", CommonStatisticEntity.class);

        return queryResult.getMappedResults();
    }

    /**
     * 根据工具名称，任务编号范围，时间范围获取CommonStatistic分析报告
     * @param toolName 工具名称
     * @param taskIds 任务编号范围
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 分析报告列表
     */
    public List<CommonStatisticEntity> getScanStatisticList(
            String toolName,
            List<Long> taskIds,
            Long startTime,
            Long endTime
    ) {
        List<CommonStatisticEntity> results = new ArrayList<>();
        Query query = Query.query(Criteria.where("tool_name").is(toolName));
        query.addCriteria(Criteria.where("task_id").in(taskIds));
        query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime));
        query.fields().include(
                "task_id", "exist_count", "new_count", "fixed_count"
        );

        long page = 0;
        query.limit(ComConstants.COMMON_PAGE_SIZE);
        while (true) {
            query.skip(page * ComConstants.COMMON_PAGE_SIZE);
            List<CommonStatisticEntity> batchResult =
                    defectMongoTemplate.find(query, CommonStatisticEntity.class);
            if (batchResult.isEmpty()) {
                break;
            }
            results.addAll(batchResult);
            page++;
        }
        return results;
    }
}
