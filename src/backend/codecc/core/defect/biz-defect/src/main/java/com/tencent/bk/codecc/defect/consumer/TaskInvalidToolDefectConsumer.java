package com.tencent.bk.codecc.defect.consumer;

import static com.tencent.devops.common.constant.RedisKeyConstants.TASK_INVALID_TOOL_DEFECT;

import com.alibaba.fastjson2.JSONObject;
import com.tencent.bk.codecc.defect.service.TaskInvalidToolDefectService;
import com.tencent.bk.codecc.defect.vo.TaskInvalidToolDefectVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskInvalidToolDefectConsumer {

    @Autowired
    private TaskInvalidToolDefectService taskInvalidToolDefectService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void consumer(TaskInvalidToolDefectVO vo) {
        log.info("task invalid tool defect exclude consumer vo:{}", vo == null ? "null" : JSONObject.toJSONString(vo));
        if (vo == null || vo.getTaskId() == null || StringUtils.isBlank(vo.getCreateFrom())
                || StringUtils.isBlank(vo.getBuildId()) || StringUtils.isBlank(vo.getInvalidTool())
                || StringUtils.isBlank(vo.getToolType())) {
            return;
        }
        try {
            taskInvalidToolDefectService.excludeToolDefect(vo.getTaskId(), vo.getCreateFrom(),
                    vo.getBuildId(), vo.getInvalidTool(), vo.getToolType());
        } catch (Exception e) {
            log.error("task invalid tool defect exclude consumer cause error. taskId:{}, buildId:{}.", vo.getTaskId(),
                    vo.getBuildId(), e);
        } finally {
            reduceRedisToolCount(vo.getTaskId(), vo.getBuildId());
        }
    }

    private void reduceRedisToolCount(Long taskId, String buildId) {
        try {
            String redisKey = TASK_INVALID_TOOL_DEFECT + ":" + taskId + ":" + buildId;
            redisTemplate.opsForValue().decrement(redisKey);
        } catch (Exception e) {
            log.error("reduce invalid tool count cause error. taskId:{}, buildId:{}.", taskId, buildId, e);
        }
    }

}
