package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class CountDefectFileRequest {

    @Parameter(description = "任务Id", required = true)
    private Long taskId;

    @Parameter(description = "维度", required = false)
    private List<String> dimensionList;

    @Parameter(description = "处理人", required = false)
    private String author;

    @Parameter(description = "规则", required = false)
    private String checker;

    @Parameter(description = "严重级别", required = false)
    private List<Integer> severityList;
}
