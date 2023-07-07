package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapShotRepository extends MongoRepository<SnapShotEntity, String> {
    void deleteAllByProjectIdAndBuildIdInAndTaskId(String projectId, List<String> buildIds, Long taskId);
}
