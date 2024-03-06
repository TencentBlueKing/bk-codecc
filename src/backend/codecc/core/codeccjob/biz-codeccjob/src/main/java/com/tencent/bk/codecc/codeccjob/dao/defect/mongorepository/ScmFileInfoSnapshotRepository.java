package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScmFileInfoSnapshotRepository extends MongoRepository<ScmFileInfoSnapshotEntity, String> {

    void deleteAllByTaskIdAndBuildIdIn(Long taskId, List<String> buildIdList);

    long deleteByTaskId(long taskId);
}
