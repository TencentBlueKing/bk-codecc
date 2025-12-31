package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.AdminPrivilegeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPrivilegeInfoRepository  extends MongoRepository<AdminPrivilegeEntity, String> {

    /**
     * 根据唯一ID查询entity
     */
    AdminPrivilegeEntity findFirstByUserId(String userId);

    /**
     * 根据管理员type和status查询管理员 user_id 列表
     */
    @Query(value = "{ 'privilege_type': ?0, 'status': ?1 }", fields = "{ 'user_id': 1, '_id': 0 }") // 只返回 userId 字段
    List<AdminPrivilegeEntity> findAllByPrivilegeTypeAndStatus(String privilegeType, Boolean status);

    /**
     * 根据状态和过期时间查询管理员
     */
    List<AdminPrivilegeEntity> findAllByStatusAndEndTimeBefore(Boolean status, Long endTime);

    /**
     * 根据管理员status查询管理员信息
     */
    List<AdminPrivilegeEntity> findAllByStatus(Boolean status);
}
