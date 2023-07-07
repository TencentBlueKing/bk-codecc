package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeProjectConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 忽略类型数据存取对象
 *
 * @date 2022/8/23
 */
@Repository
public class IgnoreTypeProjectDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    /**
     * 批量更新project表的系统
     * @param id 忽略类型唯一id
     * @param ignoreTypeNameOld 旧名称
     * @param ignoreTypeNameNew 新名称
     */
    public void updateIgnoreTypeNameById(int id, String ignoreTypeNameOld, String ignoreTypeNameNew) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ignore_type_id").is(id).and("name").is(ignoreTypeNameOld));
        Update update = new Update();
        update.set("name", ignoreTypeNameNew);
        mongoTemplate.updateMulti(query, update, IgnoreTypeProjectConfig.class);
    }
}
