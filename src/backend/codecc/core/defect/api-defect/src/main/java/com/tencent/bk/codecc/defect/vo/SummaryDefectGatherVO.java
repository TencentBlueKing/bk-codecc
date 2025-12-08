package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "文件告警收敛")
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDefectGatherVO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件总数")
    private Integer fileCount;

    @Schema(description = "告警总数")
    private Integer defectCount;
}
