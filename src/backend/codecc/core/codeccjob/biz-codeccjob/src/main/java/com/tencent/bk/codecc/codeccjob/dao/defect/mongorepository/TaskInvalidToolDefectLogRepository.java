package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.TaskInvalidToolDefectLog;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInvalidToolDefectLogRepository extends MongoRepository<TaskInvalidToolDefectLog, String> {

    void deleteAllByTaskIdAndBuildIdIn(Long taskId, List<String> buildIds);

}
