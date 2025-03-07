package com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca

import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface SCASbomInfoRepository : MongoRepository<SCASbomInfoEntity, String>
