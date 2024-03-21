package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.ToolDeveloperInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolDeveloperInfoRepository extends MongoRepository<ToolDeveloperInfoEntity, String> {

    ToolDeveloperInfoEntity findFirstByToolName(String toolName);

}
