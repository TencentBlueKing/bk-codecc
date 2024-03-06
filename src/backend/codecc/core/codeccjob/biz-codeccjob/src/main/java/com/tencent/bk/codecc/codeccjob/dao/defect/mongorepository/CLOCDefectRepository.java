package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CLOCDefectRepository extends MongoRepository<CLOCDefectEntity, String> {

    List<CLOCDefectEntity> findByTaskId(long taskId, Pageable pageable);

    long deleteByTaskId(long taskId);

    CLOCDefectEntity findFirstByTaskId(long taskId);
}
