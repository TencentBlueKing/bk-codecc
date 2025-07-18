/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.quartz.job;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.devops.common.constant.ComConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODECC_IGNORE_APPROVAL_TIMING_CHECK;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_IGNORE_APPROVAL_TIMING_CHECK;

/**
 * 忽略审批定时检查单据状态
 */
public class IgnoreApprovalTimingCheckTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final Logger logger = LoggerFactory.getLogger(IgnoreApprovalTimingCheckTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        try {
            logger.info("IgnoreApprovalTimingCheckTask start");
            rabbitTemplate.convertAndSend(EXCHANGE_CODECC_IGNORE_APPROVAL_TIMING_CHECK,
                    ROUTE_CODECC_IGNORE_APPROVAL_TIMING_CHECK, ComConstants.EMPTY_STRING);
            logger.info("IgnoreApprovalTimingCheckTask end");
        } catch (Exception e) {
            logger.info("IgnoreApprovalTimingCheckTask cause error", e);
        }
    }
}
