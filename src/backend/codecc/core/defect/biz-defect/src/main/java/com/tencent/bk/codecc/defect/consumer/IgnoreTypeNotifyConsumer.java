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

package com.tencent.bk.codecc.defect.consumer;


import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeNotifyTriggerModel;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CCN告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("ignoreTypeNotifyConsumer")
@Slf4j
public class IgnoreTypeNotifyConsumer {

    @Autowired
    private IIgnoreTypeService iIgnoreTypeService;

    public void consumer(IgnoreTypeNotifyTriggerModel triggerModel) {
        log.info("Ignore Type Notify Trigger Start. Model:{}", JSONObject.toJSONString(triggerModel));
        try {
            iIgnoreTypeService.triggerProjectStatisticAndSend(triggerModel.getProjectId(), triggerModel.getName(),
                    triggerModel.getIgnoreTypeId(), triggerModel.getCreateFrom());
        } catch (Throwable e) {
            log.error("Ignore Type Notify Trigger Fail. Model:" + JSONObject.toJSONString(triggerModel), e);
        }
        log.info("Ignore Type Notify Trigger End. Model:{}", JSONObject.toJSONString(triggerModel));
    }


}
