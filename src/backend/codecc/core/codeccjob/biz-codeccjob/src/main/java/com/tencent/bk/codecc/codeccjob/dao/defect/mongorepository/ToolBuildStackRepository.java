package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolBuildStackRepository extends MongoRepository<ToolBuildStackEntity, String> {

    void deleteAllByTaskIdAndToolNameInAndBuildIdIn(long taskId, List<String> toolNames, List<String> buildIds);

    long deleteByTaskId(long taskId);
}
