package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class MetricsDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public MetricsEntity findLastByTaskId(Long taskId) {
        Criteria criteria = Criteria.where("task_id").is(taskId);
        Query query = Query.query(criteria);
        Sort sort = Sort.by(Direction.DESC, "_id");
        query.with(sort);
        query.limit(1);
        List<MetricsEntity> metricsEntityList = mongoTemplate.find(query, MetricsEntity.class);
        if (metricsEntityList.isEmpty()) {
            return null;
        }
        return metricsEntityList.get(0);
    }

    public List<MetricsEntity> findLastByTaskIdIn(List<Long> taskIds) {
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds));
        SortOperation sort = Aggregation.sort(Sort.by(Direction.DESC, "_id"));
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("rd_indicators_score").as("rd_indicators_score")
                .first("build_id").as("build_id")
                .first("is_open_scan").as("is_open_scan");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        AggregationResults<MetricsEntity> queryResult = mongoTemplate.aggregate(agg, "t_metrics", MetricsEntity.class);
        List<MetricsEntity> retList = queryResult.getMappedResults();

        return retList;
    }

    /**
     * 根据TaskId与BuildId键值对查询，Or查询多条
     * @param taskIdAndBuildIdMap
     * @return
     */
    public List<MetricsEntity> findByTaskAndBuildIdMap(Map<Long, String> taskIdAndBuildIdMap) {
        if (taskIdAndBuildIdMap == null || taskIdAndBuildIdMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<Criteria> orCriList = new ArrayList<>();
        for (Entry<Long, String> taskIdAndBuildId : taskIdAndBuildIdMap.entrySet()) {
            orCriList.add(Criteria.where("task_id").is(taskIdAndBuildId.getKey())
                    .and("build_id").is(taskIdAndBuildId.getValue()));
        }
        Criteria criteria = new Criteria();
        criteria.orOperator(orCriList.toArray(new Criteria[]{}));
        return mongoTemplate.find(Query.query(criteria), MetricsEntity.class);
    }

    /**
     * upsert by task id and build id
     *
     * @param metricsEntity
     * @return
     */
    public boolean upsert(MetricsEntity metricsEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(metricsEntity.getTaskId())
                .and("build_id").is(metricsEntity.getBuildId()));

        Update update = new Update();
        update.set("task_id", metricsEntity.getTaskId())
                .set("build_id", metricsEntity.getBuildId())
                .set("code_style_score", metricsEntity.getCodeStyleScore())
                .set("code_security_score", metricsEntity.getCodeSecurityScore())
                .set("code_measure_score", metricsEntity.getCodeMeasureScore())
                .set("rd_indicators_score", metricsEntity.getRdIndicatorsScore())
                .set("is_open_scan", metricsEntity.isOpenScan())
                .set("code_ccn_score", metricsEntity.getCodeCcnScore())
                .set("code_defect_score", metricsEntity.getCodeDefectScore())
                .set("average_thousand_defect", metricsEntity.getAverageThousandDefect())
                .set("code_style_normal_defect_count", metricsEntity.getCodeStyleNormalDefectCount())
                .set("code_style_serious_defect_count", metricsEntity.getCodeStyleSeriousDefectCount())
                .set("code_defect_normal_defect_count", metricsEntity.getCodeDefectNormalDefectCount())
                .set("code_defect_serious_defect_count", metricsEntity.getCodeDefectSeriousDefectCount())
                .set("code_security_normal_defect_count", metricsEntity.getCodeSecurityNormalDefectCount())
                .set("code_security_serious_defect_count", metricsEntity.getCodeSecuritySeriousDefectCount());
        mongoTemplate.upsert(query, update, MetricsEntity.class);
        return true;
    }
}
