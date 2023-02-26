package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolBuildStackRepository extends MongoRepository<ToolBuildStackEntity, String> {
    void deleteAllByTaskIdAndToolNameInAndBuildIdIn(long taskId, List<String> toolNames, List<String> buildIds);
}
