package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface MetricsRepository extends MongoRepository<MetricsEntity, String> {
    MetricsEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);

    void deleteAllByTaskIdAndBuildIdIn(long taskIds, List<String> buildIds);

    @Query(fields = "{'rd_indicators_score': 1}")
    List<MetricsEntity> findByTaskIdInAndBuildIdIn(List<Long> taskIds, Collection<String> buildIds);
}

