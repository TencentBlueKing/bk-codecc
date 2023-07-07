package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskLogOverviewDao;
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


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
                .tag("application", StringUtils.isNotBlank(applicationName) ? applicationName : "")
                .tag("createFrom", taskDetail != null && StringUtils.isNotBlank(taskDetail.getCreateFrom())
                        ? taskDetail.getCreateFrom() : "")
                .tags("success", ComConstants.ScanStatus.SUCCESS.getCode().equals(overview.getStatus())
                        ? "true" : "false")
                .register(registry)
                .increment(1);
    }

    private void registryScanTool(TaskLogEntity taskLog, TaskDetailVO taskDetail) {
        Counter.builder("scan_task_log_count")
                .tag("application", StringUtils.isNotBlank(applicationName) ? applicationName : "")
                .tags("toolName", taskLog.getToolName())
                .tag("createFrom", taskDetail != null && StringUtils.isNotBlank(taskDetail.getCreateFrom())
                        ? taskDetail.getCreateFrom() : "")
                .tags("success", taskLog.getFlag() == ComConstants.StepFlag.SUCC.value()
                        ? "true" : "false")
                .register(registry)
                .increment(1);
    }
}
