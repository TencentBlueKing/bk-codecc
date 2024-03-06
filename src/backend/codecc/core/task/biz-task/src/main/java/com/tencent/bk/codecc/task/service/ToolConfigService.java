package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;

import java.util.List;

public interface ToolConfigService {
    List<ToolConfigInfoVO> getToolConfigByTaskId(long taskId);

    List<ToolConfigInfoVO> getToolConfigByTaskIdAndToolName(List<Long> taskIds, String toolName);

    List<ToolConfigInfoVO> getToolConfigByTaskIdIn(List<Long> taskId);
}
