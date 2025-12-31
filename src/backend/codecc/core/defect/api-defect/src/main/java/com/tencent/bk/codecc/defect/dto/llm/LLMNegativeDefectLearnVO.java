package com.tencent.bk.codecc.defect.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 服务内部请求: 开始误报学习过程. 主要就是把用户的忽略误报/取消忽略  信息发给 LLM 服务作为机器学习的材料.
 *
 * @date 2025/03/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LLMNegativeDefectLearnVO {
    private Long taskId;
    private String projectId;
    private String userName;
    private List<LLMAddIgnoredDefectReqVO> llmIgnoredDefectInfos;

    private Boolean isDelete;
    private Set<String> deletedDefectIds;
}
