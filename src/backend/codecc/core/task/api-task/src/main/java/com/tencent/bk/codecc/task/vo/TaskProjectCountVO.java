package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务项目id维度统计")
public class TaskProjectCountVO {

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("project_count")
    private String projectCount;

}
