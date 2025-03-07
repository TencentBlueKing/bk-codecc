package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ScanCodeSummaryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScanCodeSummaryRepository extends MongoRepository<ScanCodeSummaryEntity, String> {

    ScanCodeSummaryEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);

}
