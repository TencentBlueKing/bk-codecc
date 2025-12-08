package com.tencent.bk.codecc.quartz.job;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.devops.common.constant.ComConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ADMIN_PRIVILEGE_REFRESH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ADMIN_PRIVILEGE_REFRESH;

/**
 * 定时刷新管理员状态 启用-停用
 */
public class RefreshAdminPrivilegeStatusTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RefreshAdminPrivilegeStatusTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        logger.info("start to RefreshAdminPrivilegeStatusTask");
        rabbitTemplate.convertAndSend(
                EXCHANGE_ADMIN_PRIVILEGE_REFRESH,
                ROUTE_ADMIN_PRIVILEGE_REFRESH,
                ComConstants.EMPTY_STRING
        );
    }
}
