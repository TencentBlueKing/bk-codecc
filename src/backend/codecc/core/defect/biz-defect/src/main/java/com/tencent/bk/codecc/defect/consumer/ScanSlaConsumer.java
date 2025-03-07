package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.TaskLogOverviewDao;
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.bk.codecc.defect.utils.TaskLogInfoUtils;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component("scanSlaConsumer")
public class ScanSlaConsumer {

    @Autowired
    private MeterRegistry registry;
    @Autowired
    private TaskLogOverviewDao taskLogOverviewDao;
    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private Client client;

    @Value("${spring.application.name:#{null}}")
    private String applicationName;

    /**
     * 消费与记录扫描完成事件
     *
     * @param scanTaskTrigger
     */
    public void consumer(ScanTaskTriggerDTO scanTaskTrigger) {
        try {
            Long taskId = scanTaskTrigger.getTaskId();
            String buildId = scanTaskTrigger.getBuildId();
            log.info("start to registry sla info taskId:{} buildId:{}", taskId, buildId);
            TaskLogOverviewEntity overview = taskLogOverviewDao.findOneByTaskIdAndBuildId(taskId, buildId);
            if (overview == null) {
                log.error("TaskId:{} BuildId:{} ScanSlaConsumer overview is Empty", taskId, buildId);
                return;
            }
            Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            TaskDetailVO taskDetail = null;
            if (result.isOk() && result.getData() != null) {
                taskDetail = result.getData();
            }
            registryScanOverview(overview, taskDetail);
            List<TaskLogEntity> taskLogs = taskLogRepository.findByTaskIdAndBuildId(taskId, buildId);
            if (CollectionUtils.isEmpty(taskLogs)) {
                log.error("TaskId:{} BuildId:{} ScanSlaConsumer taskLog is Empty", taskId, buildId);
                return;
            }
            for (TaskLogEntity taskLog : taskLogs) {
                registryScanTool(taskLog, taskDetail);
            }
        } catch (Throwable e) {
            log.error("ScanSlaConsumer error, task id: {}, build id: {}", scanTaskTrigger.getTaskId(),
                    scanTaskTrigger.getBuildId(), e);
        }
    }


    private void registryScanOverview(TaskLogOverviewEntity overview, TaskDetailVO taskDetail) {
        if (!ComConstants.ScanStatus.SUCCESS.getCode().equals(overview.getStatus())) {
            log.error("registryScanOverview scan fail taskId:{}", overview.getTaskId());
        }
        Counter.builder("scan_task_log_overview_count")
                .tag("application",
                        StringUtils.isNotBlank(applicationName) ? applicationName : ComConstants.EMPTY_STRING)
                .tag("projectPrefix", taskDetail != null && StringUtils.isNotBlank(taskDetail.getProjectId())
                        ? getProjectPrefix(taskDetail.getProjectId()) : ComConstants.EMPTY_STRING)
                .tag("errorCode", overview.getPluginErrorCode() != null ? overview.getPluginErrorCode().toString()
                        : ComConstants.EMPTY_STRING)
                .tag("errorType", overview.getPluginErrorType() != null ? overview.getPluginErrorType().toString()
                        : ComConstants.EMPTY_STRING)
                .tag("createFrom", taskDetail != null && StringUtils.isNotBlank(taskDetail.getCreateFrom())
                        ? taskDetail.getCreateFrom() : ComConstants.EMPTY_STRING)
                .tags("success", ComConstants.ScanStatus.SUCCESS.getCode().equals(overview.getStatus())
                        ? "true" : "false")
                .register(registry)
                .increment(1);
    }

    private void registryScanTool(TaskLogEntity taskLog, TaskDetailVO taskDetail) {
        long cost = 0L;
        if (taskLog.getStartTime() != 0L && taskLog.getEndTime() != 0L) {
            cost = taskLog.getEndTime() - taskLog.getStartTime();
        }
        // 次数
        Counter.builder("scan_task_log_count")
                .tag("application",
                        StringUtils.isNotBlank(applicationName) ? applicationName : ComConstants.EMPTY_STRING)
                .tag("projectPrefix", taskDetail != null && StringUtils.isNotBlank(taskDetail.getProjectId())
                        ? getProjectPrefix(taskDetail.getProjectId()) : ComConstants.EMPTY_STRING)
                .tag("toolName", taskLog.getToolName())
                .tag("createFrom", taskDetail != null && StringUtils.isNotBlank(taskDetail.getCreateFrom())
                        ? taskDetail.getCreateFrom() : ComConstants.EMPTY_STRING)
                .tag("fastIncScan", String.valueOf(TaskLogInfoUtils.INSTANCE.isFastIncScan(taskLog)))
                .tag("success", taskLog.getFlag() == ComConstants.StepFlag.SUCC.value()
                        ? "true" : "false")
                .tag("noDuration", Boolean.toString(cost == 0L))
                .register(registry)
                .increment(1);
        // 仅收集运行时间大于0的
        if (cost > 0) {
            // 工具扫描时长收集
            Timer.builder("scan_task_log_time")
                    .tag("application",
                            StringUtils.isNotBlank(applicationName) ? applicationName : ComConstants.EMPTY_STRING)
                    .tag("projectPrefix", taskDetail != null && StringUtils.isNotBlank(taskDetail.getProjectId())
                            ? getProjectPrefix(taskDetail.getProjectId()) : ComConstants.EMPTY_STRING)
                    .tag("toolName", taskLog.getToolName())
                    // 区分是否为超快增量，统计是可以排除超快增量
                    .tag("fastIncScan", String.valueOf(TaskLogInfoUtils.INSTANCE.isFastIncScan(taskLog)))
                    .tag("createFrom", taskDetail != null && StringUtils.isNotBlank(taskDetail.getCreateFrom())
                            ? taskDetail.getCreateFrom() : ComConstants.EMPTY_STRING)
                    .tag("success", taskLog.getFlag() == ComConstants.StepFlag.SUCC.value()
                            ? "true" : "false")
                    .register(registry)
                    .record(cost, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 项目前缀，用于监控时排除或者关注一些特殊的前缀任务
     *
     * @param projectId
     * @return
     */
    private String getProjectPrefix(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return ComConstants.EMPTY_STRING;
        } else if (projectId.startsWith(ComConstants.CUSTOMPROJ_ID_PREFIX)) {
            return ComConstants.CUSTOMPROJ_ID_PREFIX;
        } else if (projectId.startsWith(ComConstants.GONGFENG_PRIVATYE_PROJECT_PREFIX)) {
            return ComConstants.GONGFENG_PRIVATYE_PROJECT_PREFIX;
        } else if (projectId.startsWith(ComConstants.GONGFENG_PROJECT_ID_PREFIX)) {
            return ComConstants.GONGFENG_PROJECT_ID_PREFIX;
        } else if (projectId.startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            return ComConstants.GRAY_PROJECT_PREFIX;
        } else {
            return ComConstants.EMPTY_STRING;
        }
    }
}
