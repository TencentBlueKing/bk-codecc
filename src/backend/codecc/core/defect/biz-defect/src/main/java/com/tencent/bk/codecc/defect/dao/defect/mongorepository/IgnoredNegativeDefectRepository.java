package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.IgnoredNegativeDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IgnoredNegativeDefectRepository extends MongoRepository<IgnoredNegativeDefectEntity, String> {
}
