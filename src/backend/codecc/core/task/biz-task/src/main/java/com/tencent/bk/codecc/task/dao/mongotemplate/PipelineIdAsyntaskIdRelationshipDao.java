package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.mongodb.client.result.DeleteResult;
import com.tencent.bk.codecc.task.dao.mongorepository.PipelineIdAsyntaskIdRelationshipRepository;
import com.tencent.bk.codecc.task.model.PipelineIdAsyntaskIdRelationshipEntity;
import com.tencent.bk.codecc.task.pojo.PipelineIdAsyntaskIdRelationshipModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * t_pipeline_asyntask_relationship 这张表的 DAO
 *
 * @date 2023/10/26
 */
@Repository
public class PipelineIdAsyntaskIdRelationshipDao {
    @Autowired
    private PipelineIdAsyntaskIdRelationshipRepository pipelineIdAsyntaskIdRelationshipRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Boolean upsert(PipelineIdAsyntaskIdRelationshipModel model, String userName) {
        PipelineIdAsyntaskIdRelationshipEntity entity = pipelineIdAsyntaskIdRelationshipRepository
                .findFirstByAsynTaskIdAndPipelineId(model.getAsynTaskId(), model.getPipelineId());
        if (Objects.isNull(entity)) {
            entity = new PipelineIdAsyntaskIdRelationshipEntity();

            entity.setCreatedBy(userName);
            entity.setCreatedDate(System.currentTimeMillis());

            entity.setPipelineId(model.getPipelineId());
            entity.setProjectId(model.getProjectId());
            entity.setAsynTaskId(model.getAsynTaskId());

            entity.setUpdatedBy(userName);
            entity.setUpdatedDate(System.currentTimeMillis());

            pipelineIdAsyntaskIdRelationshipRepository.save(entity);
        } else {
            entity.setUpdatedBy(userName);
            entity.setUpdatedDate(System.currentTimeMillis());

            pipelineIdAsyntaskIdRelationshipRepository.save(entity);
        }

        return Boolean.TRUE;
    }

    public Boolean deleteByAsynTaskId(Long taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("asyn_task_id").is(taskId));

        mongoTemplate.remove(query, PipelineIdAsyntaskIdRelationshipEntity.class);

        return Boolean.TRUE;
    }

    public Boolean deleteByAsynTaskIdAndPipelineId(Long taskId, String pipelineId) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("asyn_task_id").is(taskId)
                        .and("pipeline_id").is(pipelineId)
        );

        DeleteResult result = mongoTemplate.remove(query, PipelineIdAsyntaskIdRelationshipEntity.class);

        return result.getDeletedCount() > 0;
    }

    public Set<String> getPipelineIdSetByAsynTaskId(Long taskId) {
        Set<String> result = new HashSet<>();
        List<PipelineIdAsyntaskIdRelationshipEntity> entities = pipelineIdAsyntaskIdRelationshipRepository
                .findByAsynTaskId(taskId);
        for (PipelineIdAsyntaskIdRelationshipEntity entity : entities) {
            result.add(entity.getPipelineId());
        }

        return result;
    }

}
