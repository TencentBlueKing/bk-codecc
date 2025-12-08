package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;


/**
 * 上报工具扫描失败的错误信息
 *
 * @version V1.0
 * @date 2024/9/23
 */
@Data
@Schema(description = "上报工具扫描失败信息的VO")
public class UploadToolErrorTaskLogVO {
    @Schema(description = "流名称", required = true)
    @JsonProperty("stream_name")
    private String streamName;

    @Schema(description = "任务主键id")
    @JsonProperty("task_id")
    private Long taskId;

    @Schema(description = "工具名（ID）", required = true)
    private String toolName;

    @NotNull(message = "构建Id不能为空")
    @Schema(description = "构建Id", required = true)
    private String landunBuildId;

    @Schema(description = "工具分析错误代码", required = true)
    private long errorCode;

    @Schema(description = "工具分析错误信息", required = true)
    @JsonProperty("errorMessage")
    private String errorMsg;
}
