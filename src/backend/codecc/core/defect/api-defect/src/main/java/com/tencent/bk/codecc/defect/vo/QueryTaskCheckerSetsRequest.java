package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTaskCheckerSetsRequest {

    @Parameter(description = "任务Id", required = false)
    private List<Long> taskIdList;

    @Parameter(description = "工具名称", required = false)
    private List<String> toolNameList;

    @Parameter(description = "维度", required = false)
    private List<String> dimensionList;

    @Parameter(description = "快照Id", required = false)
    private String buildId;
}
