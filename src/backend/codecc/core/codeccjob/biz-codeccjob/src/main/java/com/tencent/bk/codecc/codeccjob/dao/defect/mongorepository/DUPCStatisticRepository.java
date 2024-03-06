package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DUPCStatisticRepository extends MongoRepository<DUPCStatisticEntity, String> {

    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);

    long deleteByTaskId(long taskId);

    DUPCStatisticEntity findFirstByTaskIdAndBuildId(long taskId, String buildId);

    long deleteByTaskIdAndBuildIdIsNot(long taskId, String buildId);
}
