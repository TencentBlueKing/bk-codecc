package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolBuildInfoRepository extends MongoRepository<ToolBuildInfoEntity, String> {

    long deleteByTaskId(long taskId);
}
