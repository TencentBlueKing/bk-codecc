package com.tencent.bk.codecc.codeccjob.resource;

import com.tencent.bk.codecc.codeccjob.api.UserBkMetricsRestResource;
import com.tencent.bk.codecc.codeccjob.service.BkMetricsService;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 度量接口实现
 *
 * @date 2022/5/26
 */

@RestResource
public class UserBkMetricsRestResourceImpl implements UserBkMetricsRestResource {

    @Autowired
    private BkMetricsService bkMetricsService;


    @Override
    public Result<Boolean> triggerBkMetrics(String statisticsTime, String projectId, Double repoCodeccAvgScore,
            Integer resolvedDefectNum, String userId) {
        return new Result<>(bkMetricsService
                .triggerBkMetricsDaily(statisticsTime, projectId, repoCodeccAvgScore, resolvedDefectNum));
    }
}
