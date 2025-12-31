package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVOBase;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryCheckersAndAuthorsRequest {

    @Parameter(description = "任务ID", required = false)
    private List<Long> taskIdList;

    @Parameter(description = "工具名称", required = false)
    @QueryParam(value = "toolName")
    private List<String> toolNameList;

    @Parameter(description = "维度", required = false)
    @QueryParam(value = "dimension")
    private List<String> dimensionList;

    @Parameter(description = "SCA维度", required = false)
    private String scaDimension;

    @Parameter(description = "告警状态，多选逗号分割", required = false)
    @QueryParam(value = "status")
    private List<String> statusList;

    @Parameter(description = "规则及名称列表", required = false)
    @QueryParam(value = "checkerSet")
    private String checkerSet;

    @Parameter(description = "规则及名称列表", required = false)
    @QueryParam(value = "checkerSets")
    private List<DefectQueryReqVOBase.CheckerSet> checkerSets;

    @Parameter(description = "构建Id", required = false)
    @QueryParam(value = "buildId")
    private String buildId;

    @QueryParam(value = "是否跨任务查询")
    private Boolean multiTaskQuery;
}
