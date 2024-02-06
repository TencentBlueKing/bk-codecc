package com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaskPersonalStatisticDao {

    @Autowired
    MongoTemplate defectMongoTemplate;

    public boolean batchSave(Collection<TaskPersonalStatisticEntity> entities) {
        if (entities.isEmpty()) {
            return false;
        }
        defectMongoTemplate.insert(entities, TaskPersonalStatisticEntity.class);
        return true;
    }
}
