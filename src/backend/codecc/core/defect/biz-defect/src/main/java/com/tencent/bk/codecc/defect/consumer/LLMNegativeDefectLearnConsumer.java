package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dto.llm.LLMNegativeDefectLearnVO;
import com.tencent.bk.codecc.defect.service.LLMNegativeDefectFilterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LLMNegativeDefectLearn MQ 的消费者
 *
 * @date 2025/03/27
 */
@Slf4j
@Component
public class LLMNegativeDefectLearnConsumer {
    @Autowired(required = false)
    private LLMNegativeDefectFilterService llmNegativeDefectFilterService;

    public void consumer(LLMNegativeDefectLearnVO request) {
        if (request == null) {
            return;
        }

        // 如果没有注入 LLM 服务（未引入 biz-defect-llm 模块），则跳过处理
        if (llmNegativeDefectFilterService == null) {
            log.debug("LLMNegativeDefectFilterService is not available, skip processing");
            return;
        }

        try {
            log.info("LLMNegativeDefectLearn MQ consumer: {}", request);
            if (BooleanUtils.isTrue(request.getIsDelete())) {
                llmNegativeDefectFilterService.deleteNegativeDefect(request);
            } else {
                llmNegativeDefectFilterService.addNegativeDefect(request, false);
            }

        } catch (Exception e) {
            log.error("LLMNegativeDefectLearn MQ consumer error: {}", e.getMessage());
        }
    }
}
