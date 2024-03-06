package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.statistic.StatStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatStatisticRepository extends MongoRepository<StatStatisticEntity, String> {

    long deleteByTaskId(long taskId);

    StatStatisticEntity findFirstByTaskIdAndBuildId(long taskId, String buildId);

    long deleteByTaskIdAndBuildIdIsNot(long taskId, String buildId);
}
