package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_WARM_UP;

import com.tencent.bk.codecc.defect.service.HotColdDataSeparationService;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.GetTaskStatusAndCreateFromResponse;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.TaskStatus;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HotColdDataSeparationServiceImpl implements HotColdDataSeparationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private Client client;

    @Override
    public boolean warmUpColdDataIfNecessary(Long taskId) {
        // taskId 为空，跳过
        if (taskId == null) {
            return false;
        }
        GetTaskStatusAndCreateFromResponse taskResp =
                client.get(ServiceTaskRestResource.class).getTaskStatusAndCreateFrom(taskId).getData();

        if (taskResp != null && TaskStatus.COLD.value() == taskResp.getStatus()
                && !BsTaskCreateFrom.GONGFENG_SCAN.value().equals(taskResp.getCreateFrom())) {
            // 并发高时允许少量重复，不加分布式锁，消费端有兜底防重
            String redisKey = String.format("WARM_UP_COLD_DATA_BEFORE_DEFECT_COMMIT:%d", taskId);
            String redisValue = redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isEmpty(redisValue)) {
                rabbitTemplate.convertAndSend(EXCHANGE_DATA_SEPARATION, ROUTE_DATA_SEPARATION_WARM_UP, taskId);
                redisTemplate.opsForValue().set(redisKey, "1", 30, TimeUnit.MINUTES);
                log.info("task is cold, ready to warm up, task id: {}", taskId);
            }

            return true;
        }

        return false;
    }
}
