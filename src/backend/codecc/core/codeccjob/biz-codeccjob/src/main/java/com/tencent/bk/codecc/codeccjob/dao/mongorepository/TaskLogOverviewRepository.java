package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskLogOverviewRepository extends MongoRepository<TaskLogOverviewEntity, String> {

    @Query(value = "{'task_id' : ?0}", fields = "{'build_id' : 1, 'build_num' : 1}")
    List<TaskLogOverviewEntity> findByTaskId(long taskId);

    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);
}
