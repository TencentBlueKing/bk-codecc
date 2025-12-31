package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.checkreport.TaskCheckEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCheckRepository extends MongoRepository<TaskCheckEntity, String> {

    /**
     * 根据build_id列表批量删除
     * @param buildIds 构件号列表
     * @return 删除数量
     */
    long deleteAllByBuildIdIn(List<String> buildIds);
}
