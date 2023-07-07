package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_clean_mongo_data_log")
@CompoundIndexes({
        @CompoundIndex(name = "clean_date_1_node_ip_1", def = "{'clean_date':1, 'node_ip':1}")
})
public class CleanMongoDataLogEntity {
    @Id
    private String entityId;

    @Field("node_index")
    private Integer nodeIndex;

    @Field("node_num")
    private Long nodeNum;

    @Field("partition")
    private Long partition;

    @Field("clean_date")
    private Integer cleanDate;

    @Field("node_ip")
    private String nodeIp;

    @Field("thread_id")
    private Integer threadId;

    @Field("clean_task_size")
    private Integer cleanTaskSize;

    @Field("cleaned_task_size")
    private Integer cleanedTaskSize;

    @Field("cost_time")
    private Long costTime;

    @Field("stack_trace")
    private String stackTrace;

    @Field("message")
    private String message;
}
