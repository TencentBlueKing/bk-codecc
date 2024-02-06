package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.CcnClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CcnClusterStatisticRepository extends MongoRepository<CcnClusterStatisticEntity, String> {

    long deleteByTaskId(long taskId);
}
