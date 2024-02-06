package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class TaskPersonalStatisticDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;

    public List<String> getLintAuthorSet(Set<Long> taskIdList) {
        Criteria criteria = Criteria.where("task_id").in(taskIdList)
                .orOperator(
                        Criteria.where("defect_count").gt(0L),
                        Criteria.where("security_count").gt(0L),
                        Criteria.where("standard_count").gt(0L)
                );

        return getAuthorListCore(criteria);
    }

    public List<String> getCCNAuthorSet(Set<Long> taskIdList) {
        Criteria criteria = Criteria.where("task_id").in(taskIdList)
                .and("risk_count").gt(0L);

        return getAuthorListCore(criteria);
    }

    private List<String> getAuthorListCore(Criteria criteria) {
        Query query = new BasicQuery(new Document(), new Document("username", true));
        query.addCriteria(criteria);
        query.withHint("task_id_1_user_name_1");

        try {
            return defectCoreMongoTemplate.findDistinct(query, "username", "t_task_personal_statistic", String.class);
        } catch (Throwable t) {
            log.error("get author list fail", t);
            return Lists.newArrayList();
        }
    }
}
