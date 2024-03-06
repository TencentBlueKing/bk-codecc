package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import com.tencent.bk.codecc.defect.vo.StatisticVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class OperationHistoryDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /***
     * 获取某工具记录在指定时间段有指定操作
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @param taskIds 任务id集合
     * @param funcIds 操作id
     * @param toolName 工具名称
     * @return 返回有操作的任务id集合
     */
    public Set<Long> getOperationStatistic(Long startTime, Long endTime, List<Long> taskIds,
                                                          Set<String> funcIds, String toolName) {

        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(toolName)
                .and("time").gte(startTime).lte(endTime)
                .and("func_id").in(funcIds);

        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("taskId")
                .count().as("defectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<StatisticVO> queryResults = defectMongoTemplate.aggregate(aggregation,
                "t_operation_history", StatisticVO.class);
        return queryResults.getMappedResults().stream().map(StatisticVO::getTaskId).collect(Collectors.toSet());
    }
}
