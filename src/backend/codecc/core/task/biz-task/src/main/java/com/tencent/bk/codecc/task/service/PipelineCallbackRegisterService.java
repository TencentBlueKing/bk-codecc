package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.model.PipelineCallbackRegister;

public interface PipelineCallbackRegisterService {

    boolean registerBuildEndCallback(String projectId, String pipelineId, String userId);

    boolean checkIfRegisterBuildEndCallBack(String pipelineId);

    PipelineCallbackRegister getPipelineCallbackRegister(String pipelineId,String event);

    boolean checkIfTokenMatch(String pipelineId,String event, String token);
}
