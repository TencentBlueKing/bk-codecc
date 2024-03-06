package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.CheckerMisreportStatEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckerMisreportStatRepository extends MongoRepository<CheckerMisreportStatEntity, String> {

    /**
     * 查询指定统计信息
     */
    List<CheckerMisreportStatEntity> findByDataFromAndStatDateAndToolNameAndCheckerNameIn(String dataFrom,
            Long statDate, String toolName, Set<String> checkerSet);
}
