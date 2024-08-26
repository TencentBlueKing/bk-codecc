package com.tencent.bk.codecc.defect.service.impl;


import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskInvalidToolDefectLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.TaskInvalidToolDefectLog;
import com.tencent.bk.codecc.defect.service.TaskInvalidToolDefectService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.redis.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskInvalidToolDefectServiceImpl implements TaskInvalidToolDefectService {

    private static final int COMMIT_LOCK_EXPIRE_TIME = 30 * 60;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private TaskInvalidToolDefectLogRepository taskInvalidToolDefectLogRepository;

    @Override
    public void excludeToolDefect(Long taskId, String createFrom, String buildId, String toolName, String toolType) {
        RedisLock locker = null;
        try {
            // 如果是工蜂项目，不需要上锁
            locker = getLocker(taskId, toolName);
            if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom) && !locker.tryLock()) {
                // 如果没获取到锁，表明有其他构建该工具正在提单，直接放弃处理
                return;
            }
            long excludeDefectsCount = 0L;
            // 修改告警状态
            if (ToolType.DIMENSION_FOR_LINT_PATTERN_LIST.contains(toolType)) {
                excludeDefectsCount = lintDefectV2Dao.batchExcludeToolNewDefect(taskId, toolName);
            } else if (ToolType.CCN.name().equals(toolType)) {
                excludeDefectsCount = ccnDefectDao.batchExcludeToolNewDefect(taskId, toolName);
            }
            // 记录变更
            TaskInvalidToolDefectLog taskInvalidToolDefectLog =
                    taskInvalidToolDefectLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId,
                            toolName, buildId);
            if (taskInvalidToolDefectLog == null) {
                taskInvalidToolDefectLog = new TaskInvalidToolDefectLog();
                taskInvalidToolDefectLog.setTaskId(taskId);
                taskInvalidToolDefectLog.setToolName(toolName);
                taskInvalidToolDefectLog.setBuildId(buildId);
                taskInvalidToolDefectLog.setCreatedDate(System.currentTimeMillis());
            }
            taskInvalidToolDefectLog.setDefectCount(excludeDefectsCount);
            taskInvalidToolDefectLogRepository.save(taskInvalidToolDefectLog);
            log.info("task invalid tool defect exclude {} {} {} {}", taskId, toolName,
                    buildId, excludeDefectsCount);
        } catch (Exception e) {
            log.error("task invalid tool defect exclude cause error. {} {} {}", taskId, toolName, buildId, e);
        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }
        }
    }

    @Override
    public TaskInvalidToolDefectLog getLatestToolLog(Long taskId, String toolName) {
        return taskInvalidToolDefectLogRepository.findFirstByTaskIdAndToolNameOrderByCreatedDateDesc(taskId, toolName);
    }

    private RedisLock getLocker(Long taskId, String toolName) {
        String key = String.format("COMMIT_DEFECT_LOCK:%d:%s", taskId, toolName);

        return new RedisLock(redisTemplate, key, COMMIT_LOCK_EXPIRE_TIME);
    }
}
