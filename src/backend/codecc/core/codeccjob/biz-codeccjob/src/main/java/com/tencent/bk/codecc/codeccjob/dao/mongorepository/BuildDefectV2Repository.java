package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildDefectV2Repository extends MongoRepository<BuildDefectV2Entity, String> {
    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);
}
