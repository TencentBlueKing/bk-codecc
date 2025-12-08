package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流水线基础信息视图
 *
 * @date 2023/10/19
 */
@Data
@Schema(description = "流水线基础信息视图")
public class PipelineBasicInfoVO {
    @Schema(description = "流水线 id")
    private String pipelineId;
    @Schema(description = "流水线名")
    private String pipelineName;
    @Schema(description = "项目 id")
    private String projectId;
}
