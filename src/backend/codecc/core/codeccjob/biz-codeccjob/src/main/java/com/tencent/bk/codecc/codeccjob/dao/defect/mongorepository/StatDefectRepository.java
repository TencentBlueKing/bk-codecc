package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.StatDefectEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatDefectRepository extends MongoRepository<StatDefectEntity, String> {

    long deleteByTaskId(long taskId);

    List<StatDefectEntity> findByTaskId(long taskId, Pageable pageable);

    StatDefectEntity findFirstByTaskId(long taskId);
}
