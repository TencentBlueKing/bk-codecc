package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.statistic.StatStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatStatisticRepository extends MongoRepository<StatStatisticEntity, String> {
    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);
}
