package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.dto.llm.LLMNegativeDefectFilterReqVO;
import com.tencent.bk.codecc.defect.dto.llm.LLMNegativeDefectLearnVO;

import java.util.List;

/**
 * 基于大模型的误报过滤服务
 */
public interface LLMNegativeDefectFilterService {

    /**
     * 用户新增误报忽略信息
     * @param request
     * @param forHistory true, 代表正在刷历史忽略数据
     */
    void addNegativeDefect(LLMNegativeDefectLearnVO request, boolean forHistory);

    /**
     * 用户取消误报忽略信息
     * @param request
     */
    void deleteNegativeDefect(LLMNegativeDefectLearnVO request);

    /**
     * 开始过滤误报的流程
     *
     * @param projectId
     * @param taskId
     * @param buildId
     * @param toolName
     * @param llmNegativeDefectFilterReqVOS
     */
    void filterNegativeDefects(String projectId, Long taskId, String buildId,
            String toolName, List<LLMNegativeDefectFilterReqVO> llmNegativeDefectFilterReqVOS);

    /**
     * 查看工具 [toolName] 支持使用本服务的所有规则
     */
    List<String> getOpenCheckersByToolName(String toolName);
}
