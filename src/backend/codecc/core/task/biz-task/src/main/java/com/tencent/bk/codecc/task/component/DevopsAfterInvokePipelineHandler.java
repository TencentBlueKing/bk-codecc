package com.tencent.bk.codecc.task.component;

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.task.service.PipelineCallbackRegisterService;
import com.tencent.devops.common.client.proxy.DevopsAfterInvokeHandler;
import com.tencent.devops.common.pipeline.Model;
import com.tencent.devops.process.pojo.PipelineId;
import com.tencent.devops.common.api.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DevopsAfterInvokePipelineHandler implements DevopsAfterInvokeHandler {

    private static final String FILTER_CLASS = "com.tencent.devops.process.api.service.ServicePipelineResource";

    private static final String FILTER_METHOD_CREATE = "create";
    private static final String FILTER_METHOD_EDIT = "edit";
    private static final List<String> FILTER_METHODS = Arrays.asList(FILTER_METHOD_CREATE, FILTER_METHOD_EDIT);

    @Autowired
    private PipelineCallbackRegisterService pipelineCallbackRegisterService;

    @Override
    @SuppressWarnings("unchecked")
    public void handleAfterInvoke(@NotNull Method method, @NotNull Object[] args, @Nullable Object result) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        if (!FILTER_CLASS.equals(className) || !FILTER_METHODS.contains(methodName)) {
            return;
        }
        if (result == null || ((Result<?>) result).isNotOk()) {
            log.error("handleAfterInvoke result is not ok.{}", JSONObject.toJSONString(result));
            return;
        }
        log.info("match pipeline devops handler {} {}", args[0], args[1]);
        //判断是否包含Model参数
        for (Object arg : args) {
            if (arg instanceof Model) {
                // 包含Model更新， 注册回调
                try {
                    String userId = (String) args[0];
                    String projectId = (String) args[1];
                    String pipelineId;
                    if (FILTER_METHOD_CREATE.equals(methodName) && ((Result<PipelineId>) result).getData() != null) {
                        pipelineId = ((Result<PipelineId>) result).getData().getId();
                        log.info("match pipeline devops handler {} {} {}", userId, projectId, pipelineId);
                        pipelineCallbackRegisterService.registerBuildEndCallback(projectId, pipelineId, userId);
                    } else if (FILTER_METHOD_EDIT.equals(methodName)) {
                        pipelineId = (String) args[2];
                        log.info("match pipeline devops handler {} {} {}", userId, projectId, pipelineId);
                        pipelineCallbackRegisterService.registerBuildEndCallback(projectId, pipelineId, userId);
                    }
                } catch (Exception e) {
                    log.error("handleAfterInvoke pipeline callback register fail.", e);
                }
                break;
            }
        }
    }
}
