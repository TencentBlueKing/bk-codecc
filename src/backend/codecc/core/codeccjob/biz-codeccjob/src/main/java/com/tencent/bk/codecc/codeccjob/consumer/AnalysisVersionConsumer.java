/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.codeccjob.consumer;

import com.tencent.bk.codecc.defect.dto.AnalysisVersionDTO;
import com.tencent.bk.codecc.codeccjob.service.AnalysisVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 分析版本队列消费逻辑
 *
 * @version V1.0
 * @date 2019/7/16
 */
@Component
public class AnalysisVersionConsumer
{

    private static Logger logger = LoggerFactory.getLogger(AnalysisVersionConsumer.class);

    @Autowired
    private AnalysisVersionService analysisVersionService;


    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ANALYSIS_VERSION,
            value = @Queue(value = QUEUE_ANALYSIS_VERSION, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_ANALYSIS_VERSION, durable = "true", delayed = "true", type = "topic")))
    public void saveAnalysisVersion(AnalysisVersionDTO analysisVersionDTO)
    {
        logger.info("begin to save analysis version");
        try
        {
            analysisVersionService.saveAnalysisVersion(analysisVersionDTO);
        }
        catch (Exception e)
        {
            logger.error("save analysis version error! task id: {}, tool name: {}",
                    analysisVersionDTO.getTaskId(), analysisVersionDTO.getToolName());
        }

    }

}
