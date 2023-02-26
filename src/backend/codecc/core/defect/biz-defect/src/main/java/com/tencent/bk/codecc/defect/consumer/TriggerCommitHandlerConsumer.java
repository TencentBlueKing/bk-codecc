package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.pojo.HandlerDTO;
import com.tencent.bk.codecc.defect.service.impl.handler.SaveSnapshotSummaryHandler;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 提单完成后续处理逻辑消费者
 * 用于 cov 和 kloc 工具，其他工具见 EndReportAop
 *
 * @version V1.0
 * @date 2021/09/07
 */
@Slf4j
@Component
public class TriggerCommitHandlerConsumer implements IConsumer<CommitDefectVO> {

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private SaveSnapshotSummaryHandler saveSnapshotSummaryHandler;

    @Override
    public void consumer(CommitDefectVO commitDefectVO) {
        try {
            businessCore(commitDefectVO);
        } catch (Throwable t) {
            log.error("TriggerCommitHandlerConsumer error, mq msg: {}", commitDefectVO, t);
        }
    }

    private void businessCore(CommitDefectVO commitDefectVO) {
        log.info("begin to trigger handler: {} {} {}", commitDefectVO.getTaskId(),
                commitDefectVO.getBuildId(), commitDefectVO.getToolName());
        HandlerDTO handlerDTO = generateHandlerDTO(commitDefectVO);
        saveSnapshotSummaryHandler.handler(handlerDTO);
    }

    private HandlerDTO generateHandlerDTO(CommitDefectVO commitDefectVO) {
        if (commitDefectVO != null) {
            HandlerDTO handlerDTO = new HandlerDTO(
                    toolMetaCacheService.getToolPattern(commitDefectVO.getToolName()));
            BeanUtils.copyProperties(commitDefectVO, handlerDTO);
            return handlerDTO;
        }
        throw new IllegalArgumentException(
                "commitDefectVO is null: TriggerCommitHandlerConsumer can not get commitDefectVO!");
    }
}
