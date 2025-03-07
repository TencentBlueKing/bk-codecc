package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CodeCCCallbackRegisterDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 使用替换，直接覆盖
     *
     * @param register
     */
    public void saveRegister(CodeCCCallbackRegister register) {
        if (register == null) {
            return;
        }

        Query query = Query.query(Criteria.where("task_id").is(register.getTaskId()));
        mongoTemplate.findAndReplace(query, register, FindAndReplaceOptions.options().upsert());
    }

}
