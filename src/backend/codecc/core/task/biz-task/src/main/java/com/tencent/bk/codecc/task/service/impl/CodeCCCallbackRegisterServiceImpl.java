package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongorepository.CodeCCCallbackRegisterRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.CodeCCCallbackRegisterDao;
import com.tencent.bk.codecc.task.model.CodeCCCallbackRegister;
import com.tencent.bk.codecc.task.service.CodeCCCallbackRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CodeCC 回调注册
 */
@Service
public class CodeCCCallbackRegisterServiceImpl implements CodeCCCallbackRegisterService {

    @Autowired
    private CodeCCCallbackRegisterRepository codeCCCallbackRegisterRepository;

    @Autowired
    private CodeCCCallbackRegisterDao codeCCCallbackRegisterDao;

    @Override
    public CodeCCCallbackRegister findRegisterByTaskId(Long taskId) {
        return codeCCCallbackRegisterRepository.findFirstByTaskId(taskId);
    }

    @Override
    public void saveRegister(CodeCCCallbackRegister register) {
        codeCCCallbackRegisterDao.saveRegister(register);
    }
}
