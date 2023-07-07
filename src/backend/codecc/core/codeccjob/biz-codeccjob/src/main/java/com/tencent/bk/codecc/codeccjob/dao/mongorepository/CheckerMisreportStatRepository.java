package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CheckerMisreportStatEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CheckerMisreportStatRepository extends MongoRepository<CheckerMisreportStatEntity, String> {

    /**
     * 查询指定统计信息
     */
    List<CheckerMisreportStatEntity> findByDataFromAndStatDateAndToolNameAndCheckerNameIn(String dataFrom,
            Long statDate, String toolName, Set<String> checkerSet);
}
