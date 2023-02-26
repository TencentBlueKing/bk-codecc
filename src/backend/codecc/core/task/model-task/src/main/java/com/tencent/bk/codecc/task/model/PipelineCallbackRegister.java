package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;



@Document(collection = "t_pipeline_callback_register")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "pipeline_id_1_event_1_index",
                def = "{'pipeline_id': 1, 'event': 1}", background = true)
})
public class PipelineCallbackRegister extends CommonEntity {

    @Field("project_id")
    private String projectId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("event")
    private String event;

    @Field("callback_name")
    private String callbackName;

    @Field("secret")
    private String secret;

}
