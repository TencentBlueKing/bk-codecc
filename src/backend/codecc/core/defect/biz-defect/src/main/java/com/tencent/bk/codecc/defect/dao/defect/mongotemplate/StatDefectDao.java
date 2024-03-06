package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.defect.StatDefectEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class StatDefectDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 批量失效告警
     *
     * @param taskId 任务
     * @param toolName 工具
     */
    public void batchDisableStatInfo(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        Update update = new Update();
        update.set("status", "DISABLED");
        defectMongoTemplate.updateMulti(query, update, StatDefectEntity.class);
    }

    /**
     * 批量写入增量告警
     *
     * @param taskId 任务ID
     * @param toolName 工具名
     * @param customTollParam 自定义参数集
     * @param defectEntityList 告警列表
     */
    public void upsert(long taskId, String toolName, List<StatDefectEntity> defectEntityList,
            Map<String, String> customTollParam) {
        if (CollectionUtils.isNotEmpty(defectEntityList) && !customTollParam.isEmpty()) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, StatDefectEntity.class);
            defectEntityList.forEach(defectEntity ->
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
                Update update = new Update();
                update.set("status", "ENABLED");
                update.set("msg_id", defectEntity.getMsgId());
                update.set("time_stamp", defectEntity.getTimeStamp());
                update.set("user_name", defectEntity.getUsername());
                update.set("msg_body", defectEntity.getMsgBody());
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public List<Document> getByTaskIdAndToolNameAndTime(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        return defectMongoTemplate.find(query, Document.class, "t_stat_defect");
    }

    public List<StatDefectEntity> getByTaskIdAndToolNameOrderByTime(long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("task_id").is(taskId).and("tool_name").is(toolName).and("status").is("ENABLED"));
        query.with(Sort.by(new Order(Direction.DESC, "time_stamp")));
        query.limit(1);
        return defectMongoTemplate.find(query, StatDefectEntity.class, "t_stat_defect");
    }

    public void save(StatDefectEntity statDefectEntity) {
        if (statDefectEntity == null) {
            return;
        }
        defectMongoTemplate.save(statDefectEntity);
    }

    public void saveAll(List<StatDefectEntity> statDefectEntities) {
        if (CollectionUtils.isNotEmpty(statDefectEntities)) {
            for (StatDefectEntity statDefectEntity : statDefectEntities) {
                save(statDefectEntity);
            }
        }
    }
}
