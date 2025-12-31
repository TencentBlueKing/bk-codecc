package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema
public class MetricsVO {

    @Schema(description = "分析结果，各工具告警数")
    List<Analysis> lastAnalysisResultList;

    @Schema(description = "任务ID")
    private long taskId;

    @Schema(description = "本次度量计算的代码库版本")
    private String commitId;

    @Schema(description = "代码库路径")
    private String repoUrl;

    @Schema(description = "对应 CodeCC 页面路径")
    private String codeccUrl;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "代码规范计算得分")
    private double codeStyleScore;

    @Schema(description = "代码安全计算得分")
    private double codeSecurityScore;

    @Schema(description = "代码度量计算得分")
    private double codeMeasureScore;

    @Schema(description = "圈复杂度计算得分")
    private double codeCcnScore;

    @Schema(description = "缺陷计算得分(Coverity)")
    private double codeDefectScore;

    @Schema(description = "代码库总分")
    private double rdIndicatorsScore;

    @Schema(description = "平均千行复杂度超标数")
    private double averageThousandDefect;

    @Schema(description = "一般程度规范告警数")
    private int codeStyleNormalDefectCount;

    @Schema(description = "一般程度规范告警平均千行数")
    private double averageNormalStandardThousandDefect;

    @Schema(description = "严重程度规范告警数")
    private int codeStyleSeriousDefectCount;

    @Schema(description = "严重程度规范告警平均千行数")
    private double averageSeriousStandardThousandDefect;

    @Schema(description = "一般程度度量告警数")
    private int codeDefectNormalDefectCount;

    @Schema(description = "一般程度缺陷告警平均千行数")
    private double averageNormalDefectThousandDefect;

    @Schema(description = "严重程度度量告警数")
    private int codeDefectSeriousDefectCount;

    @Schema(description = "严重程度缺陷告警平均千行数")
    private double averageSeriousDefectThousandDefect;

    @Schema(description = "一般程度安全告警数")
    private int codeSecurityNormalDefectCount;

    @Schema(description = "严重程度安全告警数")
    private int codeSecuritySeriousDefectCount;

    @Schema(description = "是否符合开源治理环境")
    private boolean isOpenScan;

    @Schema(description = "扫描任务状态")
    private int status;

    @Schema(description = "扫描失败信息")
    private String message;

    @Schema(description = "分析时间")
    private long lastAnalysisTime;

    @Schema
    private Long totalSecurityDefectCount;

    @Schema
    private Long totalDefectCount;

    @Schema
    private Long totalStyleDefectCount;

    @Data
    public static class Analysis {

        private String toolName;

        private String displayName;

        private String type;

        private long elapseTime;

        private int buildNum;

        private String pattern;

        private int defectCount;

        private String defectUrl;
    }
}
