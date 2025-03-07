package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.CustomCheckerProjectRelationshipEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 自定义规则与其他对象关联持久化
 *
 * @version V1.0
 * @date 2024/8/19
 */
public interface CustomCheckerProjectRelationshipRepository
    extends MongoRepository<CustomCheckerProjectRelationshipEntity, String> {
    /**
     * 根据规则名查询
     *
     * @param checkerName
     * @return
     */
    List<CustomCheckerProjectRelationshipEntity> findByCheckerName(String checkerName);

    /**
     * 通过项目id查询
     *
     * @param projectId
     * @return
     */
    List<CustomCheckerProjectRelationshipEntity> findByProjectId(String projectId);

    /**
     * 通过工具名查询
     *
     * @param toolName
     * @return
     */
    List<CustomCheckerProjectRelationshipEntity> findByToolName(String toolName);

    /**
     * 通过工具名和规则名查询
     *
     * @param toolName
     * @return
     */
    List<CustomCheckerProjectRelationshipEntity> findByToolNameAndCheckerName(String toolName, String checkerName);

}
