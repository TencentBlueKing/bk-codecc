package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * CodeCC 回调事件注册
 */
@Data
@Document(collection = "t_codecc_callback_register")
@CompoundIndexes({
        @CompoundIndex(
                name = "task_id_1",
                def = "{'task_id': 1}",
                background = true
        )
})
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CodeCCCallbackRegister extends CommonEntity {

    /***
     * 项目id
     */
    @Field("project_id")
    private String projectId;

    /**
     * 流水线id
     */
    @Field("pipeline_id")
    private String pipelineId;

    /**
     * 流水线id
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 事件
     */
    @Field("events")
    private List<String> events;

    /**
     * 回调的Url（仅支持本插件使用）
     */
    @Field("callback_url")
    private String callbackUrl;

    /**
     * 是否启用
     */
    @Field("status")
    private Integer status;
}
