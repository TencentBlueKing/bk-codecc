package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CleanMongoFailTaskEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CleanMongoDataFailTaskRepository extends MongoRepository<CleanMongoFailTaskEntity, String> {
}
