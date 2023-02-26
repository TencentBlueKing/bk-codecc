package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_SWITCH_BATCH_MODE;
import static com.tencent.devops.common.constant.ComConstants.DATA_MIGRATION_SWITCH_SINGLE_MODE;

import com.google.common.collect.ImmutableSet;
import com.tencent.bk.codecc.defect.dao.mongorepository.CommonDefectMigrationRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.model.CommonDefectMigrationEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DataMigrationStatus;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * common告警迁移服务
 */
@Slf4j
@Service
public class CommonDefectMigrationServiceImpl implements CommonDefectMigrationService {

    @Autowired
    private CommonDefectMigrationRepository commonDefectMigrationRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private DefectConverter defectConverter;

    private static final Set<String> COMMON_DEFECT_TOOL_SET = new ImmutableSet.Builder()
            .add(ToolPattern.COVERITY.name().toUpperCase())
            .add(ToolPattern.KLOCWORK.name().toUpperCase())
            .add(ToolPattern.PINPOINT.name().toUpperCase())
            .build();

    @Override
    public boolean isMigrationDone(long taskId) {
        /*============
         *  开关均为关闭时，幂等于已执行过迁移(不代表迁移成功)
         *  便于风险控制，该判定往往用于进队前、消费前的校验
         =============*/
        if (!isOnSingleMigrationMode() && !isOnBatchMigrationMode()) {
            return true;
        }

        List<CommonDefectMigrationEntity> migrationRecords = commonDefectMigrationRepository.findByTaskId(taskId);

        return !CollectionUtils.isEmpty(migrationRecords)
                && !migrationRecords.stream().allMatch(x -> x.getStatus() == DataMigrationStatus.PROCESSING.value());
    }

    /**
     * 数据迁移
     *
     * @param taskId
     * @param toolName
     */
    @Override
    public void dataMigration(long taskId, String toolName, String triggerUser) {
        int successDefectCount = 0;
        long beginTime = System.currentTimeMillis();
        CommonDefectMigrationEntity migrationRecord = new CommonDefectMigrationEntity();
        boolean isSuccess = true;
        String errorStackTrace = null;

        try {
            // 1、开始"执行中"
            migrationRecord.setTaskId(taskId);
            migrationRecord.setToolName(toolName);
            migrationRecord.setStatus(DataMigrationStatus.PROCESSING.value());
            migrationRecord.applyAuditInfo(triggerUser, triggerUser);
            // count-300W数据量约5秒，单任务common类告警远少于300W
            Integer totalCount = defectRepository.countByTaskIdAndToolName(taskId, toolName);
            migrationRecord.setTotalCount(totalCount);
            commonDefectMigrationRepository.insert(migrationRecord);

            if (totalCount == 0) {
                return;
            }

            // 2、批量分页迁移
            int pageTotal = Integer.MAX_VALUE;
            int pageSize = 5000; // 实测每批1W的话，部分任务的告警数据会稍稍超过16MB限制，稳妥起见直接折半

            for (int curPage = 0; curPage < pageTotal; curPage++) {
                List<CommonDefectEntity> commonDefectList = defectRepository.findByTaskIdAndToolNameOrderByStatusDesc(
                        taskId,
                        toolName,
                        PageRequest.of(curPage, pageSize)
                );

                if (CollectionUtils.isEmpty(commonDefectList)) {
                    break;
                }

                List<LintDefectV2Entity> lintDefectList = defectConverter.commonToLint(commonDefectList);
                batchSaveToDB(lintDefectList);
                successDefectCount += commonDefectList.size();
                sleep(10L);
                // 补偿偏移
                beginTime += 10L;
            }
        } catch (Exception e) {
            isSuccess = false;
            // 最大存2KB
            errorStackTrace = StringUtils.substring(ExceptionUtils.getStackTrace(e), 0, 2 * 1024);
            log.error("data migration error, task id: {}, tool name: {}", taskId, toolName, e);
        } finally {
            // 3、回写终态
            int status = isSuccess ? DataMigrationStatus.SUCCESS.value() : DataMigrationStatus.FAIL.value();
            migrationRecord.setStatus(status);
            migrationRecord.setCostTimeMS(System.currentTimeMillis() - beginTime);
            migrationRecord.setSuccessCount(successDefectCount);
            migrationRecord.setErrorStackTrace(errorStackTrace);
            migrationRecord.applyAuditInfo(triggerUser);
            commonDefectMigrationRepository.save(migrationRecord);
        }
    }

    @Override
    public boolean isMigrationSuccessful(long taskId) {
        /*=================
         * 若common告警表不存在任何数据，也可认为迁移完毕且成功，一般有2种情况:
         * 1、数据迁移功能上线后，才新建的任务
         * 2、老任务，但没有配置相关工具或没有成功录入过相关告警
         =================*/
        List<CommonDefectEntity> commonDefectList = defectRepository.findByTaskId(taskId, PageRequest.of(0, 1));
        if (CollectionUtils.isEmpty(commonDefectList)) {
            return true;
        }

        List<CommonDefectMigrationEntity> migrationRecords = commonDefectMigrationRepository.findByTaskId(taskId);

        return !CollectionUtils.isEmpty(migrationRecords)
                && migrationRecords.stream().allMatch(x -> x.getStatus() == DataMigrationStatus.SUCCESS.value());
    }

    @Override
    public Set<String> matchToolNameSet() {
        return COMMON_DEFECT_TOOL_SET;
    }

    @Override
    public void switchOnSingleMigrationMode() {
        redisTemplate.opsForValue().set(DATA_MIGRATION_SWITCH_SINGLE_MODE, "1");
        redisTemplate.delete(DATA_MIGRATION_SWITCH_BATCH_MODE);
    }

    @Override
    public void switchOnBatchMigrationMode() {
        redisTemplate.opsForValue().set(DATA_MIGRATION_SWITCH_BATCH_MODE, "1");
        redisTemplate.delete(DATA_MIGRATION_SWITCH_SINGLE_MODE);
    }

    @Override
    public boolean isOnSingleMigrationMode() {
        return "1".equals(redisTemplate.opsForValue().get(ComConstants.DATA_MIGRATION_SWITCH_SINGLE_MODE));
    }

    @Override
    public boolean isOnBatchMigrationMode() {
        return "1".equals(redisTemplate.opsForValue().get(ComConstants.DATA_MIGRATION_SWITCH_BATCH_MODE));
    }

    @Override
    public void switchOffAll() {
        redisTemplate.delete(DATA_MIGRATION_SWITCH_SINGLE_MODE);
        redisTemplate.delete(DATA_MIGRATION_SWITCH_BATCH_MODE);
    }

    private void batchSaveToDB(List<LintDefectV2Entity> insertList) {
        if (CollectionUtils.isEmpty(insertList)) {
            return;
        }

        mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class)
                .insert(insertList)
                .execute();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
            // NOCC:EmptyCatchBlock(设计如此:)
        } catch (InterruptedException ignore) {

        }
    }
}
