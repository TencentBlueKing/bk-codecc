package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeSysEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统默认告警忽略类型持久层
 */
@Repository
public interface IgnoreTypeSysRepository extends MongoRepository<IgnoreTypeSysEntity, String> {

    /**
     * 按忽略类型id获取实体对象
     */
    IgnoreTypeSysEntity findFirstByIgnoreTypeId(Integer ignoreTypeId);

    /**
     * 根据实体id来查询
     */
    IgnoreTypeSysEntity findFirstByEntityId(String entityId);

    /**
     * 根据实体id来查询
     */
    IgnoreTypeSysEntity findFirstByName(String name);


    IgnoreTypeSysEntity findFirstByOrderByIgnoreTypeIdDesc();

    List<IgnoreTypeSysEntity> findByStatusInOrderByIgnoreTypeId(List<Integer> ignoreTypeIds);
}
