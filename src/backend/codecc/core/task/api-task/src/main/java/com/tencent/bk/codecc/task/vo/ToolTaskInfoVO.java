package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "工具接入停用返回类")
public class ToolTaskInfoVO {
    @Schema(description = "任务id列表", required = true)
    private Map<Integer,List<TaskDetailVO>> taskIdMap;

    @Schema(description = "项目")
    private int totalProjectCount;

    @Schema
    private int totalTaskCount;
}
