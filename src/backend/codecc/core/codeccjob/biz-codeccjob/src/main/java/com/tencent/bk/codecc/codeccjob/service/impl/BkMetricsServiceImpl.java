package com.tencent.bk.codecc.codeccjob.service.impl;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_BK_METRICS_DAILY_FANOUT;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.codeccjob.pojo.BkMetricsMessage;
import com.tencent.bk.codecc.codeccjob.service.BkMetricsService;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import com.tencent.devops.common.util.ThreadUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 蓝盾度量服务实现类
 *
 * @date 2022/5/26
 */
@Slf4j
@Service
public class BkMetricsServiceImpl implements BkMetricsService {

    private static final int FIXED_STATUS =
            ComConstants.DefectStatus.FIXED.value() | ComConstants.DefectStatus.NEW.value();
    @Autowired
    @Qualifier("devopsRabbitTemplate")
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Client client;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private MetricsDao metricsDao;

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
            BkMetricsMessage bkMetricsMessage =
                    new BkMetricsMessage(statisticsTime, projectId, repoCodeccAvgScore, resolvedDefectNum);
            this.convertAndSendMessage(bkMetricsMessage);
        }
        return true;
    }

    /**
     * 推送消息
     *
     * @param bkMetricsMessage msg
     */
    private void convertAndSendMessage(BkMetricsMessage bkMetricsMessage) {
        log.info("BkMetricsMessage: {}", bkMetricsMessage);

        rabbitTemplate.convertAndSend(EXCHANGE_BK_METRICS_DAILY_FANOUT, "", JsonUtil.INSTANCE.toJson(bkMetricsMessage),
                message -> {
                    MessageProperties messageProperties = message.getMessageProperties();
                    messageProperties.setHeader("contentType", "application/json");
                    messageProperties.setHeader("contentEncoding", "UTF-8");
                    messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                });
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
        long startTIme = DateTimeUtils.getTimeStampStart(statisticsDate);
        long endTime = DateTimeUtils.getTimeStampEnd(statisticsDate);

        // 统计任务范围
        Set<String> createFrom = Sets.newHashSet(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());

        int dataSize = 0;
        int pageNum = 1;
        int pageSize = 2000;
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
                try {
                    BkMetricsMessage bkMetricsMessage =
                            this.statTaskByProjectId(projectId, statisticsDate, startTIme, endTime);
                    if (bkMetricsMessage != null) {
                        this.convertAndSendMessage(bkMetricsMessage);
                        continue;
                    }
                    log.warn("query statistic result is null! projectId: {}", projectId);
                } catch (Throwable t) {
                    log.error("statTaskByProjectId error, project id: {}", projectId, t);
                }
            }

            pageNum++;
        } while (dataSize >= pageSize);

        log.info("BkMetricsServiceImpl statistic finish");
    }

    /**
     * 统计每个projectId项目的上报数据
     */
    @Nullable
    private BkMetricsMessage statTaskByProjectId(String projectId, String statisticsDate, long startTime,
            long endTime) {
        // 修复缺陷数
        int fixedDefectCount = 0;
        // 指标总分
        double repoCodeccTotalScore = 0.0;
        int taskTotalCount = 0;

        int dataSize = 0;
        int pageNum = 1;
        int pageSize = 100;
        // 定义重试次数
        int retryCount = 5;
        do {
            log.info("start project id:{}, pageNum:{}, pageSize:{}", projectId, pageNum, pageSize);
            // 根据项目id获取任务id
            Result<List<Long>> result =
                    client.get(ServiceTaskRestResource.class).queryTaskIdPageByProjectId(projectId, pageNum, pageSize);
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
                return null;
            }
            retryCount = 5;

            List<Long> taskIdList = result.getData();
            if (CollectionUtils.isEmpty(taskIdList)) {
                log.info("taskIdList is empty!");
                return null;
            }
            dataSize = taskIdList.size();

            long beginTime = System.currentTimeMillis();
            // 累计修复数
            fixedDefectCount += this.statLintDefectByTaskId(taskIdList, startTime, endTime);
            final long costMillis = System.currentTimeMillis() - beginTime;

            // 累计指标分数，任务有分数才纳入计算
            double[] resArr = this.sumCurrentRdIndicatorsScore(taskIdList);
            repoCodeccTotalScore += resArr[0];
            taskTotalCount += resArr[1];
            pageNum++;

            if (costMillis > TimeUnit.SECONDS.toMillis(2)) {
                ThreadUtils.sleep(TimeUnit.SECONDS.toMillis(1));
                log.info("statLintDefectByTaskId cost: {}, task size: {}, detail: {}",
                        costMillis, dataSize, taskIdList);
            } else {
                ThreadUtils.sleep(TimeUnit.MILLISECONDS.toMillis(200));
            }
        } while (dataSize >= pageSize);

        // 计算项目维度指标平均分
        double repoCodeccAvgScore = 0d;
        if (taskTotalCount != 0 && repoCodeccTotalScore != 0) {
            repoCodeccAvgScore = new BigDecimal(repoCodeccTotalScore)
                    .divide(new BigDecimal(taskTotalCount), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return new BkMetricsMessage(statisticsDate, projectId, repoCodeccAvgScore, fixedDefectCount);
    }

    /**
     * 统计已修复规范问题数
     */
    private Long statLintDefectByTaskId(List<Long> taskIds, long startTime, long endTime) {
        return lintDefectV2Dao.countLintDefectByStatus(taskIds, null, FIXED_STATUS, "fixed_time", startTime, endTime);
    }

    /**
     * 统计总分
     *
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
        List<MetricsEntity> metricsEntities = metricsDao.findByTaskIdAndBuildId(taskLatestBuildIdMap);

        double sum = metricsEntities.stream().mapToDouble(MetricsEntity::getRdIndicatorsScore).sum();
        return new double[]{sum, metricsEntities.size()};
    }
}
