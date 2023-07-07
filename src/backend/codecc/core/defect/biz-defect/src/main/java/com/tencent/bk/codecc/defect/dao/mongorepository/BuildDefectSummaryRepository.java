package com.tencent.bk.codecc.defect.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildDefectSummaryRepository extends MongoRepository<BuildDefectSummaryEntity, String> {

    BuildDefectSummaryEntity findFirstByTaskIdAndBuildId(Long taskId, String buildId);

    List<BuildDefectSummaryEntity> findByTaskIdOrderByBuildTimeDesc(Long taskId, Pageable pageable);
}
