/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.consumer;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_REFRESH_CHECKERSET_USAGE;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_REFRESH_CHECKERSET_USAGE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_REFRESH_CHECKERSET_USAGE;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerSetTaskRelationshipDao;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskCountEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 刷新规则集使用量消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class RefreshCheckerSetUsageConsumer {

    @Autowired
    private CheckerSetTaskRelationshipDao checkerSetTaskRelationshipDao;

    @Autowired
    private CheckerSetDao checkerSetDao;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @RabbitListener(
            concurrency = "1",
            bindings = @QueueBinding(
                    key = ROUTE_REFRESH_CHECKERSET_USAGE,
                    value = @Queue(value = QUEUE_REFRESH_CHECKERSET_USAGE, durable = "true"),
                    exchange = @Exchange(value = EXCHANGE_REFRESH_CHECKERSET_USAGE, durable = "true", delayed = "true")
            )
    )
    public void refreshCheckerSetUsage() {
        try {
            log.info("RefreshCheckerSetUsageConsumer begin");
            Map<String, Long> statisticsMap = getStatisticsMap();
            checkerSetDao.updateCheckerSetUsage(statisticsMap);
            log.info("RefreshCheckerSetUsageConsumer end");
        } catch (Throwable t) {
            log.error("RefreshCheckerSetUsageConsumer error", t);
        }
    }

    private Map<String, Long> getStatisticsMap() throws InterruptedException {
        Map<String, Long> statisticsMap = Maps.newHashMap();
        int page = 0;

        while (true) {
            // CheckerSetEntity中checkerSetId非唯一，存在多版本，应用层面幂等去重
            PageRequest pageRequest = PageRequest.of(page++, 500);
            List<CheckerSetEntity> checkerSetList = checkerSetRepository.findByCheckerSetIdIsNotNull(pageRequest);
            if (CollectionUtils.isEmpty(checkerSetList)) {
                break;
            }

            Set<String> checkerSetIds = checkerSetList.stream()
                    .filter(x -> !ObjectUtils.isEmpty(x.getCheckerSetId())
                            && !statisticsMap.containsKey(x.getCheckerSetId()))
                    .map(CheckerSetEntity::getCheckerSetId)
                    .collect(Collectors.toSet());
            List<CheckerSetTaskCountEntity> checkerSetTaskCountList =
                    checkerSetTaskRelationshipDao.countCheckerSetGroupByCheckerSetId(checkerSetIds);

            for (CheckerSetTaskCountEntity entity : checkerSetTaskCountList) {
                statisticsMap.put(entity.getCheckerSetId(), entity.getTaskInUseCount());
            }

            Thread.sleep(500);
        }

        return statisticsMap;
    }


}
