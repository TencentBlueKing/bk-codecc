package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CommonDefectMigrationEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonDefectMigrationRepository extends MongoRepository<CommonDefectMigrationEntity, String> {

    List<CommonDefectMigrationEntity> findByTaskId(Long taskId);
}
