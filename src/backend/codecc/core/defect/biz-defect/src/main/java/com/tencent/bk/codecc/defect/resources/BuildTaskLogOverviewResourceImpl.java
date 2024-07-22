package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildTaskLogOverviewResource;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.task.vo.PluginErrorVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class BuildTaskLogOverviewResourceImpl implements BuildTaskLogOverviewResource {

    @Autowired
    TaskLogOverviewService taskLogOverviewService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Result<Boolean> saveActualTools(TaskLogOverviewVO taskLogOverviewVO) {
        if (taskLogOverviewVO.getTaskId() == null || taskLogOverviewVO.getTools() == null) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_, new String[]{"非法空参数"});
        }
        return new Result<>(taskLogOverviewService.saveActualExeTools(taskLogOverviewVO));
    }

    @Override
    public Result<Boolean> reportPluginResult(TaskLogOverviewVO taskLogOverviewVO) {
        if (taskLogOverviewVO.getTaskId() == null || taskLogOverviewVO.getBuildId() == null) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_, new String[]{"非法空参数"});
        }
        Long taskId = taskLogOverviewVO.getTaskId();
        String buildId = taskLogOverviewVO.getBuildId();
        log.info("start to report plugin result taskId:{} buildId:{}", taskId, buildId);
        // 更新Overview错误信息
        taskLogOverviewService.reportPluginErrorInfo(taskId, buildId, taskLogOverviewVO.getPluginErrorCode(),
                taskLogOverviewVO.getPluginErrorType());
        // 发送结束通知
        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_PLUGIN_ERROR_CALLBACK,
                ConstantsKt.ROUTE_PLUGIN_ERROR_CALLBACK, new PluginErrorVO(taskId, buildId));
        return new Result<>(Boolean.TRUE);
    }
}
