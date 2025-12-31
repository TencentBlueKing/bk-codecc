package com.tencent.bk.codecc.defect.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 服务内部请求: 开始误报过滤流程, 判断 llmNegativeDefectFilterReqVOS 中的告警是否误报
 *
 * @date 2025/04/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LLMNegativeDefectFilterVO {
    Long taskId;
    String buildId;
    List<LLMNegativeDefectFilterReqVO> llmNegativeDefectFilterReqVOS;
}
