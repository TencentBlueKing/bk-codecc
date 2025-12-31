package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "代码统计条目信息返回视图")
public class CLOCDefectQueryRspInfoVO {

    @Schema(description = "语言名称")
    private String language;

    @Schema(description = "当前语言行数总和")
    private long sumLines;

    @Schema(description = "当前语言空白行数")
    private long sumBlank;

    @Schema(description = "当前语言代码行数")
    private long sumCode;

    @Schema(description = "当前语言注释行数")
    private long sumComment;

    @Schema(description = "注释率")
    private Double commentRate;

    @Schema(description = "当前语言有效注释行数")
    private Long sumEfficientComment;

    @Schema(description = "有效注释率")
    private Double efficientCommentRate;

    @Schema(description = "当前语言行数占比")
    private int proportion;

    @Schema(description = "项目总文件数")
    private long sumFileNum;
}
