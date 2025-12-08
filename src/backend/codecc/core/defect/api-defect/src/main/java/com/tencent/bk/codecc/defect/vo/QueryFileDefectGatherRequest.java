package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.Data;

@Data
public class QueryFileDefectGatherRequest {

    @Parameter(description = "任务ID", required = false)
    private List<Long> taskIdList;

    @Parameter(description = "工具名", required = false)
    private List<String> toolNameList;

    @Parameter(description = "工具维度", required = false)
    private List<String> dimensionList;

    @Parameter(description = "快照Id", required = false)
    private String buildId;
}
