package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.MetricsVO;

import java.util.List;

public interface MetricsService {
    MetricsVO getMetrics(Long taskId, String buildId);

    List<MetricsVO> getMetrics(List<Long> taskIds);
}
