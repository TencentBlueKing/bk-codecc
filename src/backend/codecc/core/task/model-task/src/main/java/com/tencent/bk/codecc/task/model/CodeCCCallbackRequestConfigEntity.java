package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_codecc_callback_request_config")
@CompoundIndexes({
        @CompoundIndex(
                name = "pipeline_id_1_multi_pipeline_mark_1",
                def = "{'pipeline_id': 1, 'multi_pipeline_mark': 1}",
                background = true
        )
})
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CodeCCCallbackRequestConfigEntity extends CommonEntity {

    @Field("url")
    private String url;

    @Field("headers")
    private Map<String,String> headers;
}
