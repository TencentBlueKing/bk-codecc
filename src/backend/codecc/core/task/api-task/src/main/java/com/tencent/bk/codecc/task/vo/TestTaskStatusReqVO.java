package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 获取测试任务的状态的请求VO
 *
 * @date 2025/07/17
 */
@Data
@Schema(description = "获取测试任务的状态的请求视图")
public class TestTaskStatusReqVO {
    @Schema(description = "工具名", required = true)
    private String toolName;
    @Schema(description = "版本", required = true)
    private String version;
}
