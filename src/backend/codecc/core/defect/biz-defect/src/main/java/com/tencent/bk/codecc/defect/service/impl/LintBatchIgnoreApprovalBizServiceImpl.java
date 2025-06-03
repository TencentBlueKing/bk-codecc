package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.TaskScopeType;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverType;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverStatus;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.service.IgnoreApprovalService;
import com.tencent.bk.codecc.defect.service.LintQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailListQueryReqVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.PreIgnoreApprovalCheckVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.DefectIgnoreApprovalVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.MapUtil;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tencent.devops.common.constant.ComConstants.BATCH_DEFECT;
import static com.tencent.devops.common.constant.ComConstants.FUNC_BATCH_DEFECT;
import static com.tencent.devops.common.constant.ComConstants.KEY_UNDERLINE;
import static com.tencent.devops.common.constant.ComConstants.SEPARATOR_SEMICOLON;
import static com.tencent.devops.common.constant.ComConstants.STRING_DELIMITER;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_DEFECT_IGNORE_APPROVAL;

/**
 * Lint 忽略审批
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("LINTBatchIgnoreApprovalBizService")
public class LintBatchIgnoreApprovalBizServiceImpl extends AbstractLintBatchDefectProcessBizService {


    @Autowired
    private IgnoreApprovalService ignoreApprovalService;

    @Autowired
    private LintDefectV2Dao defectDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private IIgnoreTypeService ignoreTypeService;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;


    private static final int DEFAULT_PRE_APPROVAL_DEFECT_LIMIT = 50;

    @Override
    @OperationHistory(funcId = FUNC_BATCH_DEFECT, operType = BATCH_DEFECT)
    public Result processBiz(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<Long> taskIds = getProjectBatchTaskList(batchDefectProcessReqVO);
        if (CollectionUtils.isEmpty(taskIds)) {
            log.warn("process ignore approval task empty {}", batchDefectProcessReqVO.getProjectId());
            return new Result<>(0, "batch process successful", 0L);
        }
        log.info("process ignore approval {} task size: {}", batchDefectProcessReqVO.getProjectId(), taskIds.size());
        // 校验权限
        validOpsPermission(batchDefectProcessReqVO.getUserName(), batchDefectProcessReqVO.getProjectId(),
                new LinkedList<>(taskIds));
        boolean isSelectAll = ComConstants.CommonJudge.COMMON_Y.value()
                .equalsIgnoreCase(batchDefectProcessReqVO.getIsSelectAll());
        ApprovalProcessVO approvalProcessVO;
        if (isSelectAll) {
            approvalProcessVO = processBizWithSelectAll(taskIds, batchDefectProcessReqVO);
        } else {
            approvalProcessVO = processBizWithNoSelectAll(taskIds, batchDefectProcessReqVO);
        }
        List<ApprovalIdToTasksVO> ignoreApprovalIdToTasks = approvalProcessVO.getIgnoreApprovalIdToTasks();
        if (ignoreApprovalIdToTasks == null || ignoreApprovalIdToTasks.isEmpty()) {
            log.warn("process ignore approval ignoreApprovalIdToTasks empty {}",
                    batchDefectProcessReqVO.getProjectId());
            return new Result<>(0, "batch process successful", 0L);
        }
        log.info("process ignore approval {} ignoreApprovalIdToTasks size: {}", batchDefectProcessReqVO.getProjectId(),
                ignoreApprovalIdToTasks.size());
        List<IgnoreApprovalConfigVO> approvalConfigs = approvalProcessVO.getApprovalConfigs();

        doAfterAllTaskDone(ignoreApprovalIdToTasks, batchDefectProcessReqVO, approvalConfigs);
        return new Result<>(0, "batch process successful", approvalProcessVO.getTotalMatchDefectCount());
    }

    private ApprovalProcessVO processBizWithSelectAll(
            Set<Long> taskIds, BatchDefectProcessReqVO batchDefectProcessReqVO) {
        log.info("process ignore approval with select all {}", batchDefectProcessReqVO.getProjectId());
        // 获取查询配置
        DefectQueryReqVO queryCondObj = getDefectQueryReqVO(batchDefectProcessReqVO);
        queryCondObj.setNeedFilterApprovalDefect(true);
        queryCondObj.setDimensionList(ParamUtils.allDimensionIfEmptyForLint(queryCondObj.getDimensionList()));
        // 获取项目下忽略审批配置，这里会根据维度与严重程度过滤不适合的忽略配置
        List<IgnoreApprovalConfigVO> approvalConfigs = ignoreApprovalService.getProjectMatchConfig(
                batchDefectProcessReqVO.getProjectId(),
                batchDefectProcessReqVO.getIgnoreReasonType(),
                queryCondObj.getDimensionList(),
                CollectionUtils.isEmpty(queryCondObj.getSeverity()) ? null
                        : queryCondObj.getSeverity().stream().map(Integer::valueOf).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            log.warn("process ignore approvalConfigs is empty : {} ", batchDefectProcessReqVO.getProjectId());
            return new ApprovalProcessVO(Collections.emptyList(), Collections.emptyList(), 0L);
        }
        // 获取规则信息
        LintQueryWarningSpecialService lintSpecialService =
                SpringContextUtil.Companion.getBean(LintQueryWarningSpecialService.class);
        List<CheckerDetailVO> checkers = lintSpecialService.getCheckerDetails(
                queryCondObj.getCheckerSet(), queryCondObj.getChecker(),
                queryCondObj.getToolNameList(), queryCondObj.getDimensionList()
        );
        String opsId = UUID.randomUUID().toString().replace("-", "");
        // 先按任务分组，匹配符合的忽略配置
        Map<Long, List<IgnoreApprovalConfigVO>> taskToApprovalConfigs =
                getTaskMatchIgnoreApprovalConfig(taskIds, approvalConfigs);
        // 再按照严重程度分组
        Map<Set<String>, Map<Long, List<String>>> severitiesToTaskToolMaps = getSeveritiesToTaskToolMap(
                taskToApprovalConfigs, queryCondObj, checkers);
        Map<String, String> matchIdToApprovalId = new HashMap<>();
        Map<String, ApprovalIdToTasksVO> ignoreApprovalIdToTasks = new HashMap<>();
        for (Map.Entry<Set<String>, Map<Long, List<String>>> severitiesMap : severitiesToTaskToolMaps.entrySet()) {
            Set<String> severities = severitiesMap.getKey();
            Map<Long, List<String>> taskToolMap = severitiesMap.getValue();
            if (taskToolMap == null || taskToolMap.isEmpty()) {
                continue;
            }
            List<ApprovalIdToTasksVO> processResult = processDefect(taskToolMap, severities, queryCondObj, opsId,
                    checkers, approvalConfigs, matchIdToApprovalId);
            // 聚合任务的处理结果
            if (CollectionUtils.isNotEmpty(processResult)) {
                for (ApprovalIdToTasksVO severitiesApprovalIdToTasksVO : processResult) {
                    ApprovalIdToTasksVO approvalIdToTasksVO = ignoreApprovalIdToTasks.computeIfAbsent(
                            severitiesApprovalIdToTasksVO.getApprovalId(),
                            key -> new ApprovalIdToTasksVO(severitiesApprovalIdToTasksVO.getApprovalId(),
                                    severitiesApprovalIdToTasksVO.getDefectMatchId(), new HashSet<>(), 0L));
                    approvalIdToTasksVO.getTaskIds().addAll(severitiesApprovalIdToTasksVO.getTaskIds());
                    approvalIdToTasksVO.incDefectCount(severitiesApprovalIdToTasksVO.getTotalMatchDefectCount());
                }
            }
        }
        List<ApprovalIdToTasksVO> approvalIdToTasksVOS = new ArrayList<>(ignoreApprovalIdToTasks.values());
        return new ApprovalProcessVO(approvalIdToTasksVOS, approvalConfigs,
                approvalIdToTasksVOS.stream().mapToLong(ApprovalIdToTasksVO::getTotalMatchDefectCount).sum());
    }


    private ApprovalProcessVO processBizWithNoSelectAll(Set<Long> taskIds,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        log.info("process ignore approval with no select all {}", batchDefectProcessReqVO.getProjectId());
        // 获取项目下忽略审批配置，这里会根据维度与严重程度过滤不适合的忽略配置
        List<IgnoreApprovalConfigVO> approvalConfigs = ignoreApprovalService.getProjectMatchConfig(
                batchDefectProcessReqVO.getProjectId(),
                batchDefectProcessReqVO.getIgnoreReasonType(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        // 没有配置忽略审核规则，返回
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            log.warn("process ignore approvalConfigs is empty : {} ", batchDefectProcessReqVO.getProjectId());
            return new ApprovalProcessVO(Collections.emptyList(), Collections.emptyList(), 0L);
        }

        // 获取告警
        List<LintDefectV2Entity> defectList =
                (List<LintDefectV2Entity>) getEffectiveDefectByDefectKeySet(batchDefectProcessReqVO,
                        new ArrayList<>(taskIds));
        // 告警为空，也返回
        if (CollectionUtils.isEmpty(defectList)) {
            log.warn("process ignore defectList is empty : {} ", batchDefectProcessReqVO.getProjectId());
            return new ApprovalProcessVO(Collections.emptyList(), Collections.emptyList(), 0L);
        }

        String opsId = UUID.randomUUID().toString().replace("-", "");
        // 获取规则配置
        Map<String, CheckerDetailListQueryReqVO.ToolCheckers> toolNameCheckers = new HashMap<>();
        for (LintDefectV2Entity defect : defectList) {
            toolNameCheckers.computeIfAbsent(defect.getToolName(),
                            key -> new CheckerDetailListQueryReqVO.ToolCheckers(defect.getToolName(), new HashSet<>()))
                    .getCheckerList().add(defect.getChecker());
        }
        List<CheckerDetailVO> checkerDetailVOS = checkerService.queryCheckerDetailList(
                new CheckerDetailListQueryReqVO(new ArrayList<>(toolNameCheckers.values())));
        Map<String, CheckerDetailVO> checkerKeyToDetails = checkerDetailVOS.stream().collect(
                Collectors.toMap(it -> it.getToolName() + "_" + it.getCheckerKey(), Function.identity(), (k1, k2) -> k1)
        );
        // 获取任务信息，如果是BG安全审核，需要使用BG信息
        List<Long> realTaskIds = defectList.stream().map(LintDefectV2Entity::getTaskId).collect(Collectors.toList());
        Map<Long, TaskDetailVO> taskMap = thirdPartySystemCaller.geTaskInfoTaskIds(realTaskIds).stream()
                .collect(Collectors.toMap(TaskDetailVO::getTaskId, Function.identity(), (o1, o2) -> o2));

        // 开始分析审批单信息
        Map<String, String> matchIdToApprovalId = new HashMap<>();
        Map<String, ApprovalIdToTasksVO> ignoreApprovalIdToTasks = new HashMap<>();
        Map<String, Map<Long, List<String>>> approvalIdToTaskDefects = new HashMap<>();
        for (LintDefectV2Entity defect : defectList) {
            trackSingleDefect(defect, taskMap, checkerKeyToDetails, approvalConfigs, opsId,
                    matchIdToApprovalId, approvalIdToTaskDefects, ignoreApprovalIdToTasks);
        }
        if (!approvalIdToTaskDefects.isEmpty()) {
            doBizByPage(approvalIdToTaskDefects);
        }
        List<ApprovalIdToTasksVO> approvalIdToTasksVOS = new ArrayList<>(ignoreApprovalIdToTasks.values());
        long sum = approvalIdToTasksVOS.stream().mapToLong(ApprovalIdToTasksVO::getTotalMatchDefectCount).sum();
        log.info("process ignore approval with no select all end {} {}", batchDefectProcessReqVO.getProjectId(), sum);
        return new ApprovalProcessVO(approvalIdToTasksVOS, approvalConfigs, sum);
    }

    private void doAfterAllTaskDone(
            List<ApprovalIdToTasksVO> ignoreApprovalIdToTasks,
            BatchDefectProcessReqVO batchDefectProcessReqVO,
            List<IgnoreApprovalConfigVO> approvalConfigs

    ) {
        log.info("process ignore approval doAfterAllTaskDone start: {}", batchDefectProcessReqVO.getProjectId());
        // 发送消息，开始处理提交审核单
        Map<String, IgnoreApprovalConfigVO> idToApprovalDetail = approvalConfigs.stream().collect(
                Collectors.toMap(IgnoreApprovalConfigVO::getEntityId, Function.identity(), (k1, k2) -> k1)
        );

        IgnoreTypeProjectConfigVO ignoreType = ignoreTypeService.ignoreTypeProjectDetail(
                batchDefectProcessReqVO.getProjectId(), batchDefectProcessReqVO.getUserName(),
                batchDefectProcessReqVO.getIgnoreReasonType());

        for (ApprovalIdToTasksVO ignoreApprovalIdToTask : ignoreApprovalIdToTasks) {
            String ignoreApprovalId = ignoreApprovalIdToTask.getApprovalId();
            String defectMatchId = ignoreApprovalIdToTask.getDefectMatchId();
            String ignoreConfigId = Arrays.stream(defectMatchId.split("\\|")).findFirst().orElse(null);
            if (StringUtils.isEmpty(ignoreConfigId) || !idToApprovalDetail.containsKey(ignoreConfigId)) {
                log.warn("process ignore approval {} not exist", ignoreConfigId);
                continue;
            }
            IgnoreApprovalConfigVO ignoreApprovalConfigVO = idToApprovalDetail.get(ignoreConfigId);
            rabbitTemplate.convertAndSend(
                    EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL,
                    ROUTE_CODECC_DEFECT_IGNORE_APPROVAL,
                    new DefectIgnoreApprovalVO(
                            ignoreApprovalId,
                            defectMatchId,
                            ignoreApprovalConfigVO.getEntityId(),
                            batchDefectProcessReqVO.getIgnoreReasonType(),
                            ignoreType == null ? ComConstants.EMPTY_STRING : ignoreType.getName(),
                            batchDefectProcessReqVO.getIgnoreReason(),
                            batchDefectProcessReqVO.getProjectId(),
                            batchDefectProcessReqVO.getUserName(),
                            ignoreApprovalIdToTask.getTaskIds(),
                            ignoreApprovalIdToTask.getTotalMatchDefectCount(),
                            false
                    )
            );
            log.info("process ignore approval doAfterAllTaskDone end: {}", batchDefectProcessReqVO.getProjectId());
        }
    }

    /**
     * 获取任务匹配的忽略配置
     *
     * @return
     */
    private List<IgnoreApprovalConfigVO> getTaskMatchIgnoreApprovalConfig(
            Long taskId, List<IgnoreApprovalConfigVO> approvalConfigs
    ) {
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            return Collections.emptyList();
        }
        return approvalConfigs.stream().filter(approvalConfig -> {
            TaskScopeType taskScopeType = TaskScopeType.getByType(approvalConfig.getTaskScopeType());
            if (taskScopeType == null) {
                return false;
            } else if (taskScopeType == TaskScopeType.ALL) {
                return true;
            } else if (taskScopeType == TaskScopeType.INCLUDE) {
                return approvalConfig.getTaskScopeList().contains(taskId);
            } else if (taskScopeType == TaskScopeType.EXCLUDE) {
                return !approvalConfig.getTaskScopeList().contains(taskId);
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 获取任务匹配的忽略配置
     *
     * @return
     */
    private Map<Long, List<IgnoreApprovalConfigVO>> getTaskMatchIgnoreApprovalConfig(
            Set<Long> taskIds,
            List<IgnoreApprovalConfigVO> approvalConfigs
    ) {
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(approvalConfigs)) {
            return Collections.emptyMap();
        }
        Map<Long, List<IgnoreApprovalConfigVO>> taskToApprovalConfigs = new HashMap<>();
        for (Long taskId : taskIds) {
            taskToApprovalConfigs.put(taskId, getTaskMatchIgnoreApprovalConfig(taskId, approvalConfigs));
        }
        return taskToApprovalConfigs;
    }

    /**
     * 获取任务匹配的忽略配置
     *
     * @return
     */
    private Map<Set<String>, Map<Long, List<String>>> getSeveritiesToTaskToolMap(
            Map<Long, List<IgnoreApprovalConfigVO>> taskToApprovalConfigs,
            DefectQueryReqVO queryCondObj,
            List<CheckerDetailVO> checkers
    ) {
        if (taskToApprovalConfigs == null || taskToApprovalConfigs.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> severities = CollectionUtils.isNotEmpty(queryCondObj.getSeverity()) ? queryCondObj.getSeverity() :
                Sets.newHashSet(String.valueOf(ComConstants.SERIOUS), String.valueOf(ComConstants.NORMAL),
                        String.valueOf(ComConstants.PROMPT));
        Map<String, Map<Long, List<String>>> severityToTaskToolMap = new HashMap<>();
        for (Map.Entry<Long, List<IgnoreApprovalConfigVO>> taskToApprovalConfig : taskToApprovalConfigs.entrySet()) {
            for (String severity : severities) {
                List<String> seriousTaskTools = getTaskMatchToolList(severity, taskToApprovalConfig.getValue(),
                        checkers, queryCondObj.getDimensionList(), queryCondObj.getToolNameList());
                if (CollectionUtils.isNotEmpty(seriousTaskTools)) {
                    severityToTaskToolMap.computeIfAbsent(severity, key -> new HashMap<>())
                            .put(taskToApprovalConfig.getKey(), seriousTaskTools);
                }
            }
        }
        if (severityToTaskToolMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Set<String>, Map<Long, List<String>>> severitiesToTaskToolMap = new HashMap<>();
        for (Map.Entry<String, Map<Long, List<String>>> severityToTaskTool : severityToTaskToolMap.entrySet()) {
            Optional<Set<String>> existSameValue = severitiesToTaskToolMap.entrySet().stream().filter(it ->
                            MapUtil.areMapsEqual(it.getValue(), severityToTaskTool.getValue()))
                    .map(Map.Entry::getKey).findFirst();
            if (existSameValue.isPresent()) {
                existSameValue.get().add(severityToTaskTool.getKey());
            } else {
                severitiesToTaskToolMap.put(Sets.newHashSet(severityToTaskTool.getKey()),
                        severityToTaskTool.getValue());
            }
        }
        return severitiesToTaskToolMap;
    }

    private List<String> getTaskMatchToolList(String severity, List<IgnoreApprovalConfigVO> approvalConfig,
            List<CheckerDetailVO> checkers, List<String> dimensions, List<String> toolNames) {
        List<String> approvalDimensions = approvalConfig.stream().filter(it ->
                        it.getSeverities().contains(Integer.valueOf(severity)))
                .map(IgnoreApprovalConfigVO::getDimensions)
                .flatMap(Collection::stream).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(approvalDimensions)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(dimensions)) {
            approvalDimensions.retainAll(dimensions);
        }
        if (CollectionUtils.isEmpty(approvalDimensions)) {
            return Collections.emptyList();
        }
        List<String> checkerDimensions =
                ParamUtils.getCheckerCategoryListByDimensionList(approvalDimensions);
        List<String> dimensionToolNames = checkers.stream().filter(
                it -> checkerDimensions.contains(it.getCheckerCategory())
        ).map(CheckerDetailVO::getToolName).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toolNames)) {
            dimensionToolNames.retainAll(toolNames);
        }
        return dimensionToolNames;
    }

    private List<ApprovalIdToTasksVO> processDefect(Map<Long, List<String>> taskToolMap,
            Set<String> severities, DefectQueryReqVO queryCondObj, String opsId, List<CheckerDetailVO> checkers,
            List<IgnoreApprovalConfigVO> approvalConfigs, Map<String, String> matchIdToApprovalId) {
        log.info("process ignore processDefect {} severities : {} ", opsId, severities);
        // 先将忽略配置的维度与严重程度合并
        DefectQueryReqVO taskQueryCondObj = new DefectQueryReqVO();
        BeanUtils.copyProperties(queryCondObj, taskQueryCondObj);
        taskQueryCondObj.setSeverity(severities);
        Map<String, CheckerDetailVO> checkerKeyToDetails = checkers.stream().collect(
                Collectors.toMap(it -> it.getToolName() + "_" + it.getCheckerKey(), Function.identity(), (k1, k2) -> k1)
        );
        // 获取相同包id下的规则集合
        List<String> toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        Set<String> pkgChecker = checkers.stream().filter(it -> toolNameList.contains(it.getToolName()))
                .map(CheckerDetailVO::getCheckerKey).collect(Collectors.toSet());
        // 获取任务信息，如果是BG安全审核，需要使用BG信息
        Map<Long, TaskDetailVO> taskMap = thirdPartySystemCaller.geTaskInfoTaskIds(taskToolMap.keySet()).stream()
                .collect(Collectors.toMap(TaskDetailVO::getTaskId, Function.identity(), (o1, o2) -> o2));

        Map<String, ApprovalIdToTasksVO> ignoreApprovalIdToTasks = new HashMap<>();
        List pageDefectList;
        // 起始的FilePath 与 跳过的数量
        String startFilePath = null;
        Long skip = 0L;
        int pageSize = 10000;
        do {
            // 分页获取，使用条件过滤加SKIP，避免出现深度分页现象
            pageDefectList = getDefectsByQueryCondWithPage(taskToolMap, taskQueryCondObj.getBuildId(), pkgChecker,
                    taskQueryCondObj, startFilePath, skip, pageSize);
            if (CollectionUtils.isEmpty(pageDefectList)) {
                break;
            }
            // 获取下一页信息
            Pair<Optional<String>, Long> skipPair = getStartFilePathWithNotSkip(pageDefectList, startFilePath, skip);
            startFilePath = skipPair.getFirst().isPresent() ? skipPair.getFirst().get() : null;
            skip = skipPair.getSecond();
            Map<String, Map<Long, List<String>>> approvalIdToTaskDefects = new HashMap<>();
            for (LintDefectV2Entity defect : (List<LintDefectV2Entity>) pageDefectList) {
                trackSingleDefect(defect, taskMap, checkerKeyToDetails, approvalConfigs, opsId,
                        matchIdToApprovalId, approvalIdToTaskDefects, ignoreApprovalIdToTasks);
            }
            if (!approvalIdToTaskDefects.isEmpty()) {
                doBizByPage(approvalIdToTaskDefects);
            }
        } while (pageDefectList.size() == pageSize);
        Long sum = ignoreApprovalIdToTasks.values().stream()
                .mapToLong(ApprovalIdToTasksVO::getTotalMatchDefectCount).sum();
        log.info("processTaskDefect end {} {} {} ", opsId, severities, sum);
        return new ArrayList<>(ignoreApprovalIdToTasks.values());
    }

    private void trackSingleDefect(LintDefectV2Entity defect, Map<Long, TaskDetailVO> taskMap,
            Map<String, CheckerDetailVO> checkerKeyToDetails,
            List<IgnoreApprovalConfigVO> approvalConfigs,
            String opsId,
            Map<String, String> matchIdToApprovalId,
            Map<String, Map<Long, List<String>>> approvalIdToTaskDefects,
            Map<String, ApprovalIdToTasksVO> ignoreApprovalIdToTasks) {
        // 获取缺陷匹配ID
        String defectMatchId = getDefectMatchIgnoreId(defect, taskMap.get(defect.getTaskId()),
                checkerKeyToDetails, approvalConfigs, opsId);
        if (StringUtils.isEmpty(defectMatchId)) {
            return;
        }
        // 生成或获取审批ID
        String approvalId = matchIdToApprovalId.computeIfAbsent(defectMatchId,
                key -> UUID.randomUUID().toString().replace("-", ""));
        // 更新审批ID到任务缺陷的映射，用于后续更新
        approvalIdToTaskDefects.computeIfAbsent(approvalId, key -> new HashMap<>())
                .computeIfAbsent(defect.getTaskId(), key -> Lists.newArrayList())
                .add(defect.getEntityId());
        // 统计信息更新
        ApprovalIdToTasksVO approvalIdToTasksVO = ignoreApprovalIdToTasks.computeIfAbsent(approvalId,
                key -> new ApprovalIdToTasksVO(approvalId, defectMatchId, new HashSet<>(), 0L));
        approvalIdToTasksVO.getTaskIds().add(defect.getTaskId());
        approvalIdToTasksVO.incDefectCount();
    }

    private void doBizByPage(Map<String, Map<Long, List<String>>> approvalIdToTaskDefects) {
        approvalIdToTaskDefects.forEach((ignoreApprovalId, taskToDefectIds) -> {
            defectDao.batchUpdateApprovalInfo(taskToDefectIds, ignoreApprovalId, ApproverStatus.SEND_TO_QUEUE.status());
        });
    }

    /**
     * 获取匹配的告警数量，并返回限制数量的告警
     *
     * @param batchDefectProcessReqVO
     * @return
     */
    public PreIgnoreApprovalCheckVO getMatchDefectCountAndDefectLimit(
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<Long> taskIds = getProjectBatchTaskList(batchDefectProcessReqVO);
        if (CollectionUtils.isEmpty(taskIds)) {
            return new PreIgnoreApprovalCheckVO(0L, Collections.emptyList());
        }
        // 校验权限
        validOpsPermission(batchDefectProcessReqVO.getUserName(), batchDefectProcessReqVO.getProjectId(),
                new LinkedList<>(taskIds));

        boolean isSelectAll = ComConstants.CommonJudge.COMMON_Y.value()
                .equalsIgnoreCase(batchDefectProcessReqVO.getIsSelectAll());

        if (isSelectAll) {
            return getMatchDefectCountAndDefectLimitWithSelectAll(taskIds, batchDefectProcessReqVO);
        } else {
            return getMatchDefectCountAndDefectLimitWithNoSelectAll(taskIds, batchDefectProcessReqVO);
        }
    }

    private PreIgnoreApprovalCheckVO getMatchDefectCountAndDefectLimitWithSelectAll(Set<Long> taskIds,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 获取查询配置
        DefectQueryReqVO queryCondObj = getDefectQueryReqVO(batchDefectProcessReqVO);
        queryCondObj.setDimensionList(ParamUtils.allDimensionIfEmptyForLint(queryCondObj.getDimensionList()));
        // 获取项目下忽略审批配置，这里会根据维度与严重程度过滤不适合的忽略配置
        List<IgnoreApprovalConfigVO> approvalConfigs = ignoreApprovalService.getProjectMatchConfig(
                batchDefectProcessReqVO.getProjectId(),
                batchDefectProcessReqVO.getIgnoreReasonType(),
                queryCondObj.getDimensionList(),
                CollectionUtils.isEmpty(queryCondObj.getSeverity()) ? null
                        : queryCondObj.getSeverity().stream().map(Integer::valueOf).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            return new PreIgnoreApprovalCheckVO(0L, Collections.emptyList());
        }
        // 获取规则信息
        LintQueryWarningSpecialService lintSpecialService =
                SpringContextUtil.Companion.getBean(LintQueryWarningSpecialService.class);
        List<CheckerDetailVO> checkers = lintSpecialService.getCheckerDetails(
                queryCondObj.getCheckerSet(), queryCondObj.getChecker(),
                queryCondObj.getToolNameList(), queryCondObj.getDimensionList()
        );
        // 先按任务分组，匹配符合的忽略配置
        Map<Long, List<IgnoreApprovalConfigVO>> taskToApprovalConfigs =
                getTaskMatchIgnoreApprovalConfig(taskIds, approvalConfigs);
        // 再按照严重程度分组
        Map<Set<String>, Map<Long, List<String>>> severitiesToTaskToolMaps = getSeveritiesToTaskToolMap(
                taskToApprovalConfigs, queryCondObj, checkers);
        long totalMatchDefectCount = 0L;
        int limit = DEFAULT_PRE_APPROVAL_DEFECT_LIMIT;
        List<LintDefectVO> vos = new ArrayList<>();
        for (Map.Entry<Set<String>, Map<Long, List<String>>> severitiesMap : severitiesToTaskToolMaps.entrySet()) {
            Set<String> severities = severitiesMap.getKey();
            Map<Long, List<String>> taskToolMap = severitiesMap.getValue();
            if (taskToolMap == null || taskToolMap.isEmpty()) {
                continue;
            }
            Pair<Long, List<LintDefectVO>> processResult = doGetDefectWithLimit(taskToolMap, severities, queryCondObj,
                    checkers, approvalConfigs, limit);
            // 聚合任务的处理结果
            if (processResult.getFirst() > 0) {
                totalMatchDefectCount += processResult.getFirst();
            }
            if (CollectionUtils.isNotEmpty(processResult.getSecond())) {
                vos.addAll(processResult.getSecond());
                limit = limit - processResult.getSecond().size();
            }
        }
        return new PreIgnoreApprovalCheckVO(totalMatchDefectCount, vos);
    }


    private PreIgnoreApprovalCheckVO getMatchDefectCountAndDefectLimitWithNoSelectAll(Set<Long> taskIds,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 获取告警
        List<LintDefectV2Entity> defectList =
                (List<LintDefectV2Entity>) getEffectiveDefectByDefectKeySet(batchDefectProcessReqVO,
                        new ArrayList<>(taskIds));
        if (CollectionUtils.isEmpty(defectList)) {
            return new PreIgnoreApprovalCheckVO(0L, Collections.emptyList());
        }
        // 获取规则配置
        Map<String, CheckerDetailListQueryReqVO.ToolCheckers> toolNameCheckers = new HashMap<>();
        for (LintDefectV2Entity defect : defectList) {
            toolNameCheckers.computeIfAbsent(defect.getToolName(),
                            key -> new CheckerDetailListQueryReqVO.ToolCheckers(defect.getToolName(), new HashSet<>()))
                    .getCheckerList().add(defect.getChecker());
        }
        List<CheckerDetailVO> checkerDetailVOS = checkerService.queryCheckerDetailList(
                new CheckerDetailListQueryReqVO(new ArrayList<>(toolNameCheckers.values())));
        Map<String, CheckerDetailVO> checkerKeyToDetails = checkerDetailVOS.stream().collect(
                Collectors.toMap(it -> it.getToolName() + "_" + it.getCheckerKey(), Function.identity(), (k1, k2) -> k1)
        );
        // 获取项目下忽略审批配置，这里会根据维度与严重程度过滤不适合的忽略配置
        List<IgnoreApprovalConfigVO> approvalConfigs = ignoreApprovalService.getProjectMatchConfig(
                batchDefectProcessReqVO.getProjectId(),
                batchDefectProcessReqVO.getIgnoreReasonType(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            return new PreIgnoreApprovalCheckVO(0L, Collections.emptyList());
        }
        List<LintDefectVO> defectVOList = new ArrayList<>();
        for (LintDefectV2Entity defect : defectList) {
            LintDefectVO defectVO = new LintDefectVO();
            BeanUtils.copyProperties(defect, defectVO);
            CheckerDetailVO defectChecker =
                    checkerKeyToDetails.get(defect.getToolName() + "_" + defect.getChecker());
            IgnoreApprovalConfigVO configVO = getDefectMatchConfig(defect, defectChecker, approvalConfigs);
            if (configVO == null) {
                continue;
            }
            defectVO.setIgnoreApproverTypes(configVO.getApproverTypes());
            defectVO.setCustomIgnoreApprovers(configVO.getCustomApprovers());
            defectVO.setSeverity(defectVO.getSeverity() == ComConstants.PROMPT_IN_DB ? ComConstants.PROMPT
                    : defectVO.getSeverity()
            );
            defectVOList.add(defectVO);
            if (defectVOList.size() >= DEFAULT_PRE_APPROVAL_DEFECT_LIMIT) {
                break;
            }
        }
        return new PreIgnoreApprovalCheckVO((long) defectVOList.size(), defectVOList);
    }

    private Pair<Long, List<LintDefectVO>> doGetDefectWithLimit(Map<Long, List<String>> taskToolMap,
            Set<String> severities, DefectQueryReqVO queryCondObj, List<CheckerDetailVO> checkers,
            List<IgnoreApprovalConfigVO> approvalConfigs, Integer limit) {
        // 先将忽略配置的维度与严重程度合并
        DefectQueryReqVO taskQueryCondObj = new DefectQueryReqVO();
        BeanUtils.copyProperties(queryCondObj, taskQueryCondObj);
        taskQueryCondObj.setSeverity(severities);
        Map<String, CheckerDetailVO> checkerKeyToDetails = checkers.stream().collect(
                Collectors.toMap(it -> it.getToolName() + "_" + it.getCheckerKey(), Function.identity(), (k1, k2) -> k1)
        );
        // 获取相同包id下的规则集合
        List<String> toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        Set<String> pkgChecker = checkers.stream().filter(it -> toolNameList.contains(it.getToolName()))
                .map(CheckerDetailVO::getCheckerKey).collect(Collectors.toSet());

        Long count = getDefectMatchCount(taskToolMap, taskQueryCondObj.getBuildId(), pkgChecker,
                taskQueryCondObj);

        if (count > 0 && limit > 0) {
            taskQueryCondObj.setNeedBatchInsert(true);
            List defectList = getDefectsByQueryCondWithPage(taskToolMap, taskQueryCondObj.getBuildId(), pkgChecker,
                    taskQueryCondObj, null, 0L, limit);
            List<LintDefectVO> defectVOList = new ArrayList<>();
            for (LintDefectV2Entity defect : (List<LintDefectV2Entity>) defectList) {
                LintDefectVO defectVO = new LintDefectVO();
                BeanUtils.copyProperties(defect, defectVO);
                CheckerDetailVO defectChecker =
                        checkerKeyToDetails.get(defect.getToolName() + "_" + defect.getChecker());
                IgnoreApprovalConfigVO configVO = getDefectMatchConfig(defect, defectChecker, approvalConfigs);
                if (configVO == null) {
                    continue;
                }
                defectVO.setIgnoreApproverTypes(configVO.getApproverTypes());
                defectVO.setCustomIgnoreApprovers(configVO.getCustomApprovers());
                defectVO.setSeverity(defectVO.getSeverity() == ComConstants.PROMPT_IN_DB ? ComConstants.PROMPT
                        : defectVO.getSeverity()
                );
                defectVOList.add(defectVO);
            }
            return Pair.of(count, defectVOList);
        } else {
            return Pair.of(count, Collections.emptyList());
        }
    }

    /**
     * 获取告警符合的忽略配置
     *
     * @param defect
     * @param checkerKeyToDetails
     * @param taskApprovalConfigs
     * @param opsId
     * @return
     */
    private String getDefectMatchIgnoreId(
            LintDefectV2Entity defect,
            TaskDetailVO taskDetailVO,
            Map<String, CheckerDetailVO> checkerKeyToDetails,
            List<IgnoreApprovalConfigVO> taskApprovalConfigs,
            String opsId
    ) {
        if (defect == null || checkerKeyToDetails == null || checkerKeyToDetails.isEmpty()
                || !checkerKeyToDetails.containsKey(defect.getToolName() + "_" + defect.getChecker())
                || CollectionUtils.isEmpty(taskApprovalConfigs)) {
            return null;
        }
        CheckerDetailVO defectChecker = checkerKeyToDetails.get(defect.getToolName() + "_" + defect.getChecker());
        IgnoreApprovalConfigVO approvalConfigVO = getDefectMatchConfig(defect, defectChecker, taskApprovalConfigs);
        if (approvalConfigVO == null) {
            return null;
        }
        // 格式如下：CONFIG_ID|TYPE1|TYPE2:TYPE_PARAM|TYPE3|...|OPS_ID
        StringBuilder defectMatchIdStr = new StringBuilder(approvalConfigVO.getEntityId() + "|");
        ApproverType.getSortedTypeList().forEach(approverType -> {
            if (!approvalConfigVO.getApproverTypes().contains(approverType)) {
                return;
            }
            defectMatchIdStr.append(approverType);
            if (ApproverType.CHECKER_PUBLISHER.type().equals(approverType)) {
                // 规则发布者，需要记录发布者，不同的发布者，需要发不同的单
                defectMatchIdStr.append(SEPARATOR_SEMICOLON).append(defectChecker.getPublisher());
            } else if (ApproverType.TASK_MANAGER.type().equals(approverType)) {
                // 任务管理员，记录任务ID
                defectMatchIdStr.append(SEPARATOR_SEMICOLON).append(defect.getTaskId());
            } else if (ApproverType.BG_SECURITY_MANAGER.type().equals(approverType)) {
                // BG管理员，记录BgID
                Integer bgId = taskDetailVO == null ? -1 : taskDetailVO.getBgId();
                Integer businessLineId = taskDetailVO == null || taskDetailVO.getBusinessLineId() == null ? -1
                        : taskDetailVO.getBusinessLineId();
                Integer deptId = taskDetailVO == null ? -1 : taskDetailVO.getDeptId();
                defectMatchIdStr.append(SEPARATOR_SEMICOLON).append(Stream.of(bgId, businessLineId, deptId)
                        .map(String::valueOf).collect(Collectors.joining(KEY_UNDERLINE)));
            }
            defectMatchIdStr.append(STRING_DELIMITER);
        });
        defectMatchIdStr.append(opsId);
        return defectMatchIdStr.toString();
    }

    /**
     * 获取告警符合的忽略配置
     *
     * @param defect
     * @param checker
     * @param approvalConfigs
     * @return
     */
    private IgnoreApprovalConfigVO getDefectMatchConfig(
            LintDefectV2Entity defect,
            CheckerDetailVO checker,
            List<IgnoreApprovalConfigVO> approvalConfigs
    ) {
        if (defect == null || checker == null || CollectionUtils.isEmpty(approvalConfigs)) {
            return null;
        }
        List<IgnoreApprovalConfigVO> taskApprovalConfigs =
                getTaskMatchIgnoreApprovalConfig(defect.getTaskId(), approvalConfigs);
        String defectCheckerCategory = checker.getCheckerCategory();
        String dimension = ParamUtils.getDimensionByCheckerCategory(defectCheckerCategory);
        int severity = defect.getSeverity() == ComConstants.PROMPT_IN_DB ? ComConstants.PROMPT : defect.getSeverity();
        Optional<IgnoreApprovalConfigVO> approvalConfigOptional = taskApprovalConfigs.stream().filter(it ->
                        it.getDimensions().contains(dimension) && it.getSeverities().contains(severity)
                                && (it.getDefectCreateTime() == null
                                || defect.getCreateTime() > it.getDefectCreateTime()))
                .findFirst();
        return approvalConfigOptional.orElse(null);
    }

    @Override
    protected Set<String> getStatusCondition(DefectQueryReqVO queryCondObj) {
        return super.getStatusCondition(queryCondObj);
    }

    /**
     * 不使用
     *
     * @param defectList
     * @param batchDefectProcessReqVO
     */
    // NOCC:OverloadMethodsDeclarationOrder(设计如此:)
    @Override
    protected void doBizByPage(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    /**
     * 不使用
     *
     * @param batchDefectProcessReqVO
     */
    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    /**
     * 不使用
     *
     * @return
     */
    @Override
    protected Pair<BusinessType, ToolType> getBusinessTypeToolTypePair() {
        return null;
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Data
    @AllArgsConstructor
    static class ApprovalProcessVO {

        private List<ApprovalIdToTasksVO> ignoreApprovalIdToTasks;

        private List<IgnoreApprovalConfigVO> approvalConfigs;

        private Long totalMatchDefectCount;
    }

    @Data
    @AllArgsConstructor
    static class ApprovalIdToTasksVO {

        private String approvalId;

        private String defectMatchId;

        private Set<Long> taskIds;

        private Long totalMatchDefectCount;

        public void incDefectCount() {
            this.totalMatchDefectCount++;
        }

        public void incDefectCount(Long count) {
            this.totalMatchDefectCount = this.totalMatchDefectCount + count;
        }
    }

}
