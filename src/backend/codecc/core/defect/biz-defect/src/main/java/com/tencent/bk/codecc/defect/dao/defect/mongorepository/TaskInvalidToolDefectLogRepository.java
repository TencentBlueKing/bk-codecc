package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskInvalidToolDefectLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInvalidToolDefectLogRepository extends MongoRepository<TaskInvalidToolDefectLog, String> {

    TaskInvalidToolDefectLog findFirstByTaskIdAndToolNameAndBuildId(Long taskId, String toolName, String buildId);

    TaskInvalidToolDefectLog findFirstByTaskIdAndToolNameOrderByCreatedDateDesc(Long taskId, String toolName);

}
