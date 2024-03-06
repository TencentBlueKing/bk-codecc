package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskStatisticVO {
    @JsonProperty("bg_id")
    private int bgId;

    @JsonProperty("dept_Id")
    private int deptId;

    @JsonProperty("task_count")
    private int taskCount;
}
