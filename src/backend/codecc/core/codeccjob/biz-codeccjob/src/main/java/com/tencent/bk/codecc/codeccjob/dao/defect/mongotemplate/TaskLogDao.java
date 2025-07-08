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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.devops.common.constant.ComConstants;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

/**
 * 分析记录复杂查询持久代码
 *
 * @version V1.0
 * @date 2021/8/6
 */
@Repository
public class TaskLogDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 通过任务id、工具名、构建id查询
     *
     * @param taskIdList 任务id集合
     * @param toolName 工具名
     * @return list
     */
    public List<TaskLogEntity> findElapseTimeByTaskIdInAndToolName(List<Long> taskIdList, String toolName) {
        List<Criteria> criteriaList = Lists.newArrayList();

        // 指定任务ID集合
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            criteriaList.add(Criteria.where("task_id").in(taskIdList));
        }

        // 工具名
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }

        // 状态
        criteriaList.add(Criteria.where("flag").is(ComConstants.StepFlag.SUCC.value()));

        // 全量扫描
        criteriaList.add(Criteria.where("step_array").elemMatch(Criteria.where("msg").is("全量扫描")));

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);
        // 根据开始时间倒序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");

        // 以任务ID进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("elapse_time").as("elapse_time");

        // 允许磁盘操作(支持较大数据集合的处理)
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);
        AggregationResults<TaskLogEntity> queryResult =
                defectMongoTemplate.aggregate(agg, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }
}