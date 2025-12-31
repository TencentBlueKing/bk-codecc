package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "数据报表节点的model类")
public class CovKlocChartVO {

    @Schema(description = "待修复图表展示的最高数量")
    private int unFixMaxHeight;

    @Schema(description = "待修复图表展示的最低数量")
    private int unFixMinHeight;

    @Schema(description = "每日新图表展示的最高数量")
    private int newMaxHeight;

    @Schema(description = "每日新图表展示的最低数量")
    private int newMinHeight;

    @Schema(description = "每日关闭图表展示的最高数量")
    private int closeMaxHeight;

    @Schema(description = "每日关闭图表展示的最低数量")
    private int closeMinHeight;

    @Schema(description = "前端图表数据列表")
    private List<CovKlocChartDateVO> elemList;


}
