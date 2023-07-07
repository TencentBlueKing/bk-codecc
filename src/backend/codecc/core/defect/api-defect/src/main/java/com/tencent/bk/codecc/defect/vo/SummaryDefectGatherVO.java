package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel("文件告警收敛")
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDefectGatherVO {

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件总数")
    private Integer fileCount;

    @ApiModelProperty(value = "告警总数")
    private Integer defectCount;
}
