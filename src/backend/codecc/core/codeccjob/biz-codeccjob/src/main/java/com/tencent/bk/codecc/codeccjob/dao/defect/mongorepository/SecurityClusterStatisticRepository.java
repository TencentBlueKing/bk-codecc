package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.SecurityClusterStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityClusterStatisticRepository extends MongoRepository<SecurityClusterStatisticEntity, String> {

    long deleteByTaskId(long taskId);
}
