package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ScanCodeSummaryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanCodeSummaryRepository extends MongoRepository<ScanCodeSummaryEntity, String> {

    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);
}
