package com.tencent.bk.codecc.defect.consumer;

import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_VIRTUAL_BUILD_ID;

import com.tencent.bk.codecc.defect.service.AbstractAnalyzeTaskBizService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.impl.CommonAnalyzeTaskBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommonDefectMigrationConsumer extends AbstractDefectMigration implements IConsumer<CommitDefectVO> {

    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Override
    public void consumer(CommitDefectVO vo) {
        try {
            // 若批量模式开启，当前mq消息为用户正常触发的，且该任务的数据还没完成迁移，则recommit延迟处理；让道于批量模式
            if (commonDefectMigrationService.isOnBatchMigrationMode()
                    && !DATA_MIGRATION_VIRTUAL_BUILD_ID.equals(vo.getBuildId())
                    && !hasMigrated(vo.getTaskId())) {

                recommitForDataMigration(vo);
                return;
            }

            boolean beContinue = continueWithDataMigration(vo);
            // true则说明已执行完迁移逻辑，可回归提单正常业务流程
            if (beContinue) {
                // 若是人工发起的批量迁移，则无需执行提单剩余流程
                if (DATA_MIGRATION_VIRTUAL_BUILD_ID.equals(vo.getBuildId())) {
                    return;
                }

                Pair<String, String> pair = getDefectCommitBizMQInfo(vo);
                String exchange = pair.getFirst();
                String routingKey = pair.getSecond();
                rabbitTemplate.convertAndSend(exchange, routingKey, vo);
            }
        } catch (Throwable t) {
            log.error("defect migration consumer error: {}, {}, {}",
                    vo.getTaskId(), vo.getToolName(), vo.getBuildId(), t);
        }
    }

    @Override
    protected boolean hasMigrated(long taskId) {
        return commonDefectMigrationService.isMigrationDone(taskId);
    }

    @Override
    protected void doMigration(long taskId, String toolName, String triggerUser) {
        commonDefectMigrationService.dataMigration(taskId, toolName, triggerUser);
    }

    @Override
    protected Collection<String> matchToolList() {
        return commonDefectMigrationService.matchToolNameSet();
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(vo.getCreateFrom())
                ? ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON_OPENSOURCE
                : ConstantsKt.EXCHANGE_DEFECT_MIGRATION_COMMON;
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(vo.getCreateFrom())
                ? ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON_OPENSOURCE : ConstantsKt.ROUTE_DEFECT_MIGRATION_COMMON;
    }

    /**
     * 获取提单业务相关的队列信息
     * 注：提单业务逻辑需要区分2点
     * 1、PINPOINT与 COVERITY、KLOCWORK 不同
     * 2、开源扫描与否
     *
     * @return 1st-> exchange, 2nd-> routingKey
     * @see AbstractAnalyzeTaskBizService#asyncCommitDefect(UploadTaskLogStepVO, TaskDetailVO)
     * @see CommonAnalyzeTaskBizServiceImpl#postHandleDefectsAndStatistic(UploadTaskLogStepVO, TaskDetailVO)
     */
    private Pair<String, String> getDefectCommitBizMQInfo(CommitDefectVO vo) {
        String toolName = vo.getToolName().toLowerCase();
        boolean isGongfengScan = BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(vo.getCreateFrom());
        String exchange;
        String routingKey;

        if (Tool.PINPOINT.name().equalsIgnoreCase(toolName)) {
            String toolPattern = ToolPattern.PINPOINT.name().toLowerCase();

            if (isGongfengScan) {
                exchange = String.format("%s%s.opensource", ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern);
                routingKey = String.format("%s%s.opensource", ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern);
            } else {
                // 非工蜂，需按告警文件大小分级
                long fileSize = vo.getDefectFileSize() == null ? 0L : vo.getDefectFileSize();
                if (fileSize > 1024 * 1024 * 1024) {
                    exchange = ConstantsKt.EXCHANGE_DEFECT_COMMIT_SUPER_LARGE;
                    routingKey = ConstantsKt.ROUTE_DEFECT_COMMIT_SUPER_LARGE;
                } else if (fileSize > 1024 * 1024 * 200 && fileSize < 1024 * 1024 * 1024) {
                    exchange = String.format("%s%s.large", ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern);
                    routingKey = String.format("%s%s.large", ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern);
                } else {
                    exchange = String.format("%s%s.new", ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern);
                    routingKey = String.format("%s%s.new", ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern);
                }
            }
        } else {
            // 若是coverity、klocwork
            if (isGongfengScan) {
                exchange = ConstantsKt.PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT + toolName;
                routingKey = ConstantsKt.PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT + toolName;
            } else {
                exchange = ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + toolName;
                routingKey = ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + toolName;
            }
        }

        return Pair.of(exchange, routingKey);
    }
}
