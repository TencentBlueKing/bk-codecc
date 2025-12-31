package com.tencent.bk.codecc.codeccjob.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLMFilterProgress MQ 的请求 VO
 *
 * @date 2025/07/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LLMFilterProgressReqVO {
    private Long taskId;
    private String buildId;
}
