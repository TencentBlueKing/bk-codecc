package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 忽略审批
 */
@Repository
public interface IgnoreApprovalRepository extends MongoRepository<IgnoreApprovalEntity, String> {
}
