package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.PipelineCallbackRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class PipelineCallbackRegisterDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public PipelineCallbackRegister findFirstByPipelineIdAndEvent(String pipelineId, String event) {
        Query query = Query.query(Criteria.where("pipeline_id").is(pipelineId).and("event").is(event));
        return mongoTemplate.findOne(query, PipelineCallbackRegister.class);
    }

    /**
     * 更新PipelineCallbackRegister
     * @param register
     */
    public void upsert(PipelineCallbackRegister register) {
        if (register == null) {
            return;
        }
        PipelineCallbackRegister oldRegister =
                findFirstByPipelineIdAndEvent(register.getPipelineId(), register.getEvent());
        register.setCreatedDate(oldRegister != null ? oldRegister.getCreatedDate() : register.getUpdatedDate());
        register.setCreatedBy(oldRegister != null ? oldRegister.getCreatedBy() : register.getUpdatedBy());

        Query query = Query.query(Criteria.where("pipeline_id").is(register.getPipelineId())
                .and("event").is(register.getEvent()));
        Update update = Update.update("project_id", register.getProjectId())
                .set("callback_name", register.getCallbackName())
                .set("secret", register.getSecret())
                .set("create_date", register.getCreatedDate())
                .set("created_by", register.getCreatedBy())
                .set("updated_date", register.getUpdatedDate())
                .set("updated_by", register.getUpdatedBy());
        mongoTemplate.upsert(query, update, PipelineCallbackRegister.class);
    }
}
