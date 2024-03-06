package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author victorljli
 * @date 2023/10/19
 */
@Data
@ApiModel("流水线基础信息视图")
public class PipelineBasicInfoVO {
    @ApiModelProperty(value = "流水线 id")
    private String pipelineId;
    @ApiModelProperty(value = "流水线名")
    private String pipelineName;
    @ApiModelProperty(value = "项目 id")
    private String projectId;
}
