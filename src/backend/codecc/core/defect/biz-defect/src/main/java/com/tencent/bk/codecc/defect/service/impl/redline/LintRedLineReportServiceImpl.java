package com.tencent.bk.codecc.defect.service.impl.redline;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.service.AbstractRedLineReportService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLDimensionVO;
import com.tencent.bk.codecc.defect.vo.redline.RLLintDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.constant.ComConstants.FileType;
import com.tencent.devops.common.util.DateTimeUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class LintRedLineReportServiceImpl extends AbstractRedLineReportService<LintDefectV2Entity> {

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    /**
     * 开源规范检查工具
     */
    private static final Set<String> TOSA_STANDARD_TOOLS = Sets.newHashSet(
            ComConstants.Tool.CPPLINT.name(), ComConstants.Tool.ESLINT.name(),
            ComConstants.Tool.STYLECOP.name(), ComConstants.Tool.CHECKSTYLE.name(), ComConstants.Tool.PYLINT.name(),
            ComConstants.Tool.GOML.name(), ComConstants.Tool.DETEKT.name(), ComConstants.Tool.OCCHECK.name());

    /**
     * 查询质量红线指标数据
     *
     * @param taskDetailVO
     * @param effectiveTools
     * @param metadataCallback
     * @param toolConfig
     * @param metadataModel
     * @param toolInfo
     * @return
     */
    @Override
    public void getAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo, ToolConfigInfoVO toolConfig,
                                  Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
                                  List<String> effectiveTools, String buildId, List<LintDefectV2Entity> newDefectList,
                                  RedLineExtraParams<LintDefectV2Entity> extraParams) {
        log.info("start to get lint analysis result for task: {} {} {}",
                taskDetailVO.getTaskId(), toolInfo.getName(), buildId);
        long taskId = taskDetailVO.getTaskId();
        String toolName = toolInfo.getName();

        boolean isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);
        RLDimensionVO rlDimensionVO = new RLDimensionVO();
        if (isMigrationSuccessful) {
            initDimension(toolName, rlDimensionVO);
        }

        RLLintDefectVO lintRLModel = new RLLintDefectVO()
                .newCheckerCounts()
                .newCheckerPkgCounts()
                .historyCheckerCounts()
                .historyCheckerPkgCounts();

        // 查询所有规则详情
        Map<String, CheckerDetailVO> allCheckerMap = checkerService.queryAllChecker(toolName);
        // 初始化规则告警数和规则包告警数为0
        List<CheckerDetailEntity> checkerDetailList = checkerRepository.findByToolName(toolName);
        initRLLintChecker(toolName, lintRLModel, PASS_COUNT);
        initRLLintPkg(checkerDetailList, lintRLModel, PASS_COUNT);
        // 项目配置的编程语言是否与该工具支持的语言相符
        boolean toolMatchProjLang = (toolInfo.getLang() & taskDetailVO.getCodeLang()) != 0;

        // 工具与语言符合的，才需要计算告警数量，语言不符合就通过
        if (!toolMatchProjLang) {
            log.info("lint red line for task is not match lang: {} {} {}",
                    taskDetailVO.getTaskId(), toolInfo.getName(), buildId);
            return;
        }

        // 语言符合时，如果未接入工具，数值全部为-1
        if (!effectiveTools.contains(toolName)) {
            lintRLModel.setHistoryNormal(FORBIDDEN_COUNT);
            lintRLModel.setHistoryPrompt(FORBIDDEN_COUNT);
            lintRLModel.setHistorySerious(FORBIDDEN_COUNT);
            lintRLModel.setNewNormal(FORBIDDEN_COUNT);
            lintRLModel.setNewPrompt(FORBIDDEN_COUNT);
            lintRLModel.setNewSerious(FORBIDDEN_COUNT);
            initRLLintPkg(checkerDetailList, lintRLModel, FORBIDDEN_COUNT);
            initRLLintChecker(toolName, lintRLModel, FORBIDDEN_COUNT);
            log.info("lint red line for task is not match tool: {} {} {}",
                    taskDetailVO.getTaskId(), toolInfo.getName(), buildId);
            return;
        }

        boolean isTosaPkgOpened = false;
        Set<String> tosaCheckers = Sets.newHashSet();
        // 查询规则包和规则打开情况
        List<CheckerPkgRspVO> checkerPkgList = configCheckerPkgBizService.getConfigCheckerPkg(
                taskId, toolName, toolInfo.getLang(), toolConfig
        ).getCheckerPackages();

        Map<String, CheckerPkgRspVO> pkgMap = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(checkerPkgList)) {
            log.info("get lint analysis result for checkerPkgList: {} {} {} {}",
                    taskDetailVO.getTaskId(), toolInfo.getName(), buildId, checkerPkgList.size());
            pkgMap = checkerPkgList.stream()
                    .collect(Collectors.toMap(CheckerPkgRspVO::getPkgId, Function.identity(), (k,v) -> v));
        }

        // 初始化规则包告警数
        if (MapUtils.isNotEmpty(pkgMap)) {
            log.info("get lint analysis result for pkgMap: {} {} {} {}",
                    taskDetailVO.getTaskId(), toolInfo.getName(), buildId, pkgMap.size());

            // 初始化规则包告警数量，设置规则未全部打开的规则包告警数量为-1，全打开的设置为0
            for (Map.Entry<String, CheckerPkgRspVO> checkerPkgEntry : pkgMap.entrySet()) {
                if (isAllCheckerOpened(checkerPkgEntry.getValue())) {
                    lintRLModel.getNewCheckerPkgCounts().put(checkerPkgEntry.getKey(), 0L);
                    lintRLModel.getHistoryCheckerPkgCounts().put(checkerPkgEntry.getKey(), 0L);

                    // 开源规范包的规则是否全部开启
                    if (ComConstants.CheckerPkgKind.TOSA.value().equals(checkerPkgEntry.getKey())) {
                        isTosaPkgOpened = true;
                    }
                } else {
                    lintRLModel.getNewCheckerPkgCounts().put(checkerPkgEntry.getKey(), FORBIDDEN_COUNT);
                    lintRLModel.getHistoryCheckerPkgCounts().put(checkerPkgEntry.getKey(), FORBIDDEN_COUNT);
                }
            }

            // 获取开源规范包的规则
            List<CheckerDetailVO> tosaCheckerModels;
            CheckerPkgRspVO checkerPkgRspVO = pkgMap.get(ComConstants.CheckerPkgKind.TOSA.value());
            if (checkerPkgRspVO != null
                    && CollectionUtils.isNotEmpty(tosaCheckerModels = checkerPkgRspVO.getCheckerList())) {
                tosaCheckerModels.stream()
                        .map(CheckerDetailVO::getCheckerKey)
                        .forEach(checkerKey -> {
                            if (checkerKey.endsWith("-tosa")) {
                                tosaCheckers.add(checkerKey.substring(0, checkerKey.indexOf("-tosa")));
                            } else {
                                tosaCheckers.add(checkerKey);
                            }
                        });
            }
        }

        List<LintDefectV2Entity> defectV2EntityList = newDefectList;

        log.info("get lint analysis result for defectV2EntityList: {} {} {} {}",
                taskDetailVO.getTaskId(), toolInfo.getName(), buildId, defectV2EntityList.size());

        // 查询接入前和接入后告警详情
        for (LintDefectV2Entity defect : defectV2EntityList) {
            // 按新老告警判定时间获取新老告警列表
            long defectLastUpdateTime = DateTimeUtils.getThirteenTimestamp(defect.getLineUpdateTime());
            // 统计告警数量
            updateLintSeverityCount(defect.getSeverity(), ComConstants.FileType.NEW, lintRLModel);

            // 统计各规则包告警数，工具与项目语言不符合的不做统计
            updateLintCheckerPkgCount(lintRLModel.getNewCheckerPkgCounts(), defect.getChecker(),
                    allCheckerMap, tosaCheckers, isTosaPkgOpened);

            // 统计接入后各规则告警数量
            updateLintCheckerCount(lintRLModel.getNewCheckerCounts(), defect.getChecker(), toolName);

            // 若数据迁移成功，则按规则标签统计维度信息
            if (isMigrationSuccessful && !StringUtils.isEmpty(defect.getChecker())) {
                CheckerDetailVO checkerDetailVO = allCheckerMap.get(defect.getChecker());
                String checkerCategory = checkerDetailVO == null ? "" : checkerDetailVO.getCheckerCategory();
                calcDimensionByCheckerTag(
                        defect.getSeverity(),
                        FileType.NEW,
                        checkerCategory,
                        rlDimensionVO,
                        defect.getEntityId()
                );
            }
        }

        // 更新存量数据
        // 获取存量问题的忽略标识位
        Integer historyIgnoreType = baseDataCacheService.getHistoryIgnoreType();
        if (historyIgnoreType != null && extraParams != null
                && CollectionUtils.isNotEmpty(extraParams.getIgnoreDefectList())) {
            // 统计告警数量
            List<LintDefectV2Entity> historyDefects = extraParams.getIgnoreDefectList()
                    .stream().filter(defect -> Objects.equals(defect.getIgnoreReasonType(), historyIgnoreType))
                    .collect(Collectors.toList());
            log.info("get lint history result for defectV2EntityList: {} {} {} {}",
                    taskDetailVO.getTaskId(), toolInfo.getName(), buildId,
                    CollectionUtils.isEmpty(historyDefects) ? 0 : historyDefects.size());
            if (CollectionUtils.isNotEmpty(historyDefects)) {
                for (LintDefectV2Entity defect : historyDefects) {
                    // 统计存量各 严重级别告警数
                    updateLintSeverityCount(defect.getSeverity(), ComConstants.FileType.HISTORY, lintRLModel);

                    // 统计各规则包告警数，工具与项目语言不符合的不做统计
                    updateLintCheckerPkgCount(lintRLModel.getHistoryCheckerPkgCounts(), defect.getChecker(),
                            allCheckerMap, tosaCheckers, isTosaPkgOpened);

                    // 统计存量各规则告警数量
                    updateLintCheckerCount(
                            lintRLModel.getHistoryCheckerCounts(), defect.getChecker(), toolName);

                    // 若数据迁移成功，则按规则标签统计维度信息
                    if (isMigrationSuccessful && !StringUtils.isEmpty(defect.getChecker())) {
                        CheckerDetailVO checkerDetailVO = allCheckerMap.get(defect.getChecker());
                        String checkerCategory = checkerDetailVO == null ? "" : checkerDetailVO.getCheckerCategory();
                        calcDimensionByCheckerTag(
                                defect.getSeverity(),
                                FileType.HISTORY,
                                checkerCategory,
                                rlDimensionVO,
                                defect.getEntityId()
                        );
                    }
                }
            }
        }

        // 更新元数据
        // 更新待修复数据
        updateValue(toolName + "_NEW_SERIOUS", String.valueOf(lintRLModel.getNewSerious()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_NORMAL", String.valueOf(lintRLModel.getNewNormal()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_NEW_PROMPT", String.valueOf(lintRLModel.getNewPrompt()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_SERIOUS", String.valueOf(lintRLModel.getHistorySerious()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_NORMAL", String.valueOf(lintRLModel.getHistoryNormal()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_HISTORY_PROMPT", String.valueOf(lintRLModel.getHistoryPrompt()),
                toolName, metadataModel, metadataCallback);

        // 规范类工具都需要上报开源规则包告警数
        if (TOSA_STANDARD_TOOLS.contains(toolName)) {
            updateValue(toolName + "_NEW_TOSA",
                    String.valueOf(lintRLModel.getNewCheckerPkgCounts().get(ComConstants.CheckerPkgKind.TOSA.value())),
                    toolName, metadataModel, metadataCallback);
        }

        // 更新规则告警数
        for (Map.Entry<String, Long> entry : lintRLModel.getNewCheckerCounts().entrySet()) {
            String checkerNameUpper = allCheckerMap.get(entry.getKey()).getCheckerName().toUpperCase();
            String metadataKey = toolName + ComConstants.KEY_UNDERLINE + checkerNameUpper + "_NEW";
            updateValue(metadataKey, String.valueOf(entry.getValue()), toolName, metadataModel,
                    metadataCallback);
        }

        for (Map.Entry<String, Long> entry : lintRLModel.getHistoryCheckerCounts().entrySet()) {
            String checkerName = allCheckerMap.get(entry.getKey()).getCheckerName();
            String metadataKey = toolName + ComConstants.KEY_UNDERLINE + checkerName.toUpperCase() + "_HISTORY";
            updateValue(metadataKey, String.valueOf(entry.getValue()), toolName, metadataModel,
                    metadataCallback);
        }

        if (isMigrationSuccessful) {
            setDimensionVO(metadataCallback, rlDimensionVO);
        }

        log.info("finish to get lint analysis result for task: {} {} {}", taskDetailVO.getTaskId(), toolName, buildId);
    }




    /**
     * 保存各严重级别告警数
     *
     * @param severity
     * @param fileType
     * @param defectCountModel
     */
    private void updateLintSeverityCount(
            int severity, ComConstants.FileType fileType, RLLintDefectVO defectCountModel) {
        if (ComConstants.SERIOUS == severity) {
            if (ComConstants.FileType.HISTORY.equals(fileType)) {
                defectCountModel.setHistorySerious(defectCountModel.getHistorySerious() + 1L);
            } else {
                defectCountModel.setNewSerious(defectCountModel.getNewSerious() + 1L);
            }
        } else if (ComConstants.NORMAL == severity) {
            if (ComConstants.FileType.HISTORY.equals(fileType)) {
                defectCountModel.setHistoryNormal(defectCountModel.getHistoryNormal() + 1L);
            } else {
                defectCountModel.setNewNormal(defectCountModel.getNewNormal() + 1L);
            }
        } else if (ComConstants.PROMPT_IN_DB == severity || ComConstants.PROMPT == severity) {
            if (ComConstants.FileType.HISTORY.equals(fileType)) {
                defectCountModel.setHistoryPrompt(defectCountModel.getHistoryPrompt() + 1L);
            } else {
                defectCountModel.setNewPrompt(defectCountModel.getNewPrompt() + 1L);
            }
        }
    }

    /**
     * 初始化规则告警数
     *
     * @param toolName
     * @param defectCountModel
     * @param initValue
     */
    private void initRLLintChecker(String toolName, RLLintDefectVO defectCountModel, long initValue) {
        if (!RED_LINE_CHECKERS.containsKey(toolName)) {
            return;
        }

        for (String checker : RED_LINE_CHECKERS.get(toolName)) {
            defectCountModel.getNewCheckerCounts().put(checker, initValue);
            defectCountModel.getHistoryCheckerCounts().put(checker, initValue);
        }
    }

    /**
     * 初始化LINT类工具数据
     *
     * @param checkerDetailList
     * @param lintRLModel
     * @param initValue
     */
    private void initRLLintPkg(List<CheckerDetailEntity> checkerDetailList,
                               RLLintDefectVO lintRLModel,
                               long initValue) {
        // 根据工具获取规则包
        if (CollectionUtils.isEmpty(checkerDetailList)) {
            return;
        }

        checkerDetailList.stream()
                .map(CheckerDetailEntity::getPkgKind)
                .distinct().forEach(pkgId -> {
            lintRLModel.getHistoryCheckerPkgCounts().put(pkgId, initValue);
            lintRLModel.getNewCheckerPkgCounts().put(pkgId, initValue);
        });
    }

    /**
     * 更新规则包告警数
     *
     * @param checkerPkgCounts
     * @param checker
     * @param allCheckerMap
     */
    private void updateLintCheckerPkgCount(Map<String, Long> checkerPkgCounts, String checker,
                                           Map<String, CheckerDetailVO> allCheckerMap,
                                           Set<String> tosaCheckers, boolean isTosaPkgOpened) {
        // 如果打开了开源规范包，则同名规则以开源规范包为准
        CheckerDetailVO checkerDetail = allCheckerMap.get(checker);
        if (checkerDetail == null) {
            return;
        }

        if (tosaCheckers.contains(checkerDetail.getCheckerKey()) && isTosaPkgOpened) {
            checkerDetail.setPkgKind(ComConstants.CheckerPkgKind.TOSA.value());
        }

        // 更新各规则包告警数
        Long currentPkgCountObj = checkerPkgCounts.get(checkerDetail.getPkgKind());
        if (currentPkgCountObj != null && currentPkgCountObj != FORBIDDEN_COUNT) {
            long currentCount = currentPkgCountObj;
            checkerPkgCounts.put(checkerDetail.getPkgKind(), currentCount + 1);
        }
    }

    /**
     * 判断规则包里的规则是否全部打开
     *
     * @param checkerPkg
     * @return
     */
    private boolean isAllCheckerOpened(CheckerPkgRspVO checkerPkg) {
        if (!checkerPkg.getPkgStatus()) {
            return false;
        }

        if (CollectionUtils.isEmpty(checkerPkg.getCheckerList())) {
            return false;
        }

        return checkerPkg.getCheckerList()
                .stream()
                .allMatch(CheckerDetailVO::getCheckerStatus);
    }

    /**
     * 累计各规则告警数
     *
     * @param checkerCounts
     * @param checker
     * @param toolName
     */
    private void updateLintCheckerCount(Map<String, Long> checkerCounts, String checker, String toolName) {
        if (RED_LINE_CHECKERS.containsKey(toolName) && RED_LINE_CHECKERS.get(toolName).contains(checker)) {
            checkerCounts.put(checker, checkerCounts.get(checker) + 1L);
        }
    }
}
