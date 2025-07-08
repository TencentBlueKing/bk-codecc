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

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.defect.api.ServiceIgnoreTypeRestResource;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeNotifyTriggerModel;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeNotifyVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Calendar;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_IGNORE_TYPE_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_TYPE_NOTIFY;

/**
 * 规则告警统计定时任务
 *
 * @version V1.0
 * @date 2020/11/17
 */

public class IgnoreTypeNotifyTask implements IScheduleTask {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Client client;

    @Value("${service.tag:prod}")
    private String serviceTag;

    private static final Logger logger = LoggerFactory.getLogger(IgnoreTypeNotifyTask.class);

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        Map<String, Object> jobCustomParam = quartzJobContext.getJobCustomParam();
        if (null == jobCustomParam) {
            logger.error("IgnoreTypeNotifyTask job custom param is null");
            return;
        }
        IgnoreTypeNotifyTriggerModel triggerModel = new IgnoreTypeNotifyTriggerModel();
        triggerModel.setProjectId((String) jobCustomParam.get("projectId"));
        triggerModel.setName((String) jobCustomParam.get("name"));
        triggerModel.setIgnoreTypeId((Integer) jobCustomParam.get("ignoreTypeId"));
        triggerModel.setCreateFrom((String) jobCustomParam.get("createFrom"));

        try {
            Result<IgnoreTypeProjectConfigVO> result =
                    client.getWithSpecialTag(ServiceIgnoreTypeRestResource.class, serviceTag)
                            .detail(triggerModel.getProjectId(), "CodeCC", triggerModel.getIgnoreTypeId());

            if (result == null || result.isNotOk() || result.getData() == null) {
                logger.error(
                        "IgnoreTypeNotifyTask job param : {} get IgnoreTypeProjectConfigVO Fail Or Null. Result:{}",
                        JSONObject.toJSONString(triggerModel), JSONObject.toJSONString(result));
                return;
            }
            IgnoreTypeNotifyVO notify = result.getData().getNotify();
            if (notify == null) {
                logger.error("IgnoreTypeNotifyTask job param : {} get Notify Null.",
                        JSONObject.toJSONString(triggerModel));
                return;
            }
            Calendar calendar = Calendar.getInstance();
            int month = calendar.get(Calendar.MONTH) + 1;
            int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            dayOfWeek = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            logger.info("IgnoreTypeNotifyTask projectId: {}, name:{}, ignoreTypeId:{}, month:{}, weekOfMonth:{}, "
                            + "dayOfWeek:{}, notify:{}", triggerModel.getProjectId(), triggerModel.getName(),
                    triggerModel.getIgnoreTypeId(), month, weekOfMonth, dayOfWeek, JSONObject.toJSONString(notify));
            //检查月份
            if (!checkIfMonthMatch(month, notify) || !checkIfWeekMatch(weekOfMonth, notify)
                    || !checkIfDayMatch(dayOfWeek, notify)) {
                logger.info("IgnoreTypeNotifyTask projectId: {}, name:{}, ignoreTypeId:{} notify not match",
                        triggerModel.getProjectId(), triggerModel.getName(), triggerModel.getIgnoreTypeId());
                return;
            }
            rabbitTemplate.convertAndSend(EXCHANGE_IGNORE_TYPE_NOTIFY, ROUTE_IGNORE_TYPE_NOTIFY, triggerModel);
            logger.info("IgnoreTypeNotifyTask projectId: {}, name:{}, ignoreTypeId:{} notify success",
                    triggerModel.getProjectId(), triggerModel.getName(), triggerModel.getIgnoreTypeId());
        } catch (Exception e) {
            logger.error("IgnoreTypeNotifyTask projectId " + triggerModel.getProjectId() + ", name: "
                            + triggerModel.getName() + ", ignoreTypeId: " + triggerModel.getIgnoreTypeId()
                            + "notify cause error", e);
        }
    }

    private boolean checkIfMonthMatch(Integer month, IgnoreTypeNotifyVO notify) {
        if (notify.getEveryMonth() != null && notify.getEveryMonth()) {
            return true;
        }
        return CollectionUtils.isNotEmpty(notify.getNotifyMonths()) && notify.getNotifyMonths().contains(month);
    }

    private boolean checkIfWeekMatch(Integer weekOfMonth, IgnoreTypeNotifyVO notify) {
        if (notify.getEveryWeek() != null && notify.getEveryWeek()) {
            return true;
        }
        return CollectionUtils.isNotEmpty(notify.getNotifyWeekOfMonths())
                && notify.getNotifyWeekOfMonths().contains(weekOfMonth);
    }

    private boolean checkIfDayMatch(Integer dayOfWeek, IgnoreTypeNotifyVO notify) {
        return CollectionUtils.isNotEmpty(notify.getNotifyDayOfWeeks())
                && notify.getNotifyDayOfWeeks().contains(dayOfWeek);
    }
}
