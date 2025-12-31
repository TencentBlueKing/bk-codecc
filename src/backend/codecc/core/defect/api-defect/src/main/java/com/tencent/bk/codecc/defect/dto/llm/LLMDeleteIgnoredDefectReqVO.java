package com.tencent.bk.codecc.defect.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向 LLM 服务中发请求: 删除已上报的被忽略告警
 *
 * @date 2025/04/09
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LLMDeleteIgnoredDefectReqVO {
    List<String> defectIds;
}
