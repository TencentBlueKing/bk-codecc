package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CheckerSetPackageDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;

    public void removeByTypeAndLangValueAndCheckerSetId(String type, Long langValue,
            String checkerSetId) {
        Query query = Query.query(Criteria.where("type").is(type)
                .and("lang_value").is(langValue)
                .and("checker_set_id").is(checkerSetId));
        defectCoreMongoTemplate.remove(query, CheckerSetPackageEntity.class);
    }
}
