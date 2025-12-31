package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "工具执行记录请求对象")
public class TaskLogOverviewVO {

    @Schema(description = "实体编号")
    private String entityId;
    @Schema(description = "工具分析信息")
    List<TaskLogVO> taskLogVOList;
    @Schema(description = "代码库字符串信息")
    List<String> repoInfoStrList;
    @Schema(description = "本次工具分析结果")
    List<ToolLastAnalysisResultVO> lastAnalysisResultVOList;
    @Schema(description = "任务ID")
    private Long taskId;
    @Schema(description = "构建号")
    private String buildId;
    @Schema(description = "构建号")
    private String buildNum;
    @Schema(description = "任务状态")
    private Integer status;
    @Schema(description = "代码库版本号")
    private String version;
    @Schema(description = "分析开始时间")
    private Long startTime;
    @Schema(description = "分析结束时间")
    private Long endTime;
    @Schema(description = "扫描耗时")
    private Long elapseTime;
    @Schema(description = "分析触发用户")
    private String buildUser;
    @Schema(description = "工具集", required = true)
    private List<String> tools;
    @Schema(description = "未过滤的工具集")
    private List<String> originScanTools;
    @Schema(description = "插件错误码", required = true)
    private Integer pluginErrorCode;
    @Schema(description = "插件错误类型", required = true)
    private Integer pluginErrorType;
    @Schema(description = "自动识别语言扫描标识，标识本次扫描是识别语言")
    private Boolean autoLanguageScan;
}
