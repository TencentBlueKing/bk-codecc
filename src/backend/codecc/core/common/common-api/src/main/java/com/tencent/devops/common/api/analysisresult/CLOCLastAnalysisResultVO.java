package com.tencent.devops.common.api.analysisresult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "CLOC工具最近一次分析结果")
public class CLOCLastAnalysisResultVO extends BaseLastAnalysisResultVO {
    @Schema(description = "代码行总数")
    private Long sumCode;

    @Schema(description = "空白行总数")
    private Long sumBlank;

    @Schema(description = "注释行总数")
    private Long sumComment;

    @Schema(description = "代码行变化")
    private Long codeChange;

    @Schema(description = "空白行变化")
    private Long blankChange;

    @Schema(description = "注释行变化")
    private Long commentChange;

    @Schema(description = "总行数")
    private Long totalLines;

    @Schema(description = "行数总变化")
    private Long linesChange;

    @Schema(description = "扫描总文件数量")
    private Long fileNum;

    @Schema(description = "文件数量变化")
    private Long fileNumChange;
}
