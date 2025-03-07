package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;

public interface CodeCCCallbackRegisterService {

    CodeCCCallbackRegister findRegisterByTaskId(Long taskId);

    void saveRegister(CodeCCCallbackRegister register);

}
