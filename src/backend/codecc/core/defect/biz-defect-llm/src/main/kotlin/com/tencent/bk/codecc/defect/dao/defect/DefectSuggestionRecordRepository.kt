package com.tencent.bk.codecc.defect.dao.defect

import com.tencent.bk.codecc.defect.model.DefectSuggestionRecordEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DefectSuggestionRecordRepository : MongoRepository<DefectSuggestionRecordEntity, String> {

    fun findByDefectId(defectId: String): List<DefectSuggestionRecordEntity?>?
    fun findFirstByDefectId(defectId: String): DefectSuggestionRecordEntity?
}
