package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.StatStatisticEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class StatStatisticDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    public void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("build_id").in(buildIds));
        defectMongoTemplate.remove(query, StatStatisticEntity.class);
    }
}
