package com.tencent.bk.codecc.codeccjob.service;

/**
 * 蓝盾度量服务接口
 *
 * @date 2022/5/26
 */
public interface BkMetricsService {

    Boolean triggerBkMetricsDaily(String statisticsTime, String projectId, Double repoCodeccAvgScore,
            Integer resolvedDefectNum);

    void statistic(String statisticsTime);
}
