package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.checkreport.FluctuationTaskEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FluctuationTaskCheckRepository extends MongoRepository<FluctuationTaskEntity, String> {

    /**
     * 根据task_id批量删除
     * @param taskId 任务编号
     * @return 删除数量
     */
    long deleteAllByTaskId(Long taskId);
}
