package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonStatisticRepository extends MongoRepository<CommonStatisticEntity, String> {

    void deleteAllByTaskIdAndToolNameInAndBuildIdIn(long taskId, List<String> toolNames, List<String> buildIds);

    long deleteByTaskId(long taskId);

    List<CommonStatisticEntity> findByTaskIdAndBuildId(long taskId, String buildId);

    long deleteByTaskIdAndBuildIdIsNot(long taskId, String buildId);
}
