package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MetricsDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    public boolean upsert(MetricsEntity metricsEntity) {
        Query query = new Query(
                Criteria.where("task_id").is(metricsEntity.getTaskId())
                        .and("build_id").is(metricsEntity.getBuildId())
        );

        Update update = new Update();
        update.set("task_id", metricsEntity.getTaskId())
                .set("build_id", metricsEntity.getBuildId())
                .set("code_style_score", metricsEntity.getCodeStyleScore())
                .set("code_security_score", metricsEntity.getCodeSecurityScore())
                .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore());
        defectMongoTemplate.upsert(query, update, MetricsEntity.class);
        return true;
    }

    /**
     * 批量更新度量数据
     *
     * @param metricsEntities
     */
    public boolean batchUpsert(Collection<MetricsEntity> metricsEntities) {
        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MetricsEntity.class);
        if (CollectionUtils.isNotEmpty(metricsEntities)) {
            for (MetricsEntity metricsEntity : metricsEntities) {
                Query query = new Query(
                        Criteria.where("task_id").is(metricsEntity.getTaskId())
                                .and("build_id").is(metricsEntity.getBuildId())
                );

                Update update = new Update();
                update.set("task_id", metricsEntity.getTaskId())
                        .set("build_id", metricsEntity.getBuildId())
                        .set("code_style_score", metricsEntity.getCodeStyleScore())
                        .set("code_security_score", metricsEntity.getCodeSecurityScore())
                        .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                        .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore());

                ops.upsert(query, update);
            }
            ops.execute();
        }
        return Boolean.TRUE;
    }

    /**
     * 查询指定任务 构建id 的分数
     */
    public List<MetricsEntity> findByTaskIdAndBuildId(Map<Long, String> taskLatestBuildIdMap) {
        List<Criteria> orCriteriaList = new ArrayList<>();
        // 为空则不查询
        if (taskLatestBuildIdMap == null || taskLatestBuildIdMap.isEmpty()) {
            return Collections.emptyList();
        }
        // or语句拼接条件
        for (Map.Entry<Long, String> entry : taskLatestBuildIdMap.entrySet()) {
            orCriteriaList.add(Criteria.where("task_id").is(entry.getKey()).and("build_id").is(entry.getValue()));
        }
        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);

        // 解决同一次构建可能存在2条记录
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("rd_indicators_score").as("rd_indicators_score");

        Aggregation agg = Aggregation.newAggregation(match, group);

        AggregationResults<MetricsEntity> results =
                defectMongoTemplate.aggregate(agg, "t_metrics", MetricsEntity.class);
        return results.getMappedResults();
    }
}

