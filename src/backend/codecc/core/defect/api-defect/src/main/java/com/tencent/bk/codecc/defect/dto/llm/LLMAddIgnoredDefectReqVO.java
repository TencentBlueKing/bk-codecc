package com.tencent.bk.codecc.defect.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 向 LLM 服务中发请求: 上报这个被忽略的告警
 *
 * @date 2025/03/27
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LLMAddIgnoredDefectReqVO {
    private String defectId;
    private String ignoreReason;    // 忽略原因
    private String message;         // 告警描述
    private Long defectLineNo;      // 告警所在行号
    private String filePath;       // 文件路径
    private String toolName;        // 工具名
    private String ruleName;        // 规则名
    private String lang;            // 语言名
    private String pinpointHash;
    private Long taskId;
    private String projectId;

    private String code;            // 告警所在函数代码片段
    private Long startLineNo;       // 函数开始行号
    private Long endLineNo;         // 函数结束行号

    /**
     * 复制构造函数(部分字段)
     * @param other 要复制的源对象
     */
    public LLMAddIgnoredDefectReqVO(LLMAddIgnoredDefectReqVO other) {
        if (other != null) {
            this.defectId = other.getDefectId();
            this.ignoreReason = other.getIgnoreReason() == null ? "" : other.getIgnoreReason();
            this.message = other.getMessage() == null ? "" : other.getMessage();
            this.defectLineNo = other.getDefectLineNo();
            this.filePath = other.getFilePath();
            this.toolName = other.getToolName();
            this.ruleName = other.getRuleName();
            this.lang = other.getLang();
        }
    }
}
