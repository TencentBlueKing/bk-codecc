package com.tencent.bk.codecc.defect.dao.mongorepository.file;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 构建与遗留告警快照
 *
 * @version V1.0
 * @date 2019/12/16
 */
@Repository
public interface ScmFileInfoSnapshotRepository extends MongoRepository<ScmFileInfoSnapshotEntity, String> {

    List<ScmFileInfoSnapshotEntity> findByTaskIdAndBuildIdAndFilePathIn(
            long taskId,
            String buildId,
            Set<String> filePaths
    );
}
