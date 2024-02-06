package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedLineRepository extends MongoRepository<RedLineEntity, String> {

    long deleteByBuildIdIn(List<String> buildIdList);
}
