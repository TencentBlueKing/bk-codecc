package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.dao.mongotemplate.PipelineCallbackRegisterDao;
import com.tencent.bk.codecc.task.model.PipelineCallbackRegister;
import com.tencent.bk.codecc.task.service.PipelineCallbackRegisterService;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.pipeline.event.CallBackEvent;
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType;
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.process.api.service.ServiceCallBackResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 流水线回调注册
 */
@Slf4j
@Service
public class PipelineCallbackRegisterServiceImpl implements PipelineCallbackRegisterService {

    @Autowired
    private Client client;

    @Autowired
    private PipelineCallbackRegisterDao pipelineCallbackRegisterDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${codecc.public.url:#{null}}")
    private String codeccHost;

    private static final String CALLBACK_LOCK_PREFIX = "pipeline_callback_";

    private static final Long LOCK_TIME = 10L;

    private static final String CODECC_PIPELINE_END_CALLBACK = "codecc_build_finish_callback";

    @Override
    public boolean registerBuildEndCallback(String projectId, String pipelineId, String userId) {
        //如果没有创建，就创建
        String secret = doRegisterBuildEndCallback(projectId, pipelineId, userId);
        if (StringUtils.isBlank(secret)) {
            return false;
        }
        log.info("register build end callback! {} {} {}", userId, projectId, pipelineId);
        //记录到数据库
        PipelineCallbackRegister callbackRegister = new PipelineCallbackRegister(projectId, pipelineId,
                CallBackEvent.BUILD_END.name(), CODECC_PIPELINE_END_CALLBACK, secret);
        callbackRegister.setUpdatedDate(System.currentTimeMillis());
        callbackRegister.setUpdatedBy(userId);
        pipelineCallbackRegisterDao.upsert(callbackRegister);
        return true;
    }

    @Override
    public boolean checkIfRegisterBuildEndCallBack(String pipelineId) {
        return checkIfRegisterEvent(pipelineId, CallBackEvent.BUILD_END.name());
    }

    @Override
    public PipelineCallbackRegister getPipelineCallbackRegister(String pipelineId, String event) {
        return pipelineCallbackRegisterDao.findFirstByPipelineIdAndEvent(pipelineId, event);
    }

    @Override
    public boolean checkIfTokenMatch(String pipelineId, String event, String token) {
        PipelineCallbackRegister callbackRegister = getPipelineCallbackRegister(pipelineId, event);
        if (callbackRegister == null || StringUtils.isBlank(callbackRegister.getSecret())) {
            return false;
        }
        return callbackRegister.getSecret().equals(token);
    }

    public boolean checkIfRegisterEvent(String pipelineId, String event) {
        return getPipelineCallbackRegister(pipelineId, event) != null;
    }

    private String doRegisterBuildEndCallback(String projectId, String pipelineId, String userId) {
        RedisLock lock = getRegisterRedisLock(projectId, CallBackEvent.BUILD_END.name());
        boolean tryLock = lock.tryLock();
        //没获取到锁，表示已经有其他线程在创建相同的CallBack，直接返回
        if (!tryLock) {
            return null;
        }
        try {
            //判断是否已创建
            String secret = UUID.randomUUID().toString();
            PipelineCallbackEvent callbackEvent = new PipelineCallbackEvent(
                    CallBackEvent.BUILD_END,
                    getCallbackUrl(projectId),
                    secret,
                    CODECC_PIPELINE_END_CALLBACK,
                    CallBackNetWorkRegionType.DEVNET
            );
            client.getDevopsService(ServiceCallBackResource.class).createPipelineCallBack(userId,
                    projectId,
                    pipelineId,
                    callbackEvent);
            return secret;
        } catch (Exception e) {
            log.error("registerBuildEndCallback fail! projectId:" + projectId + " pipelineId:"
                    + pipelineId + " userId:" + userId, e);
        } finally {
            lock.unlock();
        }
        return null;
    }

    private String getCallbackUrl(String projectId) {
        return "http://" + codeccHost + "/ms/task/api/service/task/pipeline/callback?x-devops-project-id=" + projectId;
    }

    private RedisLock getRegisterRedisLock(String pipelineId, String event) {
        return new RedisLock(redisTemplate, CALLBACK_LOCK_PREFIX + pipelineId + "_" + event, LOCK_TIME);
    }
}
