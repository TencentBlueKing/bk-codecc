package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTaskCheckerSetsRequest {

    @ApiParam(value = "任务Id", required = false)
    private List<Long> taskIdList;

    @ApiParam(value = "工具名称", required = false)
    private List<String> toolNameList;

    @ApiParam(value = "维度", required = false)
    private List<String> dimensionList;

    @ApiParam(value = "快照Id", required = false)
    private String buildId;
}
