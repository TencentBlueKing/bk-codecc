package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    public List<CheckerSetPackageEntity> findCheckerSetIdListByType(String type) {
        Criteria criteria = Criteria.where("type").is(type)
                .and("env_type").is(ComConstants.CheckerSetEnvType.PROD.getKey());
        Query query = new Query(criteria);
        query.fields().include("checker_set_id", "version");
        return defectCoreMongoTemplate.find(query, CheckerSetPackageEntity.class);
    }
}
