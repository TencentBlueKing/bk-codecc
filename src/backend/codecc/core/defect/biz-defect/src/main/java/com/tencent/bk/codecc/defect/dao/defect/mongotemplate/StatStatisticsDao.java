package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.StatStatisticEntity;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class StatStatisticsDao {

    @Autowired MongoTemplate defectMongoTemplate;

    public List<StatStatisticEntity> findByTaskIdAndBuildIdAndToolName(long taskId, String buildId, String toolName) {
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName)
                .and("build_id").is(buildId));
        return defectMongoTemplate.find(query, StatStatisticEntity.class);
    }

    public void save(StatStatisticEntity statistic){
        defectMongoTemplate.save(statistic);
    }

    public void saveAll(List<StatStatisticEntity> statisticEntityList){
        if(CollectionUtils.isNotEmpty(statisticEntityList)){
            for (StatStatisticEntity statStatisticEntity : statisticEntityList) {
                save(statStatisticEntity);
            }
        }
    }
}
