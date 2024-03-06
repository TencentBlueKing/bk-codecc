package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolConfigRepository extends MongoRepository<ToolConfigInfoEntity, String> {

    List<ToolConfigInfoEntity> findByTaskId(long taskId);

    List<ToolConfigInfoEntity> findByTaskIdIn(List<Long> taskIds);

    @Query(
            value = "{'task_id': {'$in': ?0}, 'tool_name': ?1}",
            fields = "{'task_id': 1, 'tool_name': 1, 'current_build_id': 1}"
    )
    List<ToolConfigInfoEntity> findByTaskIdInAndToolName(List<Long> taskIds, String toolName);


    @Query(fields = "{'task_id':1, 'current_build_id':1, 'updated_date': 1}")
    List<ToolConfigInfoEntity> findSpecialFieldByTaskId(long taskId);

    @Query(fields = "{'task_id':1, 'current_build_id':1, 'updated_date': 1}")
    List<ToolConfigInfoEntity> findSpecialFieldByTaskIdInAndFollowStatusIsNot(
            Collection<Long> taskIds,
            int followStatusIsNot
    );
}
