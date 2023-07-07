package com.tencent.bk.codecc.codeccjob.dao.mongorepository;

import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckerDetailRepository extends MongoRepository<CheckerDetailEntity, String> {

    @Query(fields = "{'checker_key': 1, 'tool_name': 1}")
    List<CheckerDetailEntity> findByCheckerCategoryAndToolNameIn(String checkerCategory, List<String> toolNames);
}
