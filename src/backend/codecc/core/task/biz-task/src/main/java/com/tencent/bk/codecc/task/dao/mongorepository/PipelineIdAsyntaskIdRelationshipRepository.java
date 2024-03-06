package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.NewTaskRetryRecordEntity;
import com.tencent.bk.codecc.task.model.PipelineIdAsyntaskIdRelationshipEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineIdAsyntaskIdRelationshipRepository
        extends MongoRepository<PipelineIdAsyntaskIdRelationshipEntity, String> {
    List<PipelineIdAsyntaskIdRelationshipEntity> findByAsynTaskId(Long asynTaskId);

    PipelineIdAsyntaskIdRelationshipEntity findFirstByAsynTaskId(Long asynTaskId);

    PipelineIdAsyntaskIdRelationshipEntity findFirstByAsynTaskIdAndPipelineId(Long asynTaskId, String pipelineId);
}
