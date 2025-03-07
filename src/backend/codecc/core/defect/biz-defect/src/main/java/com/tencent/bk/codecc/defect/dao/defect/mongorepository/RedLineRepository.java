package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 质量红线数据表
 *
 * @version V1.0
 * @date 2019/12/19
 */
@Repository
public interface RedLineRepository extends MongoRepository<RedLineEntity, String>
{
    /**
     * 按构建号查询
     *
     * @param buildId
     * @return
     */
    List<RedLineEntity> findByBuildId(String buildId);

    /**
     * 根据构建ID和任务ID查询红线数据
     * @param buildId 构建ID
     * @param taskId 任务ID
     * @return 红线entity
     */
    List<RedLineEntity> findByBuildIdAndTaskId(String buildId, Long taskId);
}
