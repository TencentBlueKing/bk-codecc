package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.Data;

@Data
public class ListTaskToolDimensionRequest {

    @Parameter(description = "任务Id", required = false)
    private List<Long> taskIdList;
}
