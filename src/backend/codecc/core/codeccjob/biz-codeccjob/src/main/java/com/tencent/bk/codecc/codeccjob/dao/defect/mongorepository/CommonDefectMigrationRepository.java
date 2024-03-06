package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.CommonDefectMigrationEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonDefectMigrationRepository extends MongoRepository<CommonDefectMigrationEntity, String> {

    @Query(fields = "{'task_id':1}")
    List<CommonDefectMigrationEntity> findByTaskIdIn(Collection<Long> taskIds);

    List<CommonDefectMigrationEntity> findByTaskId(Long taskId);
}
