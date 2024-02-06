package com.tencent.bk.codecc.codeccjob.service.impl;

import static com.tencent.devops.common.constant.ComConstants.TOOL_LICENSE_WHITE_LIST;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.CheckerDetailRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskPersonalStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.TaskPersonalStatisticDao;
import com.tencent.bk.codecc.codeccjob.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.TaskPersonalStatisticEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.List2StrUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskPersonalStatisticServiceImpl implements TaskPersonalStatisticService {

    // NOCC:MemberName(设计如此:)
    private final int DEFECT_MAX_PAGE = 200;
    // NOCC:MemberName(设计如此:)
    private final int DEFECT_PAGE_SIZE = 10000;
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private TaskPersonalStatisticRepository taskPersonalStatisticRepository;
    @Autowired
    private TaskPersonalStatisticDao taskPersonalStatisticDao;
    @Autowired
    private Client client;
    @Autowired
    private CheckerDetailRepository checkerDetailRepository;

    /**
     * 工具许可项目白名单缓存(即只有指定的项目才能使用该工具，用于某些收费工具对特定项目使用)
     */
    private LoadingCache<String, Set<String>> toolLicenseWhiteListCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                @Override
                public Set<String> load(String toolName) {
                    Set<String> projectIdSet = Sets.newHashSet();
                    if (StringUtils.isNotEmpty(toolName)) {
                        List<BaseDataVO> baseDataVOS = client.get(ServiceBaseDataResource.class)
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

    @Override
    public void refresh(Long taskId, String extraInfo) {
        Map<String, TaskPersonalStatisticEntity> taskPersonalStatisticMap = new HashMap<>();

        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
        if (taskInfo == null) {
            log.error("task info is null, task id: {}", taskId);
            return;
        }

        // 停用了的工具不再统计
        List<String> taskToolNameList = taskInfo.getToolConfigInfoList().stream()
                .filter(x ->
                        x != null && x.getFollowStatus() != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
                                && !checkToolRemoved(x.getToolName(), taskInfo))
                .map(ToolConfigInfoVO::getToolName)
                .collect(Collectors.toList());

        Pair<List<String>, List<String>> pairForDefectDimension = getToolNamesAndCheckerKeysPairByDimension(
                CheckerCategory.CODE_DEFECT.name(),
                taskToolNameList
        );
        List<String> defectTools = pairForDefectDimension.getFirst();
        List<String> defectCheckerKeys = pairForDefectDimension.getSecond();

        if (CollectionUtils.isNotEmpty(defectTools)) {
            log.info("start to get overview defect count for task: {}, tools: {}", taskId, defectTools);
            lintDefectV2Dao.findStatisticGroupByAuthor(taskId, defectTools, defectCheckerKeys).forEach(
                    agg -> {
                        String author = ToolParamUtils.trimUserName(agg.getAuthorName());
                        TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);

                        if (entity == null) {
                            entity = new TaskPersonalStatisticEntity();
                            entity.setTaskId(taskId);
                            entity.setUsername(author);
                            taskPersonalStatisticMap.put(author, entity);
                        }

                        entity.setDefectCount(entity.getDefectCount() + agg.getDefectCount());
                    }
            );
        }

        Pair<List<String>, List<String>> pairForSecurityDimension = getToolNamesAndCheckerKeysPairByDimension(
                CheckerCategory.SECURITY_RISK.name(),
                taskToolNameList
        );
        List<String> securityTools = pairForSecurityDimension.getFirst();
        List<String> securityCheckerKeys = pairForSecurityDimension.getSecond();

        if (CollectionUtils.isNotEmpty(securityTools)) {
            log.info("start to get overview security count for task: {}, tools: {}", taskId, securityTools);

            lintDefectV2Dao.findStatisticGroupByAuthor(taskId, securityTools, securityCheckerKeys).forEach(
                    agg -> {
                        String author = ToolParamUtils.trimUserName(agg.getAuthorName());
                        TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);

                        if (entity == null) {
                            entity = new TaskPersonalStatisticEntity();
                            entity.setTaskId(taskId);
                            entity.setUsername(author);
                            taskPersonalStatisticMap.put(author, entity);
                        }

                        entity.setSecurityCount(entity.getSecurityCount() + agg.getDefectCount());
                    }
            );
        }

        Pair<List<String>, List<String>> pairForStandardDimension = getToolNamesAndCheckerKeysPairByDimension(
                CheckerCategory.CODE_FORMAT.name(),
                taskToolNameList
        );
        List<String> standardTools = pairForStandardDimension.getFirst();
        List<String> standardCheckerKeys = pairForStandardDimension.getSecond();

        if (CollectionUtils.isNotEmpty(standardTools)) {
            log.info("start to get overview standard count for task: {}, tools: {}", taskId, standardTools);

            lintDefectV2Dao.findStatisticGroupByAuthor(taskId, standardTools, standardCheckerKeys).forEach(
                    agg -> {
                        String author = ToolParamUtils.trimUserName(agg.getAuthorName());
                        TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                        if (entity == null) {
                            entity = new TaskPersonalStatisticEntity();
                            entity.setTaskId(taskId);
                            entity.setUsername(author);
                            taskPersonalStatisticMap.put(author, entity);
                        }

                        entity.setStandardCount(entity.getStandardCount() + agg.getDefectCount());
                    }
            );
        }

        if (taskToolNameList.contains(ComConstants.Tool.CCN.name())) {
            log.info("start to get overview ccn risk count for task: {}", taskId);

            ccnDefectDao.findStatisticGroupByAuthor(
                    taskId,
                    ComConstants.DefectStatus.NEW.value()
            ).forEach(defectEntity -> {
                String author = ToolParamUtils.trimUserName(defectEntity.getAuthorName());
                TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                if (entity == null) {
                    entity = new TaskPersonalStatisticEntity();
                    entity.setTaskId(taskId);
                    entity.setUsername(author);
                    taskPersonalStatisticMap.put(author, entity);
                }

                entity.setRiskCount(entity.getRiskCount() + defectEntity.getDefectCount());
            });
        }

        if (taskToolNameList.contains(ComConstants.Tool.DUPC.name())) {
            log.info("start to get overview dup file count for task: {}", taskId);

            for (int curPage = 1; curPage <= DEFECT_MAX_PAGE; curPage++) {
                Pageable pageable = PageableUtils.getPageable(curPage, DEFECT_MAX_PAGE);
                List<DUPCDefectEntity> dupcDefectList = dupcDefectRepository.findAuthorListByTaskIdAndAuthor(
                        taskId,
                        ComConstants.DefectStatus.NEW.value(),
                        pageable
                );

                if (CollectionUtils.isEmpty(dupcDefectList)) {
                    break;
                }

                dupcDefectList.forEach(defectEntity -> {
                    String authorListString = defectEntity.getAuthorList();
                    if (StringUtils.isNotBlank(authorListString)) {
                        String[] authorList = authorListString.split(";");
                        for (String rawAuthor : authorList) {
                            String author = ToolParamUtils.trimUserName(rawAuthor);
                            TaskPersonalStatisticEntity entity = taskPersonalStatisticMap.get(author);
                            if (entity == null) {
                                entity = new TaskPersonalStatisticEntity();
                                entity.setTaskId(taskId);
                                entity.setUsername(author);
                                taskPersonalStatisticMap.put(author, entity);
                            }
                            entity.setDupFileCount(entity.getDupFileCount() + 1);
                        }
                    }
                });
            }
        }

        log.info("start to delete old overview data and save new for task: {}", taskId);
        taskPersonalStatisticRepository.deleteByTaskId(taskId);
        taskPersonalStatisticDao.batchSave(taskPersonalStatisticMap.values());
    }

    /**
     * 检查工具是否已经下架
     *
     * @param toolName
     * @param taskDetailVO
     * @return true:已下架，false:未下架
     */
    private boolean checkToolRemoved(String toolName, TaskDetailVO taskDetailVO) {
        // 指定项目不限制，可以执行coverity
        try {
            Set<String> projectIdSet = toolLicenseWhiteListCache.get(toolName);
            if (CollectionUtils.isNotEmpty(projectIdSet) && projectIdSet.contains(taskDetailVO.getProjectId())) {
                return false;
            }
        } catch (ExecutionException e) {
            log.warn("get tool license white list exception: {}, {}, {}",
                    toolName, taskDetailVO.getProjectId(), taskDetailVO.getTaskId());
        }

        ToolMetaBaseVO toolMetaBase = toolMetaCacheService.getToolBaseMetaCache(toolName);
        if (ComConstants.ToolIntegratedStatus.D.name().equals(toolMetaBase.getStatus())) {
            log.info("tool was removed: {}, {}", toolName, taskDetailVO.getTaskId());
            return true;
        }

        return false;
    }

    /**
     * 获取维度对应的工具列表以及规则列表
     *
     * @param checkerCategory
     * @param taskToolNameList
     * @return 1-> 工具列表, 2-> 规则列表
     */
    private Pair<List<String>, List<String>> getToolNamesAndCheckerKeysPairByDimension(
            String checkerCategory,
            List<String> taskToolNameList
    ) {
        List<CheckerDetailEntity> checkerDetailList = checkerDetailRepository.findByCheckerCategoryAndToolNameIn(
                checkerCategory,
                taskToolNameList
        );

        if (CollectionUtils.isEmpty(checkerDetailList)) {
            return Pair.of(Lists.newArrayList(), Lists.newArrayList());
        }

        Set<String> toolNameSet = Sets.newHashSet();
        Set<String> checkerKeySet = Sets.newHashSetWithExpectedSize(checkerDetailList.size());

        for (CheckerDetailEntity checkerDetailEntity : checkerDetailList) {
            String toolName = checkerDetailEntity.getToolName();
            String checkerKey = checkerDetailEntity.getCheckerKey();

            if (StringUtils.isNotEmpty(toolName) && !toolNameSet.contains(toolName)) {
                toolNameSet.add(toolName);
            }

            if (StringUtils.isNotEmpty(checkerKey) && !checkerKeySet.contains(checkerKey)) {
                checkerKeySet.add(checkerKey);
            }
        }

        return Pair.of(Lists.newArrayList(toolNameSet), Lists.newArrayList(checkerKeySet));
    }
}
