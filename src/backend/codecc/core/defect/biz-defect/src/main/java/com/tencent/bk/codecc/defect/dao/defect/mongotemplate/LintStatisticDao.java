package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import java.util.List;
import java.util.Set;

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
import org.springframework.stereotype.Repository;

@Repository
public class LintStatisticDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

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
