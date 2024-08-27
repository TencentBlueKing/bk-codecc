package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 记录将 id = asyn_task_id 的任务作为异步任务的流水线, 在删除自建任务的时候需要用到这个关系信息.
 *
 * @date 2023/10/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_pipeline_asyntask_relationship")
public class PipelineIdAsyntaskIdRelationshipEntity extends CommonEntity {
    @Indexed
    @Field("asyn_task_id")
    Long asynTaskId;

    @Field("pipeline_id")
    String pipelineId;

    @Field("project_id")
    String projectId;
}
