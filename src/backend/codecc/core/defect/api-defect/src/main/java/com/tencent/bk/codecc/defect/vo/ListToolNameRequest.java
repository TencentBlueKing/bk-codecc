package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListToolNameRequest {

    @Parameter(description = "任务", required = false)
    private List<Long> taskIdList;

    @Parameter(description = "快照", required = false)
    private String buildId;

    @Parameter(description = "维度", required = false)
    private List<String> dimensionList;
}
