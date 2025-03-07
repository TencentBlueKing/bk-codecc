package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceCodeCCCallbackRestResource;
import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import com.tencent.bk.codecc.task.service.CodeCCCallbackRegisterService;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import com.tencent.devops.common.web.RestResource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class ServiceCodeCCCallbackRestResourceImpl implements ServiceCodeCCCallbackRestResource {

    @Autowired
    private CodeCCCallbackRegisterService codeCCCallbackRegisterService;

    @Override
    public Result<List<CodeCCCallbackEvent>> getTaskEvents(Long taskId) {
        if (taskId == null) {
            return new Result<>(Collections.emptyList());
        }
        CodeCCCallbackRegister register = codeCCCallbackRegisterService.findRegisterByTaskId(taskId);
        if (register == null || CollectionUtils.isEmpty(register.getEvents())) {
            return new Result<>(Collections.emptyList());
        }
        List<CodeCCCallbackEvent> events = register.getEvents().stream().map(CodeCCCallbackEvent::getByName)
                .filter(Objects::nonNull).collect(Collectors.toList());
        return new Result<>(events);
    }
}
