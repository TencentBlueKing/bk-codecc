package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.Data;

@Data
public class QueryFileDefectGatherRequest {

    @ApiParam(value = "任务ID", required = false)
    private List<Long> taskIdList;

    @ApiParam(value = "工具名", required = false)
    private List<String> toolNameList;

    @ApiParam(value = "工具维度", required = false)
    private List<String> dimensionList;

    @ApiParam(value = "快照Id", required = false)
    private String buildId;
}
