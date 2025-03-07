package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalConfigEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统默认告警忽略类型持久层
 */
@Repository
public interface IgnoreApprovalConfigRepository extends MongoRepository<IgnoreApprovalConfigEntity, String> {
    /**
     * 根据实体id查询指定实体对象
     * @param entityId id
     * @return 实体对象
     */
    IgnoreApprovalConfigEntity findByEntityId(String entityId);
}
