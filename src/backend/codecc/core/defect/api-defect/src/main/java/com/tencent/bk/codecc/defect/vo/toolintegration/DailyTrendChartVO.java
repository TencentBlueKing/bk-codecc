package com.tencent.bk.codecc.defect.vo.toolintegration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DailyTrendChartVO {
    /**
     * @see com.tencent.devops.common.constant.ComConstants.DefectStatType
     */
    @Schema(description = "统计业务类型")
    private String statType;

    @Schema(description = "工具名")
    private String toolName;

    @Schema(description = "统计归属日期，格式：yyyy-MM-dd")
    private String date;

    @Schema(description = "当天分析成功次数")
    private long analysisSuccessCount;

    @Schema(description = "当天分析总次数")
    private long analysisCount;

    @Schema(description = "当天所扫描代码仓库数量")
    private long codeRepoCount;

    @Schema(description = "当天代码仓库新增数量")
    private long codeRepoNewAddCount;
}
