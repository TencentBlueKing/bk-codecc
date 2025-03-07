package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.BgSecurityApproverEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BgApprovalConfigRepository extends MongoRepository<BgSecurityApproverEntity, String> {
    /**
     * 根据实体id查询指定实体对象
     * @param entityId id
     * @return 实体对象
     */
    BgSecurityApproverEntity findByEntityId(String entityId);
}
