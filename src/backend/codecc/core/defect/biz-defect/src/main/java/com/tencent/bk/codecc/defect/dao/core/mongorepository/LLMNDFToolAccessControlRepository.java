package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.LLMNDFToolAccessControlEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * LLMNDFToolAccessControlEntity 的 Repository 类
 *
 * @date 2025/06/26
 */
@Repository
public interface LLMNDFToolAccessControlRepository extends MongoRepository<LLMNDFToolAccessControlEntity, String> {
    Boolean existsByToolNameAndOpenCheckersContains(String toolName, String checker);
}
