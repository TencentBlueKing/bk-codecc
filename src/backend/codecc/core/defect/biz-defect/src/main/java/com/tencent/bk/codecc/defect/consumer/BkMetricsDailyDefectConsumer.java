package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dto.BkMetricsDailyTriggerDTO;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.ThreadUtils;
import com.tencent.devops.metrics.api.ServiceMetricsDataReportResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.tencent.devops.metrics.pojo.dto.CodeccDataReportDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BkMetricsDailyDefectConsumer {

    private static final int FIXED_STATUS =
            ComConstants.DefectStatus.FIXED.value() | ComConstants.DefectStatus.NEW.value();

    @Autowired
    private Client client;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private TaskLogDao taskLogDao;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private MetricsDao metricsDao;

    /**
     * 蓝盾度量统计按项目缺陷统计推送
     * @param bkMetricsDailyTriggerDTO 按项目统计触发实体类
     */
    public void consumer(BkMetricsDailyTriggerDTO bkMetricsDailyTriggerDTO) {
        try {
            String projectId = bkMetricsDailyTriggerDTO.getProjectId();
            String statisticsDate = bkMetricsDailyTriggerDTO.getStatisticsDate();
            log.info("BkMetricsDailyDefectConsumer begin, statistic_date: {}, project_id:  {}",
                    statisticsDate, projectId);

            // 修复缺陷数
            int fixedDefectCount = 0;
            // 指标总分
            double repoCodeccTotalScore = 0.0;
            int taskTotalCount = 0;
            int dataSize = 0;
            int pageNum = 1;
            int pageSize = 100;
            // 统计时间戳范围
            long startTime = DateTimeUtils.getTimeStampStart(statisticsDate);
            long endTime = DateTimeUtils.getTimeStampEnd(statisticsDate);
            // 定义重试次数
            int retryCount = 5;
            do {
                log.info("start project id:{}, pageNum:{}, pageSize:{}", projectId, pageNum, pageSize);
                // 根据项目id获取任务id
                Result<List<Long>> result = client.get(ServiceTaskRestResource.class)
                        .queryTaskIdPageByProjectId(projectId, pageNum, pageSize);

                if (result.isNotOk() || result.getData() == null) {
                    if (retryCount > 0) {
                        retryCount--;
                        log.warn("query task id page failed! retrying...");
                        try {
                            log.info("waiting 10s");
                            Thread.sleep(10000L);
                        } catch (InterruptedException e) {
                            log.error("thread sleep fail!", e);
                        }
                        continue;
                    }
                    // 获取不到任务id 终止执行
                    log.error("query task id page failed!");
                    break;
                }
                retryCount = 5;

                List<Long> taskIdList = result.getData();
                if (CollectionUtils.isEmpty(taskIdList)) {
                    log.info("taskIdList is empty!");
                    break;
                }
                dataSize = taskIdList.size();

                // 查询taskLog预先过滤掉当天没有扫描的task, 减少计算已修复数
                List<Long> activeTaskIds = taskLogDao.findActiveTaskIdsInTimeRange(taskIdList, startTime, endTime);
                // 按照活跃任务逐条处理，统计每个taskId的修复缺陷的同时并统计指标总分
                for (Long taskId : activeTaskIds) {
                    // t_lint_defect_v2属于慢查询，一个个统计减少压力
                    fixedDefectCount += this.statLintDefectByTaskId(taskId, startTime, endTime);
                    // 累计指标分数，任务有分数才纳入计算
                    double[] taskSourceArr = this.sumCurrentRdIndicatorsScore(Lists.newArrayList(taskId));
                    repoCodeccTotalScore += taskSourceArr[0];
                    taskTotalCount += (int) taskSourceArr[1];
                }

                // 批量快速计算出来剩余任务指标总分
                List<Long> remainTasks = Lists.newArrayList(taskIdList);
                remainTasks.removeAll(activeTaskIds);
                double[] remainTaskSourceArr = this.sumCurrentRdIndicatorsScore(remainTasks);
                repoCodeccTotalScore += remainTaskSourceArr[0];
                taskTotalCount += (int) remainTaskSourceArr[1];

                pageNum++;

            } while (dataSize >= pageSize);

            // 计算项目维度指标平均分
            double repoCodeCCAvgScore = calculateAverageScore(repoCodeccTotalScore, taskTotalCount);

            // 推送消息上报Metrics
            uploadReportMetrics(statisticsDate, projectId, repoCodeCCAvgScore, fixedDefectCount);
        } catch (Throwable e) {
            log.error("BkMetricsDailyDefectConsumer error, obj mq: {}", bkMetricsDailyTriggerDTO, e);
        }
    }

    /**
     * 动态流量控制
     * @param costMillis 耗时
     * @param taskId 耗时任务ID
     */
    private void throttleProcessing(long costMillis, Long taskId) {
        final long threshold = TimeUnit.SECONDS.toMillis(2);
        if (costMillis > threshold) {
            // 计算超出的时间比例，按比例增加休眠时间
            long excessTime = costMillis - threshold;
            long sleepTime = threshold + (long)(excessTime * 0.5); // 按超出时间的50%增加
            ThreadUtils.sleep(sleepTime);
            log.info("statLintDefectByTaskId cost: {}, taskId: {}", costMillis, taskId);
        }
    }

    /**
     * 计算项目维度指标平均分
     * @param repoCodeCCTotalScore 总分数
     * @param taskTotalCount 任务条数
     * @return 平均分
     */
    private double calculateAverageScore(double repoCodeCCTotalScore, int taskTotalCount) {
        if (taskTotalCount != 0 && repoCodeCCTotalScore != 0) {
            return BigDecimal.valueOf(repoCodeCCTotalScore)
                    .divide(BigDecimal.valueOf(taskTotalCount), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return 0d;
    }

    /**
     * 给蓝盾上报数据
     * @param date 日期
     * @param projectId 项目id
     * @param repoCodeCCAvgScore 分数
     * @param fixedDefectCount 已修复数
     */
    private void uploadReportMetrics(String date, String projectId, double repoCodeCCAvgScore, int fixedDefectCount) {
        CodeccDataReportDTO codeccDataReportDTO =
                new CodeccDataReportDTO(date, projectId, repoCodeCCAvgScore, fixedDefectCount);
        log.info("BkMetricsDailyDefectConsumer CodeCCDataReportDTO: {}", codeccDataReportDTO);
        client.getDevopsService(ServiceMetricsDataReportResource.class, projectId)
                .metricsCodeccDataReport(codeccDataReportDTO);
    }

    /**
     * 统计已修复规范问题数, 慢查询实时监控性能，动态控制流量
     */
    private Long statLintDefectByTaskId(Long taskId, long startTime, long endTime) {
        long beginTime = System.currentTimeMillis();
        Long fixedCount =
                lintDefectV2Dao.countLintDefectByStatus(taskId, null, FIXED_STATUS, "fixed_time", startTime, endTime);
        final long costMillis = System.currentTimeMillis() - beginTime;
        // 动态流量控制
        throttleProcessing(costMillis, taskId);
        return fixedCount;
    }

    /**
     * 统计总分
     * @param taskIds 任务id清单
     * @return score
     */
    @NotNull
    private double[] sumCurrentRdIndicatorsScore(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return new double[]{0.0, 0};
        }

        // 拿到最新build id
        List<ToolBuildInfoEntity> buildInfoEntityList = toolBuildInfoDao.findLatestBuildIdByTaskIdSet(taskIds);
        Map<Long, String> taskLatestBuildIdMap = buildInfoEntityList.stream()
                .filter(it -> StringUtils.isNotEmpty(it.getDefectBaseBuildId()))
                .collect(Collectors.toMap(ToolBuildInfoEntity::getTaskId,
                        ToolBuildInfoEntity::getDefectBaseBuildId, (k, v) -> v));
        if (taskLatestBuildIdMap.isEmpty()) {
            return new double[]{0.0, 0};
        }

        // 根据上面查询到的任务id，构建id，获取度量分数
        List<MetricsEntity> metricsEntities = metricsDao.findScoreByTaskIdAndBuildId(taskLatestBuildIdMap);

        double sum = metricsEntities.stream().mapToDouble(MetricsEntity::getRdIndicatorsScore).sum();
        return new double[]{sum, metricsEntities.size()};
    }
}
