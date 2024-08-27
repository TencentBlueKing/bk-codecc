package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 以误报为理由忽略的告警的列表展示视图
 *
 * @date 2024/01/16
 */
@Data
public class ListNegativeDefectReqVO {

    @ApiModelProperty(value = "规则名称")
    private Set<String> checkerNames;

    @ApiModelProperty(value = "规则标签")
    private Set<String> checkerTags;

    @ApiModelProperty(value = "严重级别")
    private Set<Integer> severities;

    @ApiModelProperty(value = "规则发布者")
    private Set<String> publishers;

    @ApiModelProperty(value = "来源")
    private Set<String> createFroms;

    @ApiModelProperty(value = "起始日期")
    private String startDate;

    @ApiModelProperty(value = "截止日期")
    private String endDate;

    @ApiModelProperty(value = "组织 id")
    private List<List<Integer>> organizationIds;

    @ApiModelProperty(value = "处理进展")
    private List<Integer> processProgresses;

}
