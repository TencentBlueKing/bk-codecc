package com.tencent.bk.codecc.defect.service;

import static com.tencent.devops.common.constant.ComConstants.BATCH_DEFECT;
import static com.tencent.devops.common.constant.ComConstants.FUNC_BATCH_DEFECT;
import static com.tencent.devops.common.constant.audit.CodeccAuditAttributeNames.TASK_ID;

import com.google.common.collect.Sets;
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditAttribute;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse.TaskBase;
import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.audit.ActionAuditRecordContents;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.constant.audit.ResourceTypes;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

/**
 * 告警批量分配处理器抽象类
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Slf4j
public abstract class AbstractBatchDefectProcessBizService implements IBizService<BatchDefectProcessReqVO> {

    @Autowired
    protected BizServiceFactory<IQueryWarningBizService> factory;

    @Autowired
    protected Client client;

    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    @Autowired
    private IIgnoredNegativeDefectService iIgnoredNegativeDefectService;

    @Autowired
    private IgnoredNegativeDefectDao ignoredNegativeDefectDao;

    private Map<String, BatchDefectProcessHandler> handlers;

    private final int NOP = 0;
    private final int INS = 1;
    private final int DEL = 2;

    @Override
    @ActionAuditRecord(
            actionId = ActionIds.PROCESS_DEFECT,
            instance = @AuditInstanceRecord(
                    resourceType = ResourceTypes.DEFECT,
                    instanceIds = "#batchDefectProcessReqVO?.getFirstDefectKey()",
                    instanceNames = "#batchDefectProcessReqVO?.bizType"
            ),
            attributes = {
                    @AuditAttribute(name = TASK_ID, value = "#batchDefectProcessReqVO?.getFirstTaskId()")
            },
            content = ActionAuditRecordContents.PROCESS_DEFECT
    )
    @OperationHistory(funcId = FUNC_BATCH_DEFECT, operType = BATCH_DEFECT)
    public Result processBiz(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        log.info("begin to batch process: {}", batchDefectProcessReqVO.toString());
        if (CollectionUtils.isEmpty(batchDefectProcessReqVO.getTaskIdList())
                || batchDefectProcessReqVO.getTaskIdList().size() > 1) {
            return projectBatchProcess(batchDefectProcessReqVO);
        } else {
            // 校验权限
            validOpsPermission(batchDefectProcessReqVO.getUserName(), batchDefectProcessReqVO.getProjectId(),
                    batchDefectProcessReqVO.getTaskIdList());
            batchDefectProcessReqVO.setTaskId(batchDefectProcessReqVO.getTaskIdList().get(0));
            Long effectCount = singleTaskBatchProcess(batchDefectProcessReqVO);
            return new Result<Long>(0, "batch process successful", effectCount);

        }
    }

    /**
     * 校验用户是否可以对任务进行告警管理
     *
     * @param userName
     * @param projectId
     * @param taskIds
     */
    protected void validOpsPermission(String userName, String projectId, List<Long> taskIds) {
        // 无法校验就通过
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(projectId) || CollectionUtils.isEmpty(taskIds)) {
            return;
        }

        // 如果是开源扫描任务，就跳过权限的校验，因为查看问题列表已经是工蜂的成员了
        List<Long> hasPermissionTaskIds = ParamUtils.filterNoDefectOpsPermissionsTask(
                taskIds, projectId, userName);
        if (hasPermissionTaskIds.isEmpty() || hasPermissionTaskIds.size() < taskIds.size()) {
            List<Long> notPermissionList = taskIds.stream().filter(taskId ->
                    !hasPermissionTaskIds.contains(taskId)).collect(Collectors.toList());
            List<TaskBase> taskBases = ParamUtils.getTaskInfoWithToolConfig(notPermissionList, false);
            if (CollectionUtils.isEmpty(taskBases)) {
                return;
            }
            // 组装返回信息
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("对于");
            int index = 0;
            for (TaskBase taskBase : taskBases) {
                if (index > 0) {
                    errorMsg.append(", ");
                }
                errorMsg.append(taskBase.getNameCn());
                index++;
                if (index >= 3 && taskBases.size() > 3) {
                    errorMsg.append("等");
                    break;
                }
            }
            errorMsg.append("任务，用户").append(userName);
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{errorMsg.toString()});
        }
    }

    private Result projectBatchProcess(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<Long> taskIds = getProjectBatchTaskList(batchDefectProcessReqVO);
        if (CollectionUtils.isEmpty(taskIds)) {
            return new Result<>(0, "batch process successful", 0L);
        }
        // 校验权限
        validOpsPermission(batchDefectProcessReqVO.getUserName(), batchDefectProcessReqVO.getProjectId(),
                new LinkedList<>(taskIds));
        long effectCount = 0;
        for (Long taskId : taskIds) {
            BatchDefectProcessReqVO taskBatchDefectVO = new BatchDefectProcessReqVO();
            BeanUtils.copyProperties(batchDefectProcessReqVO, taskBatchDefectVO);
            taskBatchDefectVO.setTaskId(taskId);
            // todo: 考虑try-catch
            effectCount += singleTaskBatchProcess(taskBatchDefectVO);
        }
        return new Result<Long>(0, "batch process successful", effectCount);
    }


    protected Set<Long> getProjectBatchTaskList(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        if (StringUtils.isBlank(batchDefectProcessReqVO.getProjectId())) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"projectId"});
        }
        String projectId = batchDefectProcessReqVO.getProjectId();
        Set<Long> taskIds;
        if (ComConstants.CommonJudge.COMMON_Y.value().equalsIgnoreCase(batchDefectProcessReqVO.getIsSelectAll())) {
            taskIds = new HashSet<>(
                    ParamUtils.allTaskByProjectIdIfEmpty(batchDefectProcessReqVO.getTaskIdList(), projectId,
                            batchDefectProcessReqVO.getUserName()));
        } else {
            taskIds = new HashSet<>(getTaskIdsByDefectKeySet(batchDefectProcessReqVO));
        }
        return taskIds;
    }


    protected abstract List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO);


    /**
     * 单个任务批处理
     *
     * @param batchDefectProcessReqVO
     * @return
     */
    private long singleTaskBatchProcess(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        boolean isSelectAll = ComConstants.CommonJudge.COMMON_Y.value()
                .equalsIgnoreCase(batchDefectProcessReqVO.getIsSelectAll());
        if (!isSelectAll) {
            return processDefect(false, batchDefectProcessReqVO);
        } else {
            DefectQueryReqVO queryCondObj = getDefectQueryReqVO(batchDefectProcessReqVO);
            // fileList 是分页的因子，author会导致处理人分配时分页出现不确定性，并且筛选后数量不大，所以都直接全量查询
            if (CollectionUtils.isNotEmpty(queryCondObj.getFileList())
                    || StringUtils.isNotBlank(queryCondObj.getAuthor())) {
                return processDefect(true, batchDefectProcessReqVO);
            } else {
                return processDefectByPage(batchDefectProcessReqVO);
            }
        }
    }


    /**
     * 根据 business type 判断是需要插入 negative defect 还是删除
     *
     * @param bizType 业务类型
     * @param ignoreReasonType 忽略原因类型
     * @return int              0, 不用操作; 1, 插入; 2 删除
     * @date 2024/3/9
     */
    private int isInsertOrDelete(String bizType, int ignoreReasonType) {
        if (StringUtils.isBlank(bizType)) {
            return NOP;
        }

        if (bizType.contains(BusinessType.REVERT_IGNORE.value())) {
            return DEL;
        } else if (bizType.contains(BusinessType.IGNORE_DEFECT.value())
                && ignoreReasonType == ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
            return INS;
        } else if (bizType.contains(BusinessType.CHANGE_IGNORE_TYPE.value())) {
            if (ignoreReasonType == ComConstants.IgnoreReasonType.ERROR_DETECT.value()) {
                return INS;
            } else {
                return DEL;
            }
        }

        return NOP;
    }

    /**
     * 一次性查询处理所有符合条件的告警
     *
     * @param isSelectAll
     * @param batchDefectProcessReqVO
     * @return
     */
    private long processDefect(Boolean isSelectAll, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        List defectList;
        if (isSelectAll) {
            DefectQueryReqVO queryCondObj = getDefectQueryReqVO(batchDefectProcessReqVO);
            defectList = getDefectsByQueryCond(batchDefectProcessReqVO.getTaskId(), queryCondObj,
                    batchDefectProcessReqVO.getDefectKeySet());
        } else {
            defectList = getEffectiveDefectByDefectKeySet(batchDefectProcessReqVO);
        }

        log.info("batch process, task id: {}, biz: {}, defect list size: {}, defectKeySet size: {}",
                batchDefectProcessReqVO.getTaskId(), batchDefectProcessReqVO.getBizType(),
                defectList == null ? 0 : defectList.size(), batchDefectProcessReqVO.getDefectKeySet().size());

        int opType = NOP;
        if (CollectionUtils.isNotEmpty(defectList) && defectList.get(0) instanceof LintDefectV2Entity) {
            opType = isInsertOrDelete(batchDefectProcessReqVO.getBizType(),
                    batchDefectProcessReqVO.getIgnoreReasonType());
        }

        if (opType == INS) {
            Result<TaskDetailVO> taskBaseResult = client.get(ServiceTaskRestResource.class)
                    .getTaskInfoById(batchDefectProcessReqVO.getTaskId());
            if (null == taskBaseResult || taskBaseResult.isNotOk() || null == taskBaseResult.getData()) {
                log.error("get task info fail!, task id: {}", batchDefectProcessReqVO.getTaskId());
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
            iIgnoredNegativeDefectService.batchInsertIgnoredDefects(
                    defectList,
                    batchDefectProcessReqVO,
                    taskBaseResult.getData()
            );
        } else if (opType == DEL) {
            iIgnoredNegativeDefectService.batchDeleteIgnoredDefects(batchDefectProcessReqVO.getTaskId(),
                    batchDefectProcessReqVO.getDefectKeySet());
        }

        if (CollectionUtils.isNotEmpty(defectList)) {
            doBiz(defectList, batchDefectProcessReqVO);
            processBatchDefectProcessHandler(defectList, batchDefectProcessReqVO);
        }

        return CollectionUtils.isNotEmpty(defectList) ? defectList.size() : 0L;
    }

    protected long processDefectByPage(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        log.info("processDefectByPage start {} {} {}", batchDefectProcessReqVO.getTaskId(),
                batchDefectProcessReqVO.getToolName(), batchDefectProcessReqVO.getDimension());

        String bizType = batchDefectProcessReqVO.getBizType();
        int ignoreReasonType = batchDefectProcessReqVO.getIgnoreReasonType();
        boolean needBatchInsert = (bizType.contains(BusinessType.IGNORE_DEFECT.value())
                || bizType.contains(BusinessType.CHANGE_IGNORE_TYPE.value()))
                && ignoreReasonType == ComConstants.IgnoreReasonType.ERROR_DETECT.value();

        List pageDefectList;
        DefectQueryReqVO queryCondObj = getDefectQueryReqVO(batchDefectProcessReqVO);
        // 起始的FilePath 与 跳过的数量
        String startFilePath = null;
        Long skip = 0L;
        int pageSize = 10000;
        int processCount = 0;
        do {
            // 分页获取，使用条件过滤加SKIP，避免出现深度分页现象
            DefectQueryReqVO reqVO = new DefectQueryReqVO();
            BeanUtils.copyProperties(queryCondObj, reqVO);
            // 设置某些操作特殊的查询条件
            reqVO.setNeedBatchInsert(needBatchInsert);
            pageDefectList = getDefectsByQueryCondWithPage(batchDefectProcessReqVO.getTaskId(), reqVO,
                    startFilePath, skip, pageSize);

            if (CollectionUtils.isEmpty(pageDefectList)) {
                break;
            }

            Pair<Optional<String>, Long> skipPair;
            if (batchDefectProcessReqVO.getBizType().equals(BusinessType.IGNORE_DEFECT.value())
                    || batchDefectProcessReqVO.getBizType().equals(BusinessType.REVERT_IGNORE.value())) {
                // 结果会变更，不需要跳过，直接取
                skipPair = getStartFilePathWithNotSkip(pageDefectList, startFilePath, skip);
            } else {
                skipPair = getStartFilePathAndSkip(pageDefectList, startFilePath, skip);
            }
            startFilePath = skipPair.getFirst().isPresent() ? skipPair.getFirst().get() : null;
            skip = skipPair.getSecond();
            doBizByPage(pageDefectList, batchDefectProcessReqVO);
            processAfterEachPageDone(pageDefectList, batchDefectProcessReqVO);
            processCount += pageDefectList.size();
        } while (pageDefectList.size() == pageSize);
        processAfterAllPageDone(batchDefectProcessReqVO);
        log.info("processDefectByPage end {} {} {} {}", batchDefectProcessReqVO.getTaskId(),
                batchDefectProcessReqVO.getToolName(), batchDefectProcessReqVO.getDimension(), processCount);
        return processCount;
    }

    /**
     * 获取下一轮分页起始的filePath 与 跳过的数量
     *
     * @param pageDefectList 默认按照filePath已经排序好
     * @param startFilePath
     * @param currentSkip
     * @return
     */
    protected Pair<Optional<String>, Long> getStartFilePathAndSkip(List pageDefectList, String startFilePath,
            Long currentSkip) {
        if (CollectionUtils.isEmpty(pageDefectList)) {
            return Pair.of(Optional.ofNullable(startFilePath), currentSkip);
        }
        String lastFilePath = getFilePath(((CommonEntity) pageDefectList.get(pageDefectList.size() - 1)));
        // 查找相同filePath的数量
        Long sameFilePathNum = pageDefectList.stream().filter(defect -> {
            String filePath = getFilePath((CommonEntity) defect);
            if (lastFilePath == null && filePath == null) {
                return true;
            } else if (lastFilePath != null && filePath != null
                    && lastFilePath.equals(filePath)) {
                return true;
            } else {
                return false;
            }
        }).count();
        // 返回下一轮分页起始的filePath 与 跳过的数量
        if (lastFilePath == null && startFilePath == null) {
            return Pair.of(Optional.empty(), currentSkip + sameFilePathNum);
        } else if (lastFilePath != null && startFilePath != null
                && lastFilePath.equals(startFilePath)) {
            return Pair.of(Optional.of(lastFilePath), currentSkip + sameFilePathNum);
        } else {
            return Pair.of(Optional.ofNullable(lastFilePath), sameFilePathNum);
        }
    }

    /**
     * 不使用SKIP 的分页，因为数据在查询后，被处理就不符合下次查询条件，所以不需要SKIP
     *
     * @param pageDefectList
     * @param startFilePath
     * @param currentSkip
     * @return
     */
    protected Pair<Optional<String>, Long> getStartFilePathWithNotSkip(List pageDefectList, String startFilePath,
            Long currentSkip) {
        if (CollectionUtils.isEmpty(pageDefectList)) {
            return Pair.of(Optional.ofNullable(startFilePath), 0L);
        }
        String lastFilePath = getFilePath((CommonEntity) pageDefectList.get(pageDefectList.size() - 1));
        return Pair.of(Optional.ofNullable(lastFilePath), 0L);
    }

    /**
     * 获取defect 的filepath
     *
     * @param defect
     * @return
     */
    protected String getFilePath(CommonEntity defect) {
        if (defect == null) {
            return null;
        }
        if (defect instanceof LintDefectV2Entity) {
            LintDefectV2Entity lint = (LintDefectV2Entity) defect;
            return lint.getFilePath();
        } else if (defect instanceof CCNDefectEntity) {
            CCNDefectEntity ccn = (CCNDefectEntity) defect;
            return ccn.getRelPath();
        } else if ((defect instanceof SCASbomPackageEntity) || (defect instanceof SCALicenseEntity)) {
            return defect.getEntityId();
        }
        return null;
    }


    protected DefectQueryReqVO getDefectQueryReqVO(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 1.解析批量处理请求中的告警查询条件
        String queryDefectCondition = batchDefectProcessReqVO.getQueryDefectCondition();
        DefectQueryReqVO queryCondObj = JsonUtil.INSTANCE.to(queryDefectCondition, DefectQueryReqVO.class);
        if (queryCondObj == null) {
            log.error("defect batch op, query obj deserialize fail, json: {}", queryDefectCondition);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
        }

        // 2.设置是否为恢复忽略再标记
        queryCondObj.setRevertAndMark(batchDefectProcessReqVO.getRevertAndMark());

        // 3.设置是否需要过滤掉正在审核的告警
        boolean needFilterApprovalDefect =
                batchDefectProcessReqVO.getBizType().contains(BusinessType.IGNORE_DEFECT.value())
                        || batchDefectProcessReqVO.getBizType().contains(BusinessType.IGNORE_APPROVAL.value());
        queryCondObj.setNeedFilterApprovalDefect(needFilterApprovalDefect);
        log.info("defect batch op, query obj: {}", queryCondObj);

        // 4.设置业务处理类型对应处理的告警状态
        Set<String> statusAllows = new HashSet<>(getStatusCondition(queryCondObj));
        Set<String> retainStatus = CollectionUtils.isEmpty(queryCondObj.getStatus()) ? statusAllows
                : queryCondObj.getStatus().stream().filter(statusAllows::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(retainStatus)) {
            retainStatus = Sets.newHashSet("-1");
        }
        queryCondObj.setStatus(retainStatus);

        return queryCondObj;
    }

    protected void refreshOverviewData(long taskId) {
        taskPersonalStatisticService.refresh(taskId, "from batch defect process: " + this);
    }


    protected void processBatchDefectProcessHandler(List defects, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        if (handlers == null || handlers.isEmpty()) {
            loadBatchDefectProcessHandler();
        }
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        Pair<ComConstants.BusinessType, ComConstants.ToolType> typePair =
                getBusinessTypeToolTypePair();
        if (typePair == null) {
            return;
        }
        ComConstants.BusinessType businessType = typePair.getFirst();
        ComConstants.ToolType toolType = typePair.getSecond();
        for (BatchDefectProcessHandler handler : handlers.values()) {
            if (CollectionUtils.isNotEmpty(handler.supportBusinessTypes())
                    && handler.supportBusinessTypes().contains(businessType)
                    && CollectionUtils.isNotEmpty(handler.supportToolTypes())
                    && handler.supportToolTypes().contains(toolType)) {
                handler.handler(defects, batchDefectProcessReqVO, businessType, toolType);
            }
        }
    }

    private void loadBatchDefectProcessHandler() {
        handlers = SpringContextUtil.Companion.getBeansOfType(BatchDefectProcessHandler.class);
    }

    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
     * @param queryCondObj
     * @return
     */
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
    }

    protected abstract void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    /**
     * 分页处理
     *
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    protected abstract void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    /**
     * 每一次分页完成后执行
     *
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    protected void processAfterEachPageDone(List defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // Handler 执行
        processBatchDefectProcessHandler(defectList, batchDefectProcessReqVO);
        // 自定义的操作执行
        processCustomizeOpsAfterEachPageDone(defectList, batchDefectProcessReqVO);
    }

    /**
     * 每一次分页完成后执行
     *
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    protected abstract void processCustomizeOpsAfterEachPageDone(List defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO);

    /**
     * 所有分页完成后执行
     *
     * @param batchDefectProcessReqVO
     */
    protected abstract void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO);

    protected abstract List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO,
            Set<String> defectKeySet);

    protected abstract List getDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO defectQueryReqVO,
            String startFilePath, Long skip, Integer pageSize);

    protected abstract List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO);

    protected abstract Pair<ComConstants.BusinessType, ComConstants.ToolType> getBusinessTypeToolTypePair();
}
