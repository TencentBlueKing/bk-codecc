package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.Data;

@Data
public class ListTaskToolDimensionRequest {

    @ApiParam(value = "任务Id", required = false)
    private List<Long> taskIdList;
}
