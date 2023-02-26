package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolConfigRepository extends MongoRepository<ToolConfigInfoEntity, String> {
    List<ToolConfigInfoEntity> findByTaskId(long taskId);

    List<ToolConfigInfoEntity> findByTaskIdIn(List<Long> taskIds);
}
