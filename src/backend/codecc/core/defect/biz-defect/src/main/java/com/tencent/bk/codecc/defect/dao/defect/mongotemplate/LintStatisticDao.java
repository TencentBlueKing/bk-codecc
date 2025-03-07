package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.vo.GrayDefectStaticVO;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class LintStatisticDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    public Map<String, Integer> batchQueryDefectCount(Long taskId, String buildId, List<String> toolNameList) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").is(taskId);
        criteria.and("tool_name").in(toolNameList);
        criteria.and("build_id").is(buildId);

        Query query = new Query(criteria);
        query.fields().include("defect_count", "tool_name");
        List<LintStatisticEntity> lintStatList = defectMongoTemplate.find(query, LintStatisticEntity.class);

        return lintStatList.stream().collect(Collectors.toMap(
                LintStatisticEntity::getToolName,
                LintStatisticEntity::getDefectCount
        ));
    }

    public Integer queryDefectCount(Long taskId, String toolName, String buildId) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").is(taskId);
        criteria.and("tool_name").is(toolName);
        criteria.and("build_id").is(buildId);

        Query query = new Query(criteria);
        query.fields().include("defect_count");

        LintStatisticEntity result = defectMongoTemplate.findOne(query, LintStatisticEntity.class);
        if (result == null || result.getDefectCount() == null) {
            return 0;
        }

        return result.getDefectCount();
    }

    /**
     * 获取各工具相同构建Id的最后一次统计
     *
     * @param taskId
     * @param toolNames
     * @param buildId
     * @return
     */
    public List<LintStatisticEntity> getLatestStatisticForCluster(Long taskId, List<String> toolNames, String buildId) {
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
        AggregationResults<LintStatisticEntity> queryResult = defectMongoTemplate.aggregate(aggregation,
                "t_lint_statistic", LintStatisticEntity.class);

        return queryResult.getMappedResults();
    }

    public List<GrayDefectStaticVO> getByGrayDefectCountData(List<Long> taskIds, Set<String> buildIds) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds)
                .and("build_id").in(buildIds.toArray()));
        GroupOperation as = Aggregation.group("task_id")
                .first("task_id").as("taskId").first("defect_count").as("defectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, as);
        return defectMongoTemplate.aggregate(aggregation,
                "t_lint_statistic", GrayDefectStaticVO.class).getMappedResults();
    }
}
