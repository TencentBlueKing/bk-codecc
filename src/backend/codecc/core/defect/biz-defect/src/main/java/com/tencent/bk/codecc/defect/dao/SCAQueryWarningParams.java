package com.tencent.bk.codecc.defect.dao;

import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@ApiModel("SCA告警列表数据查询参数类，用于传递处理后的过滤条件")
public class SCAQueryWarningParams {
    @ApiModelProperty("处理后的SCA告警查询请求参数")
    private SCADefectQueryReqVO scaDefectQueryReqVO;

    @ApiModelProperty("告警查询的任务-工具列表")
    private Map<Long, List<String>> taskToolMap;

    @ApiModelProperty("告警的数据库id")
    private Set<String> scaDefectMongoIdSet;

    @ApiModelProperty("告警聚类,预留")
    private Map<String, Boolean> filedMap;

    @ApiModelProperty("筛选规则,预留")
    private Set<String> pkgChecker;
}
