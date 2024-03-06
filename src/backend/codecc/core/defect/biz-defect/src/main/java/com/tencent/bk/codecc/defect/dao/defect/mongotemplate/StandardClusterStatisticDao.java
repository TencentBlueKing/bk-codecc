package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class StandardClusterStatisticDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;

    public List<StandardClusterStatisticEntity> findListByTaskIdAndBuildId(List<MetricsEntity> metricsEntityList) {
        if (CollectionUtils.isEmpty(metricsEntityList)) {
            return Collections.emptyList();
        }

        List<Criteria> criteriaList = new ArrayList<>();
        for (int i = 0; i < metricsEntityList.size(); i++) {
            criteriaList.add(Criteria.where("task_id").is(metricsEntityList.get(i).getTaskId())
                    .and("build_id").is(metricsEntityList.get(i).getBuildId()));
        }

        Criteria criteria = new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query();
        query.addCriteria(criteria);
        return defectMongoTemplate.find(query, StandardClusterStatisticEntity.class);
    }
}
