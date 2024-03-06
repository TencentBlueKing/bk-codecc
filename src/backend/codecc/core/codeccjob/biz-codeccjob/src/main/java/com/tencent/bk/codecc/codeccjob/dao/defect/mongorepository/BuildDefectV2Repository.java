package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildDefectV2Repository extends MongoRepository<BuildDefectV2Entity, String> {

    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);

    long deleteByTaskId(long taskId);
}
