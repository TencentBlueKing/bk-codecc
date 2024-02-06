package com.tencent.bk.codecc.defect.resources;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DATA_SEPARATION;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_DEFECT_MIGRATION_TRIGGER_BATCH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_COOL_DOWN_TRIGGER;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DATA_SEPARATION_WARM_UP;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEFECT_MIGRATION_TRIGGER_BATCH;

import com.tencent.bk.codecc.defect.api.UserBackendParamsResource;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.common.BackendParamsVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 后端配置参数接口实现类
 *
 * @version V1.0
 * @date 2019/12/14
 */
@Slf4j
@RestResource
public class UserBackendParamsResourceImpl implements UserBackendParamsResource {

    /**
     * 特殊参数需要与规则集关联的工具
     */
    @Value("${codecc.paramJsonRelateCheckerSetTools:#{null}}")
    private String paramJsonRelateCheckerSetTools;

    /**
     * 任务语言需要与规则集关联的工具
     */
    @Value("${codecc.codeLangRelateCheckerSetTools:#{null}}")
    private String codeLangRelateCheckerSetTools;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Override
    public Result<BackendParamsVO> getParams() {
        BackendParamsVO backendParamsVO = new BackendParamsVO();
        backendParamsVO.setCodeLangRelateCheckerSetTools(
                List2StrUtil.fromString(codeLangRelateCheckerSetTools, ComConstants.SEMICOLON));
        backendParamsVO.setParamJsonRelateCheckerSetTools(
                List2StrUtil.fromString(paramJsonRelateCheckerSetTools, ComConstants.SEMICOLON));
        return new Result<>(backendParamsVO);
    }

    @Override
    public Result<Boolean> pubMsgToCommitDefect(String mode, String jsonBody) {
        // 注：large消费线程数为1
        String toolPattern = "LINT";
        String prefix = !StringUtils.isEmpty(mode) && "new".equals(mode) ? "new" : "large";
        String exchange = String.format(
                "%s%s.%s",
                ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern.toLowerCase(Locale.ENGLISH), prefix
        );
        String routingKey = String.format(
                "%s%s.%s",
                ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT, toolPattern.toLowerCase(Locale.ENGLISH), prefix
        );

        CommitDefectVO vo = JsonUtil.INSTANCE.to(jsonBody, CommitDefectVO.class);
        rabbitTemplate.convertAndSend(exchange, routingKey, vo);

        return new Result<>(true);
    }

    @Override
    public Result<Boolean> commonToLintDataMigrationSwitch(String mode) {
        // off为关闭、single为单任务用户触发、batch为人工后台批量触发

        if ("single".equals(mode)) {
            commonDefectMigrationService.switchOnSingleMigrationMode();
        } else if ("batch".equals(mode)) {
            commonDefectMigrationService.switchOnBatchMigrationMode();
        } else if ("off".equals(mode)) {
            commonDefectMigrationService.switchOffAll();
        } else {
            return new Result<>(-9999, "参数错误");
        }

        log.warn("commonToLintDataMigrationSwitch: {}", mode);

        return new Result<>(true);
    }

    @Override
    public Result<Boolean> batchCommonToLintDataMigration() {
        if (!commonDefectMigrationService.isOnBatchMigrationMode()) {
            return new Result<>(-9999, "批量开关没有打开");
        }

        rabbitTemplate.convertAndSend(
                EXCHANGE_DEFECT_MIGRATION_TRIGGER_BATCH,
                ROUTE_DEFECT_MIGRATION_TRIGGER_BATCH,
                1
        );

        return new Result<>(true);
    }

    @Override
    public Result<String> addCommitDefectBlockList(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return new Result<>("POST BODY EMPTY");
        }

        Set<Long> allTaskIdSet = Sets.newHashSet();

        String before = redisTemplate.opsForValue().get(RedisKeyConstants.COMMIT_DEFECT_TASK_ID_BLOCK_LIST);
        log.info("addCommitDefectBlockList before: {}", before);
        if (!StringUtils.isEmpty(before)) {
            Set<Long> beforeSet = Stream.of(before.split(",")).map(Long::valueOf).collect(Collectors.toSet());
            allTaskIdSet.addAll(beforeSet);
        }

        allTaskIdSet.addAll(taskIds);
        String after = String.join(",", allTaskIdSet.stream().map(Object::toString).collect(Collectors.toList()));
        log.info("addCommitDefectBlockList after: {}", after);
        redisTemplate.opsForValue().set(RedisKeyConstants.COMMIT_DEFECT_TASK_ID_BLOCK_LIST, after);

        return new Result<>(after);
    }

    @Override
    public Result<Boolean> clearCommitDefectBlockList(List<Long> taskIds) {
        String current = redisTemplate.opsForValue().get(RedisKeyConstants.COMMIT_DEFECT_TASK_ID_BLOCK_LIST);
        redisTemplate.delete(RedisKeyConstants.COMMIT_DEFECT_TASK_ID_BLOCK_LIST);
        log.info("clearCommitDefectBlockList, current: {}", current);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> coolDown(Long taskId) {
        rabbitTemplate.convertAndSend(EXCHANGE_DATA_SEPARATION, ROUTE_DATA_SEPARATION_COOL_DOWN, taskId);

        return Result.success(true);
    }

    @Override
    public Result<Boolean> warmUp(Long taskId) {
        rabbitTemplate.convertAndSend(EXCHANGE_DATA_SEPARATION, ROUTE_DATA_SEPARATION_WARM_UP, taskId);

        return Result.success(true);
    }

    @Override
    public Result<Boolean> hotColdDataSeparationTrigger() {
        rabbitTemplate.convertAndSend(
                EXCHANGE_DATA_SEPARATION,
                ROUTE_DATA_SEPARATION_COOL_DOWN_TRIGGER,
                System.currentTimeMillis()
        );

        return Result.success(true);
    }
}
