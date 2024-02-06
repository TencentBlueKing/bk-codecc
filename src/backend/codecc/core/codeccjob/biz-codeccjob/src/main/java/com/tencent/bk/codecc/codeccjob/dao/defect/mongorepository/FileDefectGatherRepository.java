package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.FileDefectGatherEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileDefectGatherRepository extends MongoRepository<FileDefectGatherEntity, String> {

    long deleteByTaskId(long taskId);
}
