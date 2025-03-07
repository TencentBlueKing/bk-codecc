package com.tencent.bk.codecc.defect.vo.toolintegration;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DailyTrendChartVO {
    /**
     * @see com.tencent.devops.common.constant.ComConstants.DefectStatType
     */
    @ApiModelProperty("统计业务类型")
    private String statType;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("统计归属日期，格式：yyyy-MM-dd")
    private String date;

    @ApiModelProperty("当天分析成功次数")
    private long analysisSuccessCount;

    @ApiModelProperty("当天分析总次数")
    private long analysisCount;

    @ApiModelProperty("当天所扫描代码仓库数量")
    private long codeRepoCount;

    @ApiModelProperty("当天代码仓库新增数量")
    private long codeRepoNewAddCount;
}
