package com.tencent.bk.codecc.defect.model.common;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 记录消费多次后被放弃的提单信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_defect_consumer_retry_limit_log")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_tool_name_1_type",
                def = "{'task_id': 1, 'build_id':1, 'tool_name': 1, 'type':1}")
})
public class DefectConsumerRetryLimitLog extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("build_id")
    private String buildId;

    @Field("tool_name")
    private String toolName;

    @Field("type")
    private String type;

    @Field("message")
    private String message;

}
