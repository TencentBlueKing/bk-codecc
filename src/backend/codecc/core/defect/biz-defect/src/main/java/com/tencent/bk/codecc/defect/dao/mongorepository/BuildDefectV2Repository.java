package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildDefectV2Repository extends MongoRepository<BuildDefectV2Entity, String> {

    List<BuildDefectV2Entity> findByTaskIdAndBuildIdAndToolNameIn(Long taskId, String buildId, Set<String> toolNameSet);

    List<BuildDefectV2Entity> findByTaskIdAndBuildIdAndToolNameIn(
            Long taskId,
            String buildId,
            List<String> toolNameSet
    );

    List<BuildDefectV2Entity> findByTaskIdAndBuildIdAndToolName(Long taskId, String buildId, String toolName);

    List<BuildDefectV2Entity> findByTaskIdAndBuildIdAndDefectIdIn(Long taskId, String buildId, Set<String> defectIdSet);

    BuildDefectV2Entity findFirstByTaskIdAndBuildIdAndDefectId(Long taskId, String buildId, String defectId);

    List<BuildDefectV2Entity> findByTaskIdAndBuildIdInAndDefectId(Long taskId, Set<String> buildIdSet, String defectId);

    BuildDefectV2Entity findFirstByTaskIdAndBuildIdAndToolName(Long taskId, String buildId, String toolName);
}
