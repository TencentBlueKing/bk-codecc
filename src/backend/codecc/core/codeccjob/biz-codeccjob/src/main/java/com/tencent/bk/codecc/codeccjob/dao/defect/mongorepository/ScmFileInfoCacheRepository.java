package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScmFileInfoCacheRepository extends MongoRepository<ScmFileInfoCacheEntity, String> {

    long deleteByTaskId(long taskId);
}
