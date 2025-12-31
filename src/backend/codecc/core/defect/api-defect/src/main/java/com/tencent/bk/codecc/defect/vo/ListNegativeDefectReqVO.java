package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "规则名称")
    private Set<String> checkerNames;

    @Schema(description = "规则标签")
    private Set<String> checkerTags;

    @Schema(description = "严重级别")
    private Set<Integer> severities;

    @Schema(description = "规则发布者")
    private Set<String> publishers;

    @Schema(description = "来源")
    private Set<String> createFroms;

    @Schema(description = "起始日期")
    private String startDate;

    @Schema(description = "截止日期")
    private String endDate;

    @Schema(description = "组织 id")
    private List<List<Integer>> organizationIds;

    @Schema(description = "处理进展")
    private List<Integer> processProgresses;

}
