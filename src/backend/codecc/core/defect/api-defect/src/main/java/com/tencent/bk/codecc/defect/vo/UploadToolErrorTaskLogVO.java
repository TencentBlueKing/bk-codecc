package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * 上报工具扫描失败的错误信息
 *
 * @version V1.0
 * @date 2024/9/23
 */
@Data
@ApiModel("上报工具扫描失败信息的VO")
public class UploadToolErrorTaskLogVO {
    @ApiModelProperty(value = "流名称", required = true)
    @JsonProperty("stream_name")
    private String streamName;

    @ApiModelProperty(value = "任务主键id")
    @JsonProperty("task_id")
    private Long taskId;

    @ApiModelProperty(value = "工具名（ID）", required = true)
    private String toolName;

    @NotNull(message = "构建Id不能为空")
    @ApiModelProperty(value = "构建Id", required = true)
    private String landunBuildId;

    @ApiModelProperty(value = "工具分析错误代码", required = true)
    private long errorCode;

    @ApiModelProperty(value = "工具分析错误信息", required = true)
    @JsonProperty("errorMessage")
    private String errorMsg;
}
