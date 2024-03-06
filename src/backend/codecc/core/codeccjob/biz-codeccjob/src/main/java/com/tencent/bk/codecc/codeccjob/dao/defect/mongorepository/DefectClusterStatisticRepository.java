package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.DefectClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefectClusterStatisticRepository extends MongoRepository<DefectClusterStatisticEntity, String> {

    long deleteByTaskId(long taskId);
}
