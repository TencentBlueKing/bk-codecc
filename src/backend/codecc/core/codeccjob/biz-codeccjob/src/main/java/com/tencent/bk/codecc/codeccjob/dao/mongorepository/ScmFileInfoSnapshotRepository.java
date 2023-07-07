package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Repository
public interface ScmFileInfoSnapshotRepository extends MongoRepository<ScmFileInfoSnapshotEntity, String> {
    void deleteAllByTaskIdAndBuildIdIn(Long taskId, List<String> buildIdList);
}
