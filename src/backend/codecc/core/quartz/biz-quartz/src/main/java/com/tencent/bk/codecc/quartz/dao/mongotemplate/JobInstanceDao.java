package com.tencent.bk.codecc.quartz.dao.mongotemplate;

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;


@Repository
public class JobInstanceDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void updateLastAndNextTriggerTime(String jobName, Long lastTime, Long nextTime) {
        Query query = Query.query(Criteria.where("job_name").is(jobName));
        Update update = Update.update("last_trigger_time", lastTime).set("next_trigger_time", nextTime);
        mongoTemplate.updateFirst(query, update, JobInstanceEntity.class);
    }

    /**
     * 查找next_trigger_time在指定时间段内的指定ClassName的Job
     * @param classNames
     * @param startTime
     * @param endTime
     */
    public List<JobInstanceEntity> findByNextTimeInterval(List<String> classNames, String tag,
            Long startTime, Long endTime) {
        Criteria cri = Criteria.where("class_name").in(classNames).and("shard_tag").is(tag)
                .and("next_trigger_time").gte(startTime).lt(endTime);
        Query query = Query.query(cri);
        return mongoTemplate.find(query, JobInstanceEntity.class);
    }

}
