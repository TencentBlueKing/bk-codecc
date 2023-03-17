package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.model.I18NMessageEntity;
import com.tencent.bk.codecc.task.pojo.I18NQueryModel;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
public class I18NMessageDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据条件列表查询
     *
     * @param queryModelList
     * @return
     */
    public List<I18NMessageEntity> query(List<I18NQueryModel> queryModelList) {
        if (CollectionUtils.isEmpty(queryModelList)) {
            return Lists.newArrayList();
        }

        List<Criteria> orCriteriaList = Lists.newArrayList();
        for (I18NQueryModel queryModel : queryModelList) {
            if (CollectionUtils.isEmpty(queryModel.getKeySet())
                    || StringUtils.isEmpty(queryModel.getLocale())
                    || StringUtils.isEmpty(queryModel.getModuleCode())) {
                continue;
            }

            orCriteriaList.add(
                    Criteria.where("module_code").is(queryModel.getModuleCode())
                            .and("key").in(queryModel.getKeySet())
                            .and("locale").is(queryModel.getLocale())
            );
        }

        Query query = Query.query(new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0])));

        return mongoTemplate.find(query, I18NMessageEntity.class);
    }
}
