package com.tencent.bk.codecc.defect.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向 LLM 服务中发请求: 判断该告警是否误报
 *
 * @date 2025/04/14
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LLMNegativeDefectFilterReqVO {
    private String defectId;    // 告警id（非空）
    private String code;        // 告警所在函数代码片段（非空）
    private String message;     // 告警描述（非空）
    private Long defectLineNo;  // 告警所在行号（非空）
    private Long startLineNo;   // 函数开始行号（非空）
    private Long endLineNo;     // 函数结束行号（非空）
    private String filePath;    // 文件路径（可为空）
    private String ruleName;    // 规则名（非空）
    private String toolName;    // 工具名（非空）
    private String lang;        // 语言名（非空）
    private Long taskId;        // 扫描任务id（非空）
    private String projectId;   // 蓝盾项目id（非空）
    private String caseType;
    private String fileName;
    private String pinpointHash;

    private Integer status;             // 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16)
    private Integer ignoreReasonType;   // 告警忽略原因类型
    private String ignoreReason;        // 忽略原因

    @JsonIgnore
    private Boolean excluded;   // 被排除的告警不用过滤
}
