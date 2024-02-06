package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskLogOverviewRepository extends MongoRepository<TaskLogOverviewEntity, String> {

    @Query(value = "{'task_id' : ?0}", fields = "{'build_id' : 1, 'build_num' : 1}")
    List<TaskLogOverviewEntity> findByTaskId(long taskId);

    void deleteAllByTaskIdAndBuildIdIn(long taskId, List<String> buildIds);

    @Query(fields = "{'start_time':1, 'task_id': 1}")
    List<TaskLogOverviewEntity> findSpecialFieldByTaskIdIn(Iterable<Long> taskIds, Pageable pageable);

    long deleteByTaskId(long taskId);
}
