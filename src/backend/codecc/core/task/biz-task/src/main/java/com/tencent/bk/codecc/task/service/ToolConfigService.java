package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;

import java.util.List;

public interface ToolConfigService {
    public List<ToolConfigInfoVO> getToolConfigByTaskId(long taskId);
}
