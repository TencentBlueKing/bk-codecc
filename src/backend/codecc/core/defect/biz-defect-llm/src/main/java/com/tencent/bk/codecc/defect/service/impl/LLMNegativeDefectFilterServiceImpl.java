package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dto.llm.LLMNegativeDefectFilterReqVO;
import com.tencent.bk.codecc.defect.dto.llm.LLMNegativeDefectLearnVO;
import com.tencent.bk.codecc.defect.service.LLMNegativeDefectFilterService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 基于大模型的误报过滤服务
 *
 * @date 2025/03/28
 */
@Service
public class LLMNegativeDefectFilterServiceImpl implements LLMNegativeDefectFilterService {
    @Override
    public void addNegativeDefect(LLMNegativeDefectLearnVO request, boolean forHistory) {
        // 暂不开源
    }

    @Override
    public void deleteNegativeDefect(LLMNegativeDefectLearnVO request) {

    }

    @Override
    public void filterNegativeDefects(String projectId, Long taskId, String buildId,
            String toolName, List<LLMNegativeDefectFilterReqVO> llmNegativeDefectFilterReqVOS) {

    }

    @Override
    public List<String> getOpenCheckersByToolName(String toolName) {
        return Collections.emptyList();
    }
}
