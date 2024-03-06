package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.defect.GithubIssueDefectEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GithubStatDefectRepository extends MongoRepository<GithubIssueDefectEntity, String> {

}
