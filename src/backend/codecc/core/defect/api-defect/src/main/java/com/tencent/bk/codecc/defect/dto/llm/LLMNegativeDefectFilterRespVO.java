package com.tencent.bk.codecc.defect.dto.llm;

import lombok.Data;

import java.util.List;

/**
 * LLM 服务返回结果
 *
 * @date 2025/04/14
 */
@Data
public class LLMNegativeDefectFilterRespVO {
    private String defectId;            // 告警id（非空）
    private Boolean result;             // 判断结果（true是误报，false不是误报）
    private String reason;              // 判断依据
    private List<String> referDefects;  // 纳入参考的告警id清单
}
