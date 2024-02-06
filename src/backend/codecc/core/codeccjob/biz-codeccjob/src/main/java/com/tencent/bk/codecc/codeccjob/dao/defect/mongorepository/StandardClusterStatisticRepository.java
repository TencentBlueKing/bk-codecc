package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.StandardClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StandardClusterStatisticRepository extends MongoRepository<StandardClusterStatisticEntity, String> {

    long deleteByTaskId(long taskId);
}
