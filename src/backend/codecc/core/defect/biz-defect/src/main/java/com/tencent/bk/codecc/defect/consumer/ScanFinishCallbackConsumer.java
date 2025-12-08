package com.tencent.bk.codecc.defect.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.TaskLogOverviewDao;
import com.tencent.bk.codecc.defect.dto.ScanTaskTriggerDTO;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.bk.codecc.task.api.ServiceCodeCCCallbackRestResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.event.CodeCCCallbackScanFinishEventDataVO;
import com.tencent.bk.codecc.task.vo.event.CodeCCCallbackScanFinishEventVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.CodeCCCallbackEvent;
import com.tencent.devops.common.util.TaskCreateFromUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScanFinishCallbackConsumer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TaskLogOverviewDao taskLogOverviewDao;

    @Autowired
    private BuildRepository buildRepository;

    @Autowired
    private Client client;

    /**
     * 消费与记录扫描完成事件
     *
     * @param scanTaskTrigger
     */
    public void consumer(ScanTaskTriggerDTO scanTaskTrigger) {
        try {
            Long taskId = scanTaskTrigger.getTaskId();
            String buildId = scanTaskTrigger.getBuildId();
            // 判断任务是否开启了
            Result<List<CodeCCCallbackEvent>> eventsVO =
                    client.get(ServiceCodeCCCallbackRestResource.class).getTaskEvents(taskId);
            if (eventsVO.isNotOk() || CollectionUtils.isEmpty(eventsVO.getData())
                    || eventsVO.getData().contains(CodeCCCallbackEvent.SCAN_FINISH)) {
                log.error("TaskId:{} ScanFinishCallbackConsumer skip, not register event.", taskId);
            }

            log.info("start to assemble scan finish callback data taskId:{} buildId:{}", taskId, buildId);
            // 无构建记录，不需要传
            TaskLogOverviewEntity overview = taskLogOverviewDao.findOneByTaskIdAndBuildId(taskId, buildId);
            if (overview == null) {
                log.error("TaskId:{} BuildId:{} ScanFinishCallbackConsumer overview is Empty", taskId, buildId);
                return;
            }
            Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            TaskDetailVO taskDetail = null;
            // 无任务信息，则不需要传
            if (result.isOk() && result.getData() != null) {
                taskDetail = result.getData();
            } else {
                log.error("TaskId:{} BuildId:{} ScanFinishCallbackConsumer overview is Empty", taskId, buildId);
                return;
            }
            // 主要查询用户，如果没有就为空
            BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
            // 组装消息通知信息
            CodeCCCallbackScanFinishEventVO vo = new CodeCCCallbackScanFinishEventVO(
                    new CodeCCCallbackScanFinishEventDataVO(
                            taskDetail.getProjectId(),
                            taskDetail.getPipelineId(),
                            taskDetail.getTaskId(),
                            buildEntity == null ? null : buildEntity.getBuildUser(),
                            buildId,
                            overview.getToolList(),
                            taskDetail.getLanguages(),
                            taskDetail.getCodeLang(),
                            overview.getStartTime(),
                            overview.getEndTime(),
                            overview.getStatus(),
                            TaskCreateFromUtils.INSTANCE.getTaskRealCreateFrom(taskDetail.getProjectId(),
                                    taskDetail.getCreateFrom()).value()
                    )
            );
            // 发送消息
            rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_CODECC_CALLBACK_EVENT,
                    ConstantsKt.ROUTE_CODECC_CALLBACK_EVENT, vo);
            log.info("end to assemble scan finish callback data taskId:{} buildId:{}", taskId, buildId);
        } catch (Exception e) {
            log.error("ScanFinishCallbackConsumer cause error. scanTaskTrigger:"
                    + JSONObject.toJSONString(scanTaskTrigger), e.getMessage());
        }
    }
}
