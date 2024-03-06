package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.BuildEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildRepository extends MongoRepository<BuildEntity, String> {

    void deleteAllByBuildIdIn(List<String> buildIds);

    List<BuildEntity> findByBuildIdIn(List<String> buildIds);

    long deleteByBuildIdIn(List<String> buildIdList);
}
