package com.tencent.bk.codecc.quartz.job;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.devops.common.constant.RedisKeyConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLEAN_MONGO_DATA;

/**
 * 定时清理mongo数据，涉及 defect 和 schedule 服务：
 * t_file_index
 *
 * t_task_log
 * t_task_log_overview
 * t_dupc_statistic
 * t_ccn_statistic
 * t_lint_statistic
 * t_cloc_statistic
 * t_statistic
 * t_tool_build_stack
 * t_tool_build_info
 * t_build_defect
 * t_build
 *
 */
public class CleanMongoDataTask implements IScheduleTask {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        // 清理前先重置 CLEAN_DATA_TASK_LIST
        redisTemplate.opsForValue().set(RedisKeyConstants.CLEAN_DATA_TASK_LIST, "0");
        rabbitTemplate.convertAndSend(EXCHANGE_CLEAN_MONGO_DATA, "", "");
    }
}
