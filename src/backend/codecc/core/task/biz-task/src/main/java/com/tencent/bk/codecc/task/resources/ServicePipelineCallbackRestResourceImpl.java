package com.tencent.bk.codecc.task.resources;

import com.alibaba.fastjson2.JSONObject;
import com.tencent.bk.codecc.task.api.ServicePipelineCallbackRestResource;
import com.tencent.bk.codecc.task.model.PipelineCallbackRegister;
import com.tencent.bk.codecc.task.service.PipelineCallbackRegisterService;
import com.tencent.bk.codecc.task.vo.PipelineCallbackVo;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.pipeline.event.CallBackEvent;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_PIPELINE_BUILD_END_CALLBACK;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_PIPELINE_BUILD_END_CALLBACK;

@Slf4j
@RestResource
public class ServicePipelineCallbackRestResourceImpl implements ServicePipelineCallbackRestResource {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private PipelineCallbackRegisterService pipelineCallbackRegisterService;


    @Override
    public Result<Boolean> callback(PipelineCallbackVo callbackVo, String token) {
        if (StringUtils.isBlank(callbackVo.getEvent()) || callbackVo.getData() == null) {
            log.error("pipeline callback event or data is empty. vo : {}", JSONObject.toJSONString(callbackVo));
            return new Result<>(false);
        }
        String pipelineId = callbackVo.getData().getPipelineId();
        String event = callbackVo.getEvent();
        log.info("pipeline callback pipelineId : {} , event {} ", pipelineId, event);
        //校验Token
        if (!pipelineCallbackRegisterService.checkIfTokenMatch(pipelineId, callbackVo.getEvent(), token)) {
            log.error("pipeline callback token check fail. vo : {}", JSONObject.toJSONString(callbackVo));
            return new Result<>(false);
        }
        if (callbackVo.getEvent().equals(CallBackEvent.BUILD_END.name())) {
            log.info("pipeline callback pipelineId : {} , event {} buildId:{}", pipelineId, event,
                    callbackVo.getData().getBuildId());
            processBuildEndCallback(callbackVo);
        }
        return new Result<>(true);
    }

    /**
     * 发送BuildEnd消息
     */
    private void processBuildEndCallback(PipelineCallbackVo callbackVo) {
        rabbitTemplate.convertAndSend(EXCHANGE_PIPELINE_BUILD_END_CALLBACK,
                ROUTE_PIPELINE_BUILD_END_CALLBACK, callbackVo);
    }
}


