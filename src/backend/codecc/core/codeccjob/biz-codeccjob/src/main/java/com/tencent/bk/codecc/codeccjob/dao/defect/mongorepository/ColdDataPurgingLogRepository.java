package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ColdDataPurgingLogEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColdDataPurgingLogRepository extends MongoRepository<ColdDataPurgingLogEntity, String> {

    ColdDataPurgingLogEntity findFirstByTaskIdAndType(long taskId, String type);
}
