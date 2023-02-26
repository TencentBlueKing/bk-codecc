package com.tencent.bk.codecc.codeccjob.resource;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_MIGRATION_HISTORY;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_MIGRATION_HISTORY;

import com.tencent.bk.codecc.codeccjob.api.ServiceDefectDataResource;
import com.tencent.bk.codecc.defect.vo.HistoryDefectMigrateVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@RestResource
@Slf4j
public class ServiceDefectDataResourceImpl implements ServiceDefectDataResource {

    @Autowired
    private Client client;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    private static final Integer PAGE_LIMIT = 1000;

    private static final String REDIS_KEY_BATCH_MIGRATE_DEFECT_ENABLE =
            "LOCK_KEY:BATCH_MIGRATION_HISTORY_DEFECT_ENABLE";


    @Override
    public Result<String> migrateHistoryDefect(Long taskId, Boolean all, Integer ignoreType, String ignoreReason) {
        if ((taskId == null || taskId == 0) && (all == null || !all)) {
            return new Result<>("TaskId Is Null And No All Select. No Task Match");
        }

        if (taskId != null && taskId != 0) {
            rabbitTemplate.convertAndSend(EXCHANGE_DEFECT_MIGRATION_HISTORY, ROUTE_DEFECT_MIGRATION_HISTORY,
                    new HistoryDefectMigrateVO(taskId, ignoreType, ignoreReason, "CodeCC"));
            return new Result<>("Success!");
        }
        return migrateAllTaskHistoryDefect(taskId, ignoreType, ignoreReason);
    }

    private Result<String> migrateAllTaskHistoryDefect(Long taskId, Integer ignoreType, String ignoreReason) {

        // 是否允许全量迁移
        String lock = redisTemplate.opsForValue().get(REDIS_KEY_BATCH_MIGRATE_DEFECT_ENABLE);
        if (!"true".equals(lock)) {
            return new Result<>("No Batch Enable");
        }

        //分页获取任务ID
        QueryTaskListReqVO reqVO = new QueryTaskListReqVO();
        reqVO.setCreateFrom(Arrays.asList(ComConstants.BsTaskCreateFrom.BS_CODECC.value(),
                ComConstants.BsTaskCreateFrom.BS_PIPELINE.value()));
        reqVO.setStatus(ComConstants.Status.ENABLE.value());
        reqVO.setSortField("task_id");
        reqVO.setSortType("ASC");
        reqVO.setPageSize(PAGE_LIMIT);

        List<TaskDetailVO> taskDetailVOS;
        int page = 0;
        do {
            reqVO.setPageNum(page);
            Result<Page<TaskDetailVO>> result = client.get(ServiceTaskRestResource.class).getTaskDetailPage(reqVO);
            if (result.isNotOk() || null == result.getData()) {
                log.error("task information is empty! task id: {}", taskId);
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            taskDetailVOS = result.getData().getRecords();
            if (CollectionUtils.isEmpty(taskDetailVOS)) {
                break;
            }
            for (TaskDetailVO taskDetailVO : taskDetailVOS) {
                rabbitTemplate.convertAndSend(EXCHANGE_DEFECT_MIGRATION_HISTORY, ROUTE_DEFECT_MIGRATION_HISTORY,
                        new HistoryDefectMigrateVO(taskDetailVO.getTaskId(), ignoreType, ignoreReason, "CodeCC"));
            }
            page++;
            // 每10秒下发一批，每批1000个task
            sleep(10);
        } while (taskDetailVOS.size() == PAGE_LIMIT);
        return new Result<>("Success!");
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(1000L * seconds);
            // NOCC:EmptyCatchBlock(设计如此:)
        } catch (InterruptedException ignore) {

        }
    }
}
