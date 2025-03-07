package com.tencent.bk.codecc.task.consumer;

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import com.tencent.bk.codecc.task.service.CodeCCCallbackRegisterService;
import com.tencent.bk.codecc.task.vo.event.CodeCCCallbackEventDataVO;
import com.tencent.bk.codecc.task.vo.event.CodeCCCallbackEventVO;
import com.tencent.bk.codecc.task.vo.event.CodeCCCallbackScanFinishEventVO;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import com.tencent.devops.common.util.OkhttpUtils;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CodeCCCallBackEventConsumer {

    @Autowired
    private CodeCCCallbackRegisterService codeCCCallbackRegisterService;

    /**
     * 消费CodeCC Callback事件
     * @param callbackEventVO
     */
    public void consumer(CodeCCCallbackEventVO callbackEventVO) {
        try {
            // 获取Event
            CodeCCCallbackEvent event = callbackEventVO.getEvent();
            CodeCCCallbackEventDataVO dataVO = callbackEventVO.getData();
            if (dataVO.getTaskId() == null) {
                log.error("consumer callback event:{} taskId is null. data:{}", event.name(),
                        JSONObject.toJSONString(dataVO));
                return;
            }
            // 查询是否注册(再次检查)
            CodeCCCallbackRegister register = codeCCCallbackRegisterService.findRegisterByTaskId(dataVO.getTaskId());
            if (register == null || CollectionUtils.isEmpty(register.getEvents())
                    || StringUtils.isBlank(register.getCallbackUrl())) {
                log.error("consumer callback event:{}  taskId:{} register is null ", event.name(),
                        dataVO.getTaskId());
                return;
            }
            // 目前仅处理SCAN_FINISH
            if (event == CodeCCCallbackEvent.SCAN_FINISH
                    && register.getEvents().contains(CodeCCCallbackEvent.SCAN_FINISH.name())
                    && callbackEventVO instanceof CodeCCCallbackScanFinishEventVO) {
                doScanFinishCallBack(register, (CodeCCCallbackScanFinishEventVO) callbackEventVO);
            }
        } catch (Exception e) {
            log.error("consumer callback cause error. callbackEventVO:" + JSONObject.toJSONString(callbackEventVO), e);
        }
    }

    /**
     * 处理扫描完成事件
     * @param scanFinishEventVO
     */
    private void doScanFinishCallBack(CodeCCCallbackRegister register,
            CodeCCCallbackScanFinishEventVO scanFinishEventVO) {
        try {
            OkhttpUtils.INSTANCE.doShortRunHttpPost(
                    register.getCallbackUrl(),
                    JSONObject.toJSONString(scanFinishEventVO.getData()),
                    Collections.emptyMap()
            );
        } catch (Exception e) {
            log.error("doScanFinishCallBack cause error. url:" + register.getCallbackUrl(), e.getMessage());
        }
    }

}
