package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class CountDefectFileRequest {

    @ApiParam(value = "任务Id", required = true)
    private Long taskId;

    @ApiParam(value = "维度", required = false)
    private List<String> dimensionList;

    @ApiParam(value = "处理人", required = false)
    private String author;

    @ApiParam(value = "规则", required = false)
    private String checker;

    @ApiParam(value = "严重级别", required = false)
    private List<Integer> severityList;
}
