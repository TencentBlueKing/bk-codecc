package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具构建信息
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Repository
public interface ToolBuildInfoRepository extends MongoRepository<ToolBuildInfoEntity, String>
{
    /**
     * 根据任务ID和工具名称查询
     *
     * @param taskId
     * @param toolName
     * @return
     */
    ToolBuildInfoEntity findFirstByTaskIdAndToolName(long taskId, String toolName);

    /**
     * 按照 taskId 和 toolName 查询, 按照 updatedDate 降序排序，取 updatedDate 最晚的 ToolBuildInfoEntity
     *
     * @param taskId
     * @param toolName
     * @return 返回
     */
    ToolBuildInfoEntity findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(long taskId, String toolName);

    /**
     * 根据工具名称查询
     *
     * @param toolName
     * @return
     */
    List<ToolBuildInfoEntity> findByToolName(String toolName);
}
