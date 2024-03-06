package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskPersonalStatisticRepository extends MongoRepository<TaskPersonalStatisticEntity, String> {

    long deleteByTaskId(long taskId);
}
