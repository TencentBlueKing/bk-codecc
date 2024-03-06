package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.issue.DefectIssueInfoEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefectIssueInfoRepository extends MongoRepository<DefectIssueInfoEntity, String> {

    /**
     * 根据任务id和告警的主键id查找告警的提单信息
     *
     * @param taskId
     * @param defectEntityId
     * @return
     */
    List<DefectIssueInfoEntity> findByTaskIdAndDefectEntityIdIn(long taskId, Collection<String> defectEntityId);

    /**
     * 根据任务id、工具名称、状态查找告警的提单信息
     *
     * @param taskId
     * @param toolNames
     * @param status
     * @return
     */
    List<DefectIssueInfoEntity> findByTaskIdAndToolNameInAndStatus(
            long taskId, Collection<String> toolNames, int status);

    /**
     * 根据任务id、工具名称、状态获取告警的提单信息总数
     *
     * @param taskId
     * @param toolNames
     * @param status
     * @param severities
     * @return
     */
    Integer countByTaskIdAndToolNameInAndStatusAndSeverityIn(
            long taskId, Collection<String> toolNames, int status, Collection<Integer> severities);
}
