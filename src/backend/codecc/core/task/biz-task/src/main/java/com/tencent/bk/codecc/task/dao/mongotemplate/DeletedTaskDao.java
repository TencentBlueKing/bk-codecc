package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.TaskIdInfo;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.vo.TaskProjectCountVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DeletedTaskDao implements CommonTaskDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Long> getTaskIdList(Long lastTaskId, Integer limit, String filterProjectId) {
        Query query = new Query();
        query.fields().include("task_id");
        query.addCriteria(Criteria.where("task_id").gt(lastTaskId));
        // 用来过滤机器自动创建任务
        if (StringUtils.isNotEmpty(filterProjectId)) {
            query.addCriteria(Criteria.where("project_id").ne(filterProjectId));
        }
        query.with(Sort.by(Sort.Direction.ASC, "task_id"));
        query.limit(limit);
        List<TaskIdInfo> taskIdInfoList = mongoTemplate.find(
                query, TaskIdInfo.class, "t_deleted_task_detail");
        return CollectionUtils.isEmpty(taskIdInfoList) ? Collections.emptyList()
                : taskIdInfoList.stream().map(TaskIdInfo::getTaskId).sorted().collect(Collectors.toList());
    }

    @Override
    public List<TaskInfoEntity> getTaskByTaskIds(Set<Long> taskIds) {
        Query query = new Query(Criteria.where("task_id").in(taskIds));
        query.fields().include("task_id", "bg_id", "dept_id");
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_deleted_task_detail");
    }

    @Override
    public List<TaskProjectCountVO> getProjectCount(Set<Long> taskIds) {
        MatchOperation match = Aggregation
                .match(Criteria.where("task_id").in(taskIds));

        GroupOperation group = Aggregation.group("project_id")
                .first("project_id").as("projectId")
                .count().as("projectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<TaskProjectCountVO> queryResults =
                mongoTemplate.aggregate(aggregation, "t_deleted_task_detail", TaskProjectCountVO.class);
        return queryResults.getMappedResults();
    }

    @Override
    public List<TaskInfoEntity> getStopTask(Set<Long> taskIds, Long startTime, Long endTime) {
        Query query = new Query(Criteria.where("task_id").in(taskIds)
                .and("delete_date").gte(startTime).lte(endTime));
        query.fields().include("task_id", "bg_id", "dept_id");
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_deleted_task_detail");
    }
}
