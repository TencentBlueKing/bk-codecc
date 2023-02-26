package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceMetricsRestResource;
import com.tencent.bk.codecc.defect.service.MetricsService;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestResource
public class ServiceMetricsRestResourceImpl implements ServiceMetricsRestResource {
    @Autowired
    private MetricsService metricsService;

    @Override
    public Result<MetricsVO> getMetrics(Long taskId, String buildId) {
        return new Result<>(metricsService.getMetrics(taskId, buildId));
    }

    @Override
    public Result<List<MetricsVO>> getMetrics(List<Long> taskIds) {
        return new Result<>(metricsService.getMetrics(taskIds));
    }
}
