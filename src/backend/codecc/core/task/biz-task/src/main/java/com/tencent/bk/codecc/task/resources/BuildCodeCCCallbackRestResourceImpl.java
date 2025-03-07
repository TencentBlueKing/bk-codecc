package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.BuildCodeCCCallbackRestResource;
import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import com.tencent.bk.codecc.task.service.CodeCCCallbackRegisterService;
import com.tencent.bk.codecc.task.vo.CodeCCCallbackRegisterVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Status;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class BuildCodeCCCallbackRestResourceImpl implements BuildCodeCCCallbackRestResource {

    @Autowired
    private CodeCCCallbackRegisterService codeCCCallbackRegisterService;

    /**
     * 注册回调
     *
     * @param registerVO
     * @param userName
     * @return
     */
    @Override
    public Result<Boolean> registerTaskEvent(CodeCCCallbackRegisterVO registerVO, String userName) {
        if (registerVO == null || registerVO.getTaskId() == null) {
            return new Result<>(false);
        }
        Long taskId = registerVO.getTaskId();
        CodeCCCallbackRegister register = codeCCCallbackRegisterService.findRegisterByTaskId(taskId);
        // 未注册或者已经失效的， 又不注册，直接返回
        if ((register == null || register.getStatus() == Status.DISABLE.value())
                && !BooleanUtils.isTrue(registerVO.getEnable())) {
            return new Result<>(true);
        }
        // 未注册 需要注册
        if (register == null) {
            register = new CodeCCCallbackRegister();
            register.setProjectId(registerVO.getProjectId());
            register.setPipelineId(registerVO.getPipelineId());
            register.setTaskId(registerVO.getTaskId());
            register.applyAuditInfoOnCreate(userName);
        }
        register.setEvents(registerVO.getEvents());
        register.setCallbackUrl(registerVO.getCallbackUrl());
        Integer status = BooleanUtils.isTrue(registerVO.getEnable()) ? Status.ENABLE.value() : Status.DISABLE.value();
        register.setStatus(status);
        register.applyAuditInfoOnUpdate(userName);
        codeCCCallbackRegisterService.saveRegister(register);
        return new Result<>(true);
    }
}
