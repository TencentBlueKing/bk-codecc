package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeProjectConfig;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统默认告警忽略类型持久层
 */
@Repository
public interface IgnoreTypeProjectRepository extends MongoRepository<IgnoreTypeProjectConfig, String> {

    /**
     * 按忽略类型id获取实体对象
     */
    IgnoreTypeProjectConfig findFirstByIgnoreTypeId(Integer ignoreTypeId);

    /**
     * 按项目ID与忽略类型id获取实体对象
     */
    IgnoreTypeProjectConfig findFirstByProjectIdAndIgnoreTypeId(String projectId, Integer ignoreTypeId);

    /**
     * 根据实体id来查询
     */
    IgnoreTypeProjectConfig findFirstByEntityId(String entityId);

    /**
     * 根据名称来查询
     */
    IgnoreTypeProjectConfig findFirstByProjectIdAndName(String projectId, String name);


    IgnoreTypeProjectConfig findFirstByOrderByIgnoreTypeIdDesc();

    List<IgnoreTypeProjectConfig> findByProjectIdAndStatusOrderByIgnoreTypeId(String projectId, Integer status);

    IgnoreTypeProjectConfig findFirstByProjectIdAndStatus(String projectId, Integer status);

}
