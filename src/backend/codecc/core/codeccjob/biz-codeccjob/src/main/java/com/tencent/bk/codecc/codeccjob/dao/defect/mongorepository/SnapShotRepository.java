package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapShotRepository extends MongoRepository<SnapShotEntity, String> {

    void deleteAllByProjectIdAndBuildIdInAndTaskId(String projectId, List<String> buildIds, Long taskId);
}
