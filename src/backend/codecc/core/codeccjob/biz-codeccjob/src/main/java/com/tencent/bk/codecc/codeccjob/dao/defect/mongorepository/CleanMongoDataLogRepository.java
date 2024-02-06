package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.CleanMongoDataLogEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CleanMongoDataLogRepository extends MongoRepository<CleanMongoDataLogEntity, String> {

}
