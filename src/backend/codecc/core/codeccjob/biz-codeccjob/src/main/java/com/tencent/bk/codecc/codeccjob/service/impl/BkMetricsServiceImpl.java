package com.tencent.bk.codecc.codeccjob.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.service.BkMetricsService;
import com.tencent.bk.codecc.defect.dto.BkMetricsDailyTriggerDTO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import java.util.List;
import java.util.Set;

import com.tencent.devops.common.util.ThreadUtils;
import com.tencent.devops.metrics.api.ServiceMetricsDataReportResource;
import com.tencent.devops.metrics.pojo.dto.CodeccDataReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_BK_METRICS_DAILY_DEFECT_STATISTIC;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_BK_METRICS_DAILY_DEFECT_STATISTIC;

/**
 * 蓝盾度量服务实现类
 *
 * @date 2022/5/26
 */
@Slf4j
@Service
public class BkMetricsServiceImpl implements BkMetricsService {

    @Autowired
    private Client client;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 触发蓝盾度量统计
     *
     * @param statisticsTime 统计日期，格式yyyy-MM-dd
     * @param projectId 项目ID
     * @param repoCodeccAvgScore 代码库扫描平均分，精确二位小数
     * @param resolvedDefectNum 已解决缺陷数量
     * @return boolean
     */
    @Override
    public Boolean triggerBkMetricsDaily(String statisticsTime, String projectId, Double repoCodeccAvgScore,
                                         Integer resolvedDefectNum) {
        // 手动触发支持统计指定日期
        if (StringUtils.isBlank(projectId)) {
            try {
                ThreadPoolUtil.addRunnableTask(() -> statistic(statisticsTime));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return false;
            }
        } else {
            CodeccDataReportDTO codeccDataReportDTO =
                    new CodeccDataReportDTO(statisticsTime, projectId, repoCodeccAvgScore, resolvedDefectNum);
            this.uploadMetricsData(codeccDataReportDTO);
        }
        return true;
    }

    /**
     * 推送消息
     * 2025-04-23: 修改为调接口上报Metrics
     *
     * @param codeccDataReportDTO msg
     */
    private void uploadMetricsData(CodeccDataReportDTO codeccDataReportDTO) {
        log.info("BkMetricsServiceImpl CodeCCDataReportDTO: {}", codeccDataReportDTO);
        client.getDevopsService(ServiceMetricsDataReportResource.class, codeccDataReportDTO.getProjectId())
                .metricsCodeccDataReport(codeccDataReportDTO);
    }

    /**
     * 统计入口
     */
    @Override
    public void statistic(String statisticsDate) {
        log.info("BkMetricsServiceImpl statistic begin, param obj: {}", statisticsDate);
        // 生成统计时间戳
        if (StringUtils.isBlank(statisticsDate)) {
            statisticsDate = DateTimeUtils.getDateByDiff(-1);
        }

        // 统计任务范围
        Set<String> createFrom = Sets.newHashSet(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());

        int dataSize = 0;
        int pageNum = 1;
        int pageSize = ComConstants.SMALL_PAGE_SIZE;
        // 定义重试次数
        int retryCount = 5;
        do {
            log.info("start pageNum:{}, pageSize:{}", pageNum, pageSize);
            // 获取项目id
            Result<List<String>> result =
                    client.get(ServiceTaskRestResource.class).queryProjectIdPage(createFrom, pageNum, pageSize);
            if (result.isNotOk() || result.getData() == null) {
                if (retryCount > 0) {
                    retryCount--;
                    log.warn("query task id page failed! retrying...");
                    try {
                        log.info("waiting 5s");
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        log.error("thread sleep fail!", e);
                    }
                    continue;
                }
                // 获取不到任务id 终止执行
                log.error("query project id page failed!");
                return;
            }
            retryCount = 5;

            List<String> projectIdList = result.getData();
            if (CollectionUtils.isEmpty(projectIdList)) {
                log.info("projectIdList is empty!");
                return;
            }
            dataSize = projectIdList.size();

            for (String projectId : projectIdList) {
                BkMetricsDailyTriggerDTO bkMetricsDailyTriggerDTO =
                        new BkMetricsDailyTriggerDTO(projectId, statisticsDate);
                rabbitTemplate.convertAndSend(EXCHANGE_BK_METRICS_DAILY_DEFECT_STATISTIC,
                        ROUTE_BK_METRICS_DAILY_DEFECT_STATISTIC, bkMetricsDailyTriggerDTO);
            }

            pageNum++;

            // 每处理完一页数据(100个project_id)后等待1秒，避免给MQ造成瞬时压力
            ThreadUtils.sleep(1000);

        } while (dataSize >= pageSize);

        log.info("BkMetricsServiceImpl statistic finish");
    }
}
