package com.tencent.bk.codecc.defect.utils;

import static com.tencent.devops.common.constant.ComConstants.TOOL_LICENSE_WHITE_LIST;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigRequest;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse;
import com.tencent.bk.codecc.task.vo.TaskInfoWithSortedToolConfigResponse.TaskBase;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.FOLLOW_STATUS;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.List2StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.util.Pair;

@Slf4j
public class ParamUtils {

    private static volatile Comparator<String> TOOL_ORDER_COMPARATOR_CACHE = null;

    /**
     * 工具许可项目白名单缓存(即只有指定的项目才能使用该工具，用于某些收费工具对特定项目使用)
     */
    private static LoadingCache<String, Set<String>> toolLicenseWhiteListCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String toolName) {
                    Set<String> projectIdSet = Sets.newHashSet();
                    if (StringUtils.isNotEmpty(toolName)) {
                        List<BaseDataVO> baseDataVOS = SpringContextUtil.Companion.getBean(Client.class)
                                .get(ServiceBaseDataResource.class)
                                .getInfoByTypeAndCode(TOOL_LICENSE_WHITE_LIST, toolName).getData();
                        if (CollectionUtils.isNotEmpty(baseDataVOS)
                                && StringUtils.isNotEmpty(baseDataVOS.get(0).getParamValue())) {
                            String toolSetStr = baseDataVOS.get(0).getParamValue();
                            projectIdSet.addAll(List2StrUtil.fromString(toolSetStr, ComConstants.STRING_SPLIT));
                        }
                    }
                    return projectIdSet;
                }
            });

    /**
     * 根据前端传入条件，转换为后端工具列表 （兼容老业务，toolName以及dimension是逗号分割）
     *
     * @param toolName
     * @param dimension
     * @param taskId
     * @param buildId 快照为空时，仅返回目前在用工具；反之返回快照当时真实执行过的工具集
     * @param isDataMigrationSuccessful 数据迁移标识
     * @return
     */
    @Deprecated
    public static List<String> getTools(
            String toolName,
            String dimension,
            Long taskId,
            String buildId,
            boolean isDataMigrationSuccessful
    ) {
        List<String> toolNameList = List2StrUtil.fromString(toolName, ComConstants.STRING_SPLIT);
        List<String> dimensionList = List2StrUtil.fromString(dimension, ComConstants.STRING_SPLIT);

        return getTaskToolMap(toolNameList, dimensionList, Lists.newArrayList(taskId), buildId)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据前端传入条件，转换为任务与工具列表映射
     *
     * @param toolNameList
     * @param dimensionList
     * @param taskIdList
     * @param buildId 快照为空时，仅返回目前在用工具；反之返回快照当时真实执行过的工具集
     * @return key-> task id, value-> tool name list (工具为空集的任务会被剔除掉)
     */
    public static Map<Long, List<String>> getTaskToolMap(
            List<String> toolNameList,
            List<String> dimensionList,
            List<Long> taskIdList,
            String buildId
    ) {
        // 不传工具则校验下架情况
        boolean mustCheck = CollectionUtils.isEmpty(toolNameList);

        return getTaskToolMapCore(toolNameList, dimensionList, taskIdList, buildId, false, mustCheck);
    }

    /**
     * 前端问题管理页工具下拉框，不校验工具自身下架情况"checkToolRemoved"
     *
     * @param dimensionList
     * @param taskIdList
     * @param buildId
     * @return
     */
    public static List<String> listToolNameForFrontend(
            List<String> dimensionList,
            List<Long> taskIdList,
            String buildId
    ) {
        if (CollectionUtils.isEmpty(taskIdList)) {
            return Lists.newArrayList();
        }

        if (taskIdList.size() == 1) {
            return getTaskToolMapCore(Lists.newArrayList(), dimensionList, taskIdList, buildId, true, false)
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            // 多任务需要汇总排序
            Comparator<String> comparator = getToolOrderComparator();

            return getTaskToolMapCore(Lists.newArrayList(), dimensionList, taskIdList, buildId, false, false)
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .sorted(comparator)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 根据前端传入条件，转换为后端工具列表
     *
     * @param toolName
     * @param dimension
     * @param taskId
     * @param buildId 快照为空时，仅返回目前在用工具；反之返回所有
     * @return
     */
    @Deprecated
    public static List<String> getToolsByDimension(
            String toolName,
            String dimension,
            Long taskId,
            String buildId
    ) {
        // todo:待清理common时，再一并处理依赖
        return Lists.newArrayList();
    }


    /**
     * 数据迁移后，前端参数转换
     *
     * @param toolName 前端传入工具，多个则逗号分割
     * @param dimension 前端传入维度，多个则逗号分割
     * @return first -> toolNames, second-> dimension(不选等同全选)
     */
    public static Pair<List<String>, List<String>> parseToolNameAndDimensions(
            String toolName,
            String dimension
    ) {
        List<String> toolNames = List2StrUtil.fromString(toolName, ComConstants.STRING_SPLIT);
        List<String> dimensions = List2StrUtil.fromString(dimension, ComConstants.STRING_SPLIT);

        if (CollectionUtils.isEmpty(dimensions)) {
            dimensions = ToolType.DIMENSION_FOR_LINT_PATTERN_LIST;
        }

        return Pair.of(toolNames, dimensions);
    }

    public static List<String> allDimensionIfEmptyForLint(List<String> dimensionList) {
        if (CollectionUtils.isEmpty(dimensionList)) {
            return ToolType.DIMENSION_FOR_LINT_PATTERN_LIST;
        }

        return dimensionList;
    }

    /**
     * 若传入的任务为空，则获取该项目下所有任务Id
     *
     * @param projectId
     * @param userId
     * @return
     */
    public static List<Long> allTaskByProjectIdIfEmpty(List<Long> taskIdList, String projectId, String userId) {
        // 若任务Id列表为空，则说明是来自跨任务维度的查询且是全选
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            return taskIdList;
        }

        if (StringUtils.isEmpty(projectId) || StringUtils.isEmpty(userId)) {
            log.warn("all task by project, args can not be null: {}, {}", projectId, userId);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"projectId", "userId"});
        }

        try {
            return SpringContextUtil.Companion.getBean(Client.class)
                    .get(ServiceTaskRestResource.class)
                    .queryTaskIdByProjectIdWithPermission(projectId, userId)
                    .getData();
        } catch (Throwable t) {
            log.error("get task id list fail, project id: {}", projectId, t);
            return Lists.newArrayList();
        }
    }

    /**
     * 根据前端传入的维度信息，转为数据库维度信息
     *
     * @param dimensionList
     * @return
     */
    public static List<String> getCheckerCategoryListByDimensionList(List<String> dimensionList) {
        if (CollectionUtils.isEmpty(dimensionList)) {
            return Lists.newArrayList();
        }

        Set<String> retSet = Sets.newHashSet();

        /*===========================
         前端dimension
         代码缺陷：DEFECT
         安全漏洞：SECURITY
         代码规范：STANDARD
         圈复杂度：CCN
         ===========================*/
        for (String dimension : dimensionList) {
            switch (dimension) {
                case "DEFECT":
                    retSet.add(CheckerCategory.CODE_DEFECT.name());
                    break;
                case "SECURITY":
                    retSet.add(CheckerCategory.SECURITY_RISK.name());
                    break;
                case "STANDARD":
                    retSet.add(CheckerCategory.CODE_FORMAT.name());
                    break;
                case "CCN":
                    retSet.add(CheckerCategory.COMPLEXITY.name());
                    break;
                default:
                    break;
            }
        }

        return Lists.newArrayList(retSet);
    }

    /**
     * 根据前端传入条件，转换为后端工具列表
     *
     * @param toolNameList
     * @param dimensionList
     * @param taskIdList
     * @param buildId
     * @param needSorted 是否排序，指每个task中自身的工具排序
     * @param checkToolRemoved 是否检测被删除工具
     * @return 工具列表
     */
    private static Map<Long, List<String>> getTaskToolMapCore(
            List<String> toolNameList,
            List<String> dimensionList,
            List<Long> taskIdList,
            String buildId,
            boolean needSorted,
            boolean checkToolRemoved
    ) {
        if (CollectionUtils.isEmpty(taskIdList)) {
            return Maps.newHashMap();
        }

        // 业务校验：跨任务并不支持快照
        if (taskIdList.size() > 1 && StringUtils.isNotEmpty(buildId)) {
            throw new IllegalArgumentException("build id must be empty when task list size more than 1");
        }

        List<TaskBase> taskBaseList = getTaskInfoWithToolConfig(taskIdList, needSorted);
        if (CollectionUtils.isEmpty(taskBaseList)) {
            return Maps.newHashMap();
        }

        final Map<Long, List<String>> retMap = filterByBuildId(taskBaseList, buildId, checkToolRemoved);

        // 若前端传入工具参数，取交集
        if (!CollectionUtils.isEmpty(toolNameList)) {
            retMap.entrySet().removeIf(entry -> {
                List<String> toolNameListBySingleTask = entry.getValue();
                toolNameListBySingleTask.retainAll(toolNameList);

                return CollectionUtils.isEmpty(toolNameListBySingleTask);
            });
        }

        if (MapUtils.isEmpty(retMap)) {
            return retMap;
        }

        // 根据维度在规则中筛出对应工具，再交集
        List<String> toolNameListByAllTask = retMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        List<String> checkerCategoryList = getCheckerCategoryListByDimensionList(dimensionList);
        CheckerDetailDao checkerDetailDao = SpringContextUtil.Companion.getBean(CheckerDetailDao.class);
        List<String> toolNameListByChecker = checkerDetailDao.distinctToolNameByCheckerCategoryInAndToolNameIn(
                toolNameListByAllTask,
                checkerCategoryList
        );

        if (CollectionUtils.isEmpty(toolNameListByChecker)) {
            return Maps.newHashMap();
        }

        retMap.entrySet().removeIf(entry -> {
            List<String> toolNameListBySingleTask = entry.getValue();
            toolNameListBySingleTask.retainAll(toolNameListByChecker);

            return CollectionUtils.isEmpty(toolNameListBySingleTask);
        });

        return retMap;
    }

    /**
     * 检查工具是否已经下架
     *
     * @param toolName
     * @param taskBase
     * @return true:已下架，false:未下架
     */
    private static boolean checkToolRemoved(String toolName, TaskBase taskBase) {
        // 指定项目不限制，可以执行coverity
        try {
            Set<String> projectIdSet = toolLicenseWhiteListCache.get(toolName);
            if (CollectionUtils.isNotEmpty(projectIdSet) && projectIdSet.contains(taskBase.getProjectId())) {
                return false;
            }
        } catch (ExecutionException e) {
            log.warn("get tool license white list exception: {}, {}, {}",
                    toolName, taskBase.getProjectId(), taskBase.getTaskId());
        }

        ToolMetaCacheService toolMetaCache = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
        ToolMetaBaseVO toolMetaBase = toolMetaCache.getToolBaseMetaCache(toolName);
        if (ComConstants.ToolIntegratedStatus.D.name().equals(toolMetaBase.getStatus())) {
            // log.info("tool was removed: {}, {}", toolName, taskBase.getTaskId());
            return true;
        }

        return false;
    }

    /**
     * 获取带工具信息的task
     *
     * @param taskIdList
     * @param needSorted 是否排序，指每个task中自身的工具排序
     * @return
     */
    public static List<TaskBase> getTaskInfoWithToolConfig(
            List<Long> taskIdList,
            boolean needSorted
    ) {
        Client client = SpringContextUtil.Companion.getBean(Client.class);
        Result<TaskInfoWithSortedToolConfigResponse> resp = client.get(ServiceTaskRestResource.class)
                .getTaskInfoWithSortedToolConfig(new TaskInfoWithSortedToolConfigRequest(taskIdList, needSorted));

        if (resp.isNotOk() || resp.getData() == null) {
            String taskIdsJoinStr = taskIdList.stream().map(Object::toString).collect(Collectors.joining(", "));
            log.error("get tool config fail: {}, {}", taskIdsJoinStr, resp.getMessage());

            return Lists.newArrayList();
        }

        List<TaskBase> retList = resp.getData().getTaskBaseList();

        return retList == null ? Lists.newArrayList() : retList;
    }

    /**
     * 过滤掉没有操作权限的任务
     *
     * @param taskIdList
     * @param username
     * @return
     */
    public static List<Long> filterNoDefectOpsPermissionsTask(
            List<Long> taskIdList,
            String projectId,
            String username
    ) {
        AuthExPermissionApi authExPermissionApi = SpringContextUtil.Companion.getBean(AuthExPermissionApi.class);
        List<Long> hasPermissionsTaskIds = new LinkedList<>();
        List<TaskBase> taskBases = getTaskInfoWithToolConfig(taskIdList, false);
        Map<Long, TaskBase> taskIdToBaseMap = CollectionUtils.isNotEmpty(taskBases)
                ? taskBases.stream().collect(Collectors.toMap(TaskBase::getTaskId, it -> it)) : Collections.emptyMap();
        List<CodeCCAuthAction> actions = Lists.newArrayList(CodeCCAuthAction.DEFECT_MANAGE);
        for (Long taskId : taskIdList) {
            String createFrom = taskIdToBaseMap.get(taskId) != null && StringUtils.isNotBlank(
                    taskIdToBaseMap.get(taskId).getCreateFrom()) ? taskIdToBaseMap.get(taskId).getCreateFrom() : "";
            boolean hasPermissions = authExPermissionApi.authDefectOpsPermissions(taskId, projectId, username,
                    createFrom, actions);
            if (hasPermissions) {
                hasPermissionsTaskIds.add(taskId);
            }
        }
        return hasPermissionsTaskIds;
    }

    /**
     * 通过聚合查询告警包含的任务列表
     *
     * @param projectId
     * @param toolNameList
     * @param dimensionList
     * @param userId
     * @return
     */
    public static List<TaskBase> getDefectOrCCNTaskIdByNewDefectAggregate(String projectId, List<String> toolNameList,
            List<String> dimensionList, String userId) {
        List<Long> allProjectTaskIds = allTaskByProjectIdIfEmpty(Collections.emptyList(), projectId, userId);
        int pageSize = 1000;
        int page = allProjectTaskIds.size() / pageSize + (allProjectTaskIds.size() % pageSize != 0 ? 1 : 0);
        LintDefectV2Dao lintDefectV2Dao = SpringContextUtil.Companion.getBean(LintDefectV2Dao.class);
        CCNDefectDao ccnDefectDao = SpringContextUtil.Companion.getBean(CCNDefectDao.class);
        boolean ccn = (CollectionUtils.isNotEmpty(toolNameList) || CollectionUtils.isNotEmpty(dimensionList))
                && (toolNameList.contains(Tool.CCN.name()) || dimensionList.contains(ToolType.CCN.name()));
        Set<Long> taskIds = new HashSet<>();
        List<TaskBase> vo = new LinkedList<>();
        for (int i = 0; i < page; i++) {
            List<Long> pageTaskIds = allProjectTaskIds.subList(i * pageSize, Math.min(i * pageSize + pageSize,
                    allProjectTaskIds.size()));
            List<TaskBase> taskBases = ParamUtils.getTaskInfoWithToolConfig(pageTaskIds, false);
            if (CollectionUtils.isEmpty(taskBases)) {
                continue;
            }
            Map<Long, TaskBase> taskBasesCache = taskBases.stream()
                    .collect(Collectors.toMap(TaskBase::getTaskId, it -> it));
            // 分开代码问题与CCN
            Pair<Map<Long, Set<String>>, Set<Long>> toolMapSetPair = getDefectAndCCNTaskToToolMap(taskBases);
            List<Long> realTaskIdsInDb;
            if (ccn) {
                realTaskIdsInDb = ccnDefectDao.statisticTaskIdByNewDefect(toolMapSetPair.getSecond());
            } else {
                realTaskIdsInDb = lintDefectV2Dao.statisticTaskIdByNewDefect(toolMapSetPair.getFirst());
            }
            if (CollectionUtils.isEmpty(realTaskIdsInDb)) {
                continue;
            }
            realTaskIdsInDb.forEach(taskId -> {
                if (!taskIds.contains(taskId)) {
                    vo.add(taskBasesCache.get(taskId));
                }
            });
            taskIds.addAll(realTaskIdsInDb);
        }
        return vo;
    }

    /**
     * 通过聚合查询告警包含的任务列表
     *
     * @param defectIds
     * @param toolNameList
     * @param dimensionList
     * @return
     */
    public static List<TaskBase> getDefectOrCCNTaskIdByNewDefectAggregate(Set<String> defectIds,
            List<String> toolNameList,
            List<String> dimensionList) {
        LintDefectV2Dao lintDefectV2Dao = SpringContextUtil.Companion.getBean(LintDefectV2Dao.class);
        CCNDefectDao ccnDefectDao = SpringContextUtil.Companion.getBean(CCNDefectDao.class);
        Set<Long> taskIds = new HashSet<>();
        boolean ccn = (CollectionUtils.isNotEmpty(toolNameList) || CollectionUtils.isNotEmpty(dimensionList))
                && (toolNameList.contains(Tool.CCN.name()) || dimensionList.contains(ToolType.CCN.name()));
        if (ccn) {
            taskIds.addAll(ccnDefectDao.statisticTaskIdByNewDefectId(defectIds));
        } else {
            taskIds.addAll(lintDefectV2Dao.statisticTaskIdByNewDefectId(defectIds));
        }

        return getTaskInfoWithToolConfig(new LinkedList<>(taskIds), false);
    }

    public static Pair<Map<Long, Set<String>>, Set<Long>> getDefectAndCCNTaskToToolMapByTaskId(
            List<Long> taskIdList) {
        return getDefectAndCCNTaskToToolMap(getTaskInfoWithToolConfig(taskIdList, false));
    }

    /**
     * 获取工具与任务的关系，即哪些任务配置了哪些工具
     *
     * @param taskBases
     * @return
     */
    public static Pair<Map<Long, Set<String>>, Set<Long>> getDefectAndCCNTaskToToolMap(List<TaskBase> taskBases) {
        if (CollectionUtils.isEmpty(taskBases)) {
            return Pair.of(Collections.emptyMap(), Collections.emptySet());
        }
        ToolMetaCacheService toolMetaCache = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
        Map<Long, Set<String>> taskToDefectToolMap = new HashMap<>();
        Set<Long> hasCCNToolTasks = new HashSet<>();
        Map<Long, List<String>> taskToToolMap = filterByBuildId(taskBases, null, true);
        for (Entry<Long, List<String>> taskToToolEntry : taskToToolMap.entrySet()) {
            Long taskId = taskToToolEntry.getKey();
            List<String> toolNames = taskToToolEntry.getValue();
            Set<String> defectTools = new HashSet<>();
            for (String toolName : toolNames) {
                if (StringUtils.isEmpty(toolName)) {
                    continue;
                }
                ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
                if (toolMetaBaseVO == null || StringUtils.isBlank(toolMetaBaseVO.getType())) {
                    continue;
                }
                String toolType = toolMetaBaseVO.getType();
                if (ToolType.STANDARD.name().equals(toolType) || ToolType.SECURITY.name().equals(toolType)
                        || ToolType.DEFECT.name().equals(toolType)) {
                    defectTools.add(toolName);
                } else if (ToolType.CCN.name().equals(toolType)) {
                    hasCCNToolTasks.add(taskId);
                }
            }
            if (!defectTools.isEmpty()) {
                taskToDefectToolMap.put(taskId, defectTools);
            }
        }
        return Pair.of(taskToDefectToolMap, hasCCNToolTasks);
    }

    private static Map<Long, List<String>> filterByBuildId(
            List<TaskBase> taskBaseList,
            String buildId,
            boolean checkToolRemoved
    ) {
        BiFunction<ToolConfigInfoVO, TaskBase, Boolean> checkToolRemovedFunc = checkToolRemoved
                ? (tool, task) -> tool != null && task != null && !checkToolRemoved(tool.getToolName(), task)
                : (tool, task) -> tool != null && task != null;
        Map<Long, List<String>> retMap = Maps.newHashMap();

        // 若是普通查，任务关联的工具配置不能是下架的
        if (StringUtils.isEmpty(buildId)) {
            for (TaskBase taskBase : taskBaseList) {
                // 均为在用工具
                List<String> toolNameList = taskBase.getToolConfigInfoList().stream()
                        .filter(x -> x != null && x.getFollowStatus() != FOLLOW_STATUS.WITHDRAW.value()
                                && checkToolRemovedFunc.apply(x, taskBase))
                        .map(ToolConfigInfoVO::getToolName)
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(toolNameList)) {
                    retMap.put(taskBase.getTaskId(), toolNameList);
                }
            }
        } else {
            // 若是快照查询，task必为1个，工具则按照实际执行的来
            TaskBase taskBase = taskBaseList.get(0);
            List<String> toolNameList = taskBase.getToolConfigInfoList().stream()
                    .filter(x -> checkToolRemovedFunc.apply(x, taskBase))
                    .map(ToolConfigInfoVO::getToolName)
                    .collect(Collectors.toList());

            BuildSnapshotService buildSnapshotService = SpringContextUtil.Companion.getBean(BuildSnapshotService.class);
            BuildDefectSummaryEntity summary = buildSnapshotService.getSummary(taskBase.getTaskId(), buildId);
            // 所有配置过的工具与快照当时的交集
            if (summary != null && summary.getToolList() != null) {
                toolNameList.retainAll(summary.getToolList());
            }

            if (CollectionUtils.isNotEmpty(toolNameList)) {
                retMap.put(taskBase.getTaskId(), toolNameList);
            }
        }

        return retMap;
    }

    private static Comparator<String> getToolOrderComparator() {
        if (TOOL_ORDER_COMPARATOR_CACHE != null) {
            return TOOL_ORDER_COMPARATOR_CACHE;
        }

        Client client = SpringContextUtil.Companion.getBean(Client.class);
        String toolOrderStr = client.get(ServiceToolRestResource.class).findToolOrder().getData();
        if (StringUtils.isEmpty(toolOrderStr)) {
            return Comparator.naturalOrder();
        }

        List<String> toolOrderList = Arrays.asList(toolOrderStr.split(ComConstants.STRING_SPLIT));
        TOOL_ORDER_COMPARATOR_CACHE =
                Comparator.comparing(x -> toolOrderList.contains(x) ? toolOrderList.indexOf(x) : Integer.MAX_VALUE);

        return TOOL_ORDER_COMPARATOR_CACHE;
    }

    /**
     * 不同阶段的工具，能看到的规则版本不同
     */
    public static List<Integer> getCheckerVersionListByToolStatus(int toolStatus) {
        List<Integer> versionList;
        // 正式项目  toolIntegratedStatus只能筛选比自己value()更大的version
        if (toolStatus == ToolIntegratedStatus.P.value()) {
            versionList = Arrays.asList(
                    ToolIntegratedStatus.P.value(),
                    null);
        } else if (toolStatus == ToolIntegratedStatus.T.value()) {
            // 测试项目应该可以看到测试、灰度、预发布、发布的规则并进行配置
            versionList =  Arrays.asList(
                    ToolIntegratedStatus.T.value(),
                    ToolIntegratedStatus.G.value(),
                    ToolIntegratedStatus.PRE_PROD.value(),
                    ToolIntegratedStatus.P.value(),
                    null);
        } else if (toolStatus == ToolIntegratedStatus.G.value()) {
            // 灰度项目应该可以看到灰度、预发布、发布的规则并进行配置
            versionList = Arrays.asList(
                    ToolIntegratedStatus.G.value(),
                    ToolIntegratedStatus.PRE_PROD.value(),
                    ToolIntegratedStatus.P.value(),
                    null);
        } else {
            versionList = new ArrayList<>(Sets.newHashSet(
                    toolStatus,
                    ToolIntegratedStatus.P.value(),
                    null));
        }
        return versionList;
    }
}
