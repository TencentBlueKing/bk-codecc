package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildDefectSummaryRepository extends MongoRepository<BuildDefectSummaryEntity, String> {
    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);
}
