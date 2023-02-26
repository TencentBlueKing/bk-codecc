package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListToolNameRequest {

    @ApiParam(value = "任务", required = false)
    private List<Long> taskIdList;

    @ApiParam(value = "快照", required = false)
    private String buildId;

    @ApiParam(value = "维度", required = false)
    private List<String> dimensionList;
}
