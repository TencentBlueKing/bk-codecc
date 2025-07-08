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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ACTIVE_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ACTIVE_STAT;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.devops.common.api.StatisticTaskCodeLineToolVO;
import com.tencent.devops.common.constant.ComConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 活跃统计计划任务
 *
 * @version V1.0
 * @date 2020/12/11
 */

public class ActiveStatisticScheduleTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static Logger logger = LoggerFactory.getLogger(ActiveStatisticScheduleTask.class);


    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> param = quartzJobContext.getJobCustomParam();
        logger.info("beginning execute ActiveStatistic task. param:{}", param);
        if (null == param) {
            logger.error("ActiveStatisticScheduleTask job custom param is null");
            return;
        }
        String createFrom = (String) param.get("createFrom");

        // 将字符串分割并转换为枚举列表
        List<ComConstants.DefectStatType> dataFromList = Arrays.stream(createFrom.split(ComConstants.COMMA))
                .map(String::trim)
                .map(ComConstants.DefectStatType::fromValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        StatisticTaskCodeLineToolVO statisticVO = new StatisticTaskCodeLineToolVO();
        statisticVO.setDataFromList(dataFromList);

        rabbitTemplate.convertAndSend(EXCHANGE_ACTIVE_STAT, ROUTE_ACTIVE_STAT, statisticVO);
    }
}
