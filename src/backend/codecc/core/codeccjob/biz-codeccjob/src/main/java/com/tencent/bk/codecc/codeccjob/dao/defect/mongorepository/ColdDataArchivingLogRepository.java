package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ColdDataArchivingLogEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColdDataArchivingLogRepository extends MongoRepository<ColdDataArchivingLogEntity, String> {

    ColdDataArchivingLogEntity findFirstByTaskIdAndType(long taskId, String type);
}
