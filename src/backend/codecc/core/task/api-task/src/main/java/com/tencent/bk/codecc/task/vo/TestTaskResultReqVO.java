package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工具自助上架: 获取测试任务结果的请求 VO
 *
 * @date 2025/07/17
 */
@Data
@Schema(description = "获取测试任务结果的请求视图")
public class TestTaskResultReqVO {
    @Schema(description = "工具名", required = true)
    private String toolName;
    @Schema(description = "版本", required = true)
    private String version;
    @Schema(description = "stage")
    private Integer stage;
}
