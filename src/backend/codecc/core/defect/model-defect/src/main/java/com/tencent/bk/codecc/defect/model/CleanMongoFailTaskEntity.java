package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_clean_mongo_fail_task")
@CompoundIndexes({
        @CompoundIndex(name = "clean_date_1_task_id_1", def = "{'clean_date':1, 'task_id':1}")
})
public class CleanMongoFailTaskEntity {
    @Field("task_id")
    private Long taskId;

    @Field("node_ip")
    private String nodeIp;

    @Field("clean_date")
    private Integer cleanDate;

    @Field("stack_trace")
    private String stackTrace;

    @Field("message")
    private String message;
}
