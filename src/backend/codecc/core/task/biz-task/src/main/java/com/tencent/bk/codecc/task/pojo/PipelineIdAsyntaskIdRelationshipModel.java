package com.tencent.bk.codecc.task.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * t_pipeline_asyntask_relationship 表的 model
 *
 * @author victorljli
 * @date 2023/10/26
 */
@Data
public class PipelineIdAsyntaskIdRelationshipModel {
    private Long asynTaskId;

    private String pipelineId;

    private String projectId;

    /**
     * 创建时间
     */
    private Long createdDate;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新时间
     */
    private Long updatedDate;

    /**
     * 更新人
     */
    private String updatedBy;
}
