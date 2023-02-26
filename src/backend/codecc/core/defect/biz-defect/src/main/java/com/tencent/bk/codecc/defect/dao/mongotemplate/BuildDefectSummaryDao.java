package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class BuildDefectSummaryDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void addToolToSummary(long taskId, String buildId, String toolName) {
        Query query = new Query(Criteria.where("task_id").is(taskId).and("build_id").is(buildId));
        Update update = new Update();
        update.addToSet("tool_list", toolName);
        mongoTemplate.updateFirst(query, update, BuildDefectSummaryEntity.class);
    }

    public List<BuildDefectSummaryEntity> findByTaskIdAndBranch(long taskId, String branch) {
        Query query = new Query(Criteria.where("task_id").is(taskId).and("repo_info.branch").is(branch));
        return mongoTemplate.find(query, BuildDefectSummaryEntity.class);
    }


    public BuildDefectSummaryEntity findLastByTaskIdAndBranch(long taskId, String branch) {
        Query query = new Query(Criteria.where("task_id").is(taskId).and("repo_info.branch").is(branch));
        Sort sort = Sort.by(new Order(Direction.DESC, "build_time"));
        query.with(sort).limit(1);

        return mongoTemplate.findOne(query, BuildDefectSummaryEntity.class);
    }

    public BuildDefectSummaryEntity findLastByTaskId(long taskId) {
        Query query = new Query(Criteria.where("task_id").is(taskId));
        Sort sort = Sort.by(new Order(Direction.DESC, "build_time"));
        query.with(sort).limit(1);

        return mongoTemplate.findOne(query, BuildDefectSummaryEntity.class);
    }
}
