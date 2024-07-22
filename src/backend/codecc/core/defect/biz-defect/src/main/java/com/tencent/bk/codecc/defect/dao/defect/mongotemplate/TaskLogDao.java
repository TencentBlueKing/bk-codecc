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

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogGroupEntity;
import com.tencent.bk.codecc.defect.vo.BatchTaskLogQueryVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分析记录复杂查询持久代码
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Repository
public class TaskLogDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param taskId
     * @param toolSet
     * @return
     */
    public List<TaskLogGroupEntity> findFirstByTaskIdOrderByStartTime(long taskId, Set<String> toolSet) {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId).and("tool_name").in(toolSet));
        //根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        //限制数量
        LimitOperation limit = Aggregation.limit(ComConstants.SMALL_PAGE_SIZE);
        //限制字段
        ProjectionOperation projection = Aggregation.project(
                "task_id", "tool_name", "elapse_time", "end_time", "start_time", "build_num", "build_id"
        );
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("tool_name")
                .first("task_id").as("taskId")
                .first("tool_name").as("toolName")
                .first("elapse_time").as("elapseTime")
                .first("end_time").as("endTime")
                .first("start_time").as("startTime")
                .first("build_num").as("buildNum")
                .first("build_id").as("buildId");
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, limit, projection, group).withOptions(options);
        AggregationResults<TaskLogGroupEntity> queryResult = defectMongoTemplate.aggregate(agg, "t_task_log",
                TaskLogGroupEntity.class);
        return queryResult.getMappedResults();
    }


    /**
     * 批量查询多个任务： 根据工具ID和工具列表查询最近一次分析记录
     *
     * @param queryVOs
     * @return
     */
    public List<TaskLogGroupEntity> findFirstByTaskIdAndToolNameInOrderByStartTime(List<BatchTaskLogQueryVO> queryVOs) {
        List<Criteria> orCri = queryVOs.stream().map(it -> Criteria.where("task_id").is(it.getTaskId())
                        .and("tool_name").in(it.getToolSet())
                        .and("build_id").is(it.getBuildId()))
                .collect(Collectors.toList());
        Criteria cri = new Criteria().orOperator(orCri);
        Query query = Query.query(cri);
        query.fields().include("task_id", "tool_name", "elapse_time", "end_time",
                "start_time", "build_num", "build_id");
        List<TaskLogEntity> taskLogEntities = defectMongoTemplate.find(query, TaskLogEntity.class);
        if (CollectionUtils.isEmpty(taskLogEntities)) {
            return Collections.emptyList();
        }
        return taskLogEntities.stream().map(it -> {
            TaskLogGroupEntity taskLogGroupEntity = new TaskLogGroupEntity();
            taskLogGroupEntity.setTaskId(it.getTaskId());
            taskLogGroupEntity.setToolName(it.getToolName());
            taskLogGroupEntity.setElapseTime(it.getElapseTime());
            taskLogGroupEntity.setEndTime(it.getEndTime());
            taskLogGroupEntity.setStartTime(it.getStartTime());
            taskLogGroupEntity.setBuildId(it.getBuildId());
            taskLogGroupEntity.setBuildNum(it.getBuildNum());
            return taskLogGroupEntity;
        }).collect(Collectors.toList());
    }

    /**
     * 查询任务指定BuildId的工具记录
     * @param taskId
     * @param buildId
     * @return
     */
    public List<TaskLogEntity> findFirstByTaskIdAndBuildIdOrderbyStartTime(long taskId, String buildId) {
        //添加查询条件
        MatchOperation matchOperation = Aggregation.match(
                Criteria.where("task_id").is(taskId).and("build_id").is(buildId));
        //根据时间排序
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.DESC, "start_time");
        //以toolName进行分组
        GroupOperation groupOperation = Aggregation.group("tool_name")
                .first("stream_name").as("stream_name")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time")
                .first("end_time").as("end_time")
                .first("elapse_time").as("elapse_time")
                .first("pipeline_id").as("pipeline_id")
                .first("build_id").as("build_id")
                .first("build_num").as("build_num")
                .first("trigger_from").as("trigger_from")
                .first("version_time").as("version_time")
                .first("step_array").as("step_array");
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, sortOperation, groupOperation)
                .withOptions(options);

        AggregationResults<TaskLogEntity> queryResult =
                defectMongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 批量获取任务的工具最新分析记录
     *
     * @param taskIds 任务ID集合
     * @param toolName 工具名
     * @return list
     */
    public List<TaskLogEntity> findLastTaskLogByTool(Set<Long> taskIds, String toolName) {
        // 添加查询条件
        MatchOperation match = Aggregation.match(Criteria.where("tool_name").is(toolName).and("task_id").in(taskIds));
        // 根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        // 以toolName进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("curr_step").as("curr_step")
                .first("flag").as("flag")
                .first("start_time").as("start_time")
                .first("end_time").as("end_time")
                .first("build_num").as("build_num");
        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group).withOptions(options);

        AggregationResults<TaskLogEntity> queryResult =
                defectMongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }


    /**
     * 获取时间区间内的所有任务的所有分析记录
     *
     * @param startTime 开始分析时间
     * @param endTime 分析结束时间
     * @return list
     */
    public List<TaskLogEntity> findTaskLogByTime(Set<Long> taskIdSet, Long startTime, Long endTime) {
        // 添加查询条件（在时间区间范围内）
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIdSet)
                .and("start_time").gt(startTime).and("end_time").lt(endTime));

        // 根据任务ID排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "task_id");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(match, sort).withOptions(options);

        AggregationResults<TaskLogEntity> queryResult =
                defectMongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 获取最后十次构建的分析记录
     *
     * @param taskId
     * @param limitSize
     * @return list
     */
    public List<TaskLogEntity> findLatestBuild(Long taskId, int limitSize) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId));
        // 以taskId进行分组
        GroupOperation group = Aggregation.group("build_id")
                .first("build_id").as("build_id");
        LimitOperation limit = new LimitOperation(limitSize);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");
        Aggregation agg = Aggregation.newAggregation(match, sort, group, limit);

        AggregationResults<TaskLogEntity> queryResult =
                defectMongoTemplate.aggregate(agg, "t_task_log", TaskLogEntity.class);
        List<String> buildIds = queryResult.getMappedResults()
                .stream()
                .map(TaskLogEntity::getBuildId)
                .collect(Collectors.toList());
        return defectMongoTemplate.find(new Query(Criteria.where("build_id").in(buildIds)), TaskLogEntity.class);
    }

    /**
     * 查询分析成功的任务和工具信息
     *
     * @param taskIds 任务id集合
     * @param toolName 工具名
     * @return
     */
    public List<TaskLogEntity> queryAccessedTask(Collection<Long> taskIds, String toolName) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds).and("tool_name").is(toolName));
        }

        // KLOCWORK、COVERITY、PINPOINT 以curr_step=5判定执行成功
        if (toolName.equals(ComConstants.ToolPattern.KLOCWORK.name())
                || toolName.equals(ComConstants.ToolPattern.COVERITY.name())
                || toolName.equals(ComConstants.ToolPattern.PINPOINT.name())) {
            criteriaList.add(Criteria.where("curr_step").is(ComConstants.Step4Cov.DEFECT_SYNS.value())
                    .and("flag").is(ComConstants.StepFlag.SUCC.value())
            );
        } else {
            criteriaList.add(Criteria.where("curr_step").is(ComConstants.Step4MutliTool.COMMIT.value())
                    .and("flag").is(ComConstants.StepFlag.SUCC.value())
            );
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        MatchOperation match = Aggregation.match(criteria);

        // 以任务ID和构件号进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name")
                .first("task_id").as("task_id")
                .first("tool_name").as("tool_name");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);

        AggregationResults<TaskLogEntity> queryResult = defectMongoTemplate.aggregate(agg, "t_task_log", TaskLogEntity.class);
        return queryResult.getMappedResults();
    }


    /**
     * 通过任务id、工具名、构建id查询
     *
     * @param taskIdList 任务id集合
     * @param toolName 工具名
     * @param buildIdList 构建id集合
     * @return list
     */
    public List<TaskLogEntity> findByTaskIdInAndToolNameAndBuildIdIn(Collection<Long> taskIdList, String toolName,
            List<String> buildIdList) {

        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("start_time", true);
        fieldsObj.put("end_time", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        if (CollectionUtils.isNotEmpty(taskIdList)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdList));
        }

        if (StringUtils.isNotEmpty(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        // 构建ID
        if (CollectionUtils.isNotEmpty(buildIdList)) {
            query.addCriteria(Criteria.where("build_id").in(buildIdList));
        }

        return defectMongoTemplate.find(query, TaskLogEntity.class, "t_task_log");
    }

    /**
     * 根据任务ID和工具名称查询最新一次成功的构建记录
     *
     * @param taskId
     * @param toolName
     * @return
     */
    public TaskLogEntity findLastTaskLogByTaskIdAndToolName(Long taskId, String toolName) {
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName).and("flag").is(ComConstants.StepFlag.SUCC.value()));
        query.with(Sort.by(new Sort.Order(Sort.Direction.DESC, "start_time"))).limit(1);
        return defectMongoTemplate.findOne(query, TaskLogEntity.class);
    }

    public List<TaskLogEntity> getTaskLogInfoByBuildBumAndTaskId(String buildNum, List<Long> taskId) {
        if (taskId.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = Query.query(Criteria.where("task_id").in(taskId).and("build_num").is(buildNum));
        return defectMongoTemplate.find(query, TaskLogEntity.class, "t_task_log");
    }

    /**
     * 根据任务ID和工具名称查询最新N次成功的构建记录
     *
     * @param taskId 任务ID
     * @param toolName 工具名
     * @param range 查询N次, 限制最多十次
     * @return taskLog列表
     */
    public List<TaskLogEntity> findTaskLogByTaskIdAndToolName(Long taskId, String toolName, int range) {
        int minRange = Math.min(range, 10);
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName).and("flag").is(ComConstants.StepFlag.SUCC.value()));
        query.with(Sort.by(new Sort.Order(Sort.Direction.DESC, "start_time"))).limit(minRange);
        return defectMongoTemplate.find(query, TaskLogEntity.class);
    }

    /**
     * 根据指定任务id和flag 批量聚合查询
     */
    public List<TaskLogEntity> findBatchByTaskIdInAndFlag(List<Long> taskIds, int flag) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds).and("flag").is(flag));

        // 以任务ID和构件号进行分组
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id")
                .first("start_time").as("start_time")
                .first("step_array").as("step_array");

        //根据开始时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "start_time");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group).withOptions(options);
        return defectMongoTemplate.aggregate(aggregation, "t_task_log", TaskLogEntity.class).getMappedResults();
    }
}
