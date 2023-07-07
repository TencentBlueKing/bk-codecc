package com.tencent.bk.codecc.defect.component;

import com.tencent.bk.codecc.quartz.pojo.JobExternalDto;
import com.tencent.bk.codecc.quartz.pojo.OperationType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_EXTERNAL_JOB;

@Component
public class ScheduleJobComponent {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${codecc.classurl:#{null}}")
    private String publicClassUrl;


    public String addJob(String className, String cronExpression, Map<String, Object> jobCustomParam) {
        String jobName = generateUniqueJobName();
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.ADD);
        return jobName;
    }

    public void addJob(String jobName, String className, String cronExpression, Map<String, Object> jobCustomParam) {
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.ADD);
    }

    public void removeJob(String jobName, String className, String cronExpression, Map<String, Object> jobCustomParam) {
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.REMOVE);
    }

    public void rescheduleJob(String jobName, String className, String cronExpression,
            Map<String, Object> jobCustomParam) {
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.RESCHEDULE);
    }

    public void parseJob(String jobName, String className, String cronExpression, Map<String, Object> jobCustomParam) {
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.PARSE);
    }

    public void resumeJob(String jobName, String className, String cronExpression, Map<String, Object> jobCustomParam) {
        opsJob(jobName, className, cronExpression, jobCustomParam, OperationType.RESUME);
    }


    public void opsJob(String jobName, String className, String cronExpression, Map<String, Object> jobCustomParam,
            OperationType operationType) {
        opsJob(jobName, className, String.format("%s%s.java", publicClassUrl, className), cronExpression,
                jobCustomParam, operationType);
    }


    public void opsJob(String jobName, String className, String classUrl, String cronExpression,
            Map<String, Object> jobCustomParam, OperationType operationType) {
        JobExternalDto jobExternalDto =
                new JobExternalDto(jobName, classUrl, className, cronExpression, jobCustomParam, operationType);
        rabbitTemplate.convertAndSend(EXCHANGE_EXTERNAL_JOB, "", jobExternalDto);
    }

    public String generateUniqueJobName() {
        return UUID.randomUUID() + "_" + UUID.randomUUID();
    }
}
