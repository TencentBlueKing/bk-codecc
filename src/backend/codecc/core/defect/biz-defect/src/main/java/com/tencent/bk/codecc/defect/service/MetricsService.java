package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.vo.MetricsVO;

import java.util.List;

public interface MetricsService {
    MetricsVO getMetrics(Long taskId, String buildId);

    List<MetricsVO> getMetrics(List<Long> taskIds);

    MetricsVO getLatestMetrics(Long taskId);

    /**
     * 更新 entity. 对于一个新的 entity, 如果其 buildId 和 taskId 在表中已存在, 则用新的 entity 替换旧的 entity; 不存在则新增.
     * @param entity
     * @return 操作是否成功
     */
    Boolean updateMetricsByTaskIdAndBuildId(MetricsEntity entity);
}
