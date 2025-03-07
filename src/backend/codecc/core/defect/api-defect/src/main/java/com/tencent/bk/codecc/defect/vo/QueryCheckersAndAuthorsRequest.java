package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryCheckersAndAuthorsRequest {

    @ApiParam(value = "任务ID", required = false)
    private List<Long> taskIdList;

    @ApiParam(value = "工具名称", required = false)
    @QueryParam(value = "toolName")
    private List<String> toolNameList;

    @ApiParam(value = "维度", required = false)
    @QueryParam(value = "dimension")
    private List<String> dimensionList;

    @ApiParam(value = "SCA维度", required = false)
    private String scaDimension;

    @ApiParam(value = "告警状态，多选逗号分割", required = false)
    @QueryParam(value = "status")
    private List<String> statusList;

    @ApiParam(value = "规则及名称", required = false)
    @QueryParam(value = "checkerSet")
    private String checkerSet;

    @ApiParam(value = "构建Id", required = false)
    @QueryParam(value = "buildId")
    private String buildId;

    @QueryParam(value = "是否跨任务查询")
    private Boolean multiTaskQuery;
}
