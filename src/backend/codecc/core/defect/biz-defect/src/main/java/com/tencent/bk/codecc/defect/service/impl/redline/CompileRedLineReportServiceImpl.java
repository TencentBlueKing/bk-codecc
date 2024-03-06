package com.tencent.bk.codecc.defect.service.impl.redline;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerPackageRepository;
import com.tencent.bk.codecc.defect.model.CheckerPackageEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.service.AbstractRedLineReportService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLCompileDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RLDimensionVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.FileType;
import com.tencent.devops.common.constant.RedLineConstants;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CompileRedLineReportServiceImpl extends AbstractRedLineReportService<CommonDefectEntity> {
    @Autowired
    private CheckerPackageRepository checkerPackageRepository;
    @Autowired
    private CheckerService checkerService;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    /**
     * 查询Coverity分析结果
     *
     * @param taskInfo
     * @param toolMeta
     * @param toolConfig
     * @param metadataModel
     * @param metadataCallback
     * @param effectiveTools
     */
    @Override
    public void getAnalysisResult(TaskDetailVO taskInfo, ToolMetaBaseVO toolMeta, ToolConfigInfoVO toolConfig,
                                  Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
                                  List<String> effectiveTools, String buildId, List<CommonDefectEntity> newDefectList,
                                  RedLineExtraParams<CommonDefectEntity> extraParams) {

        log.info("start to get compile analysis result for task: {} {} {}",
                taskInfo.getTaskId(), toolMeta.getName(), buildId);

        if (toolMeta.getName().equals(ComConstants.Tool.COVERITY.name())) {
            getCovAnalysisResult(taskInfo, toolMeta, toolConfig, metadataModel, metadataCallback, effectiveTools,
                    buildId, newDefectList);
        } else if (toolMeta.getName().equals(ComConstants.Tool.KLOCWORK.name())) {
            getKlocAnalysisResult(taskInfo, toolMeta, toolConfig, metadataModel, metadataCallback, effectiveTools,
                    buildId, newDefectList);
        }
    }

    private void getCovAnalysisResult(TaskDetailVO taskInfo, ToolMetaBaseVO toolMeta, ToolConfigInfoVO toolConfig,
            Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
            List<String> effectiveTools, String buildId,
            List<CommonDefectEntity> newDefectList) {
        long taskId = taskInfo.getTaskId();
        String toolName = ComConstants.Tool.COVERITY.name();
        RLCompileDefectVO redLineVO = new RLCompileDefectVO();
        redLineVO.setCheckerPkgCounts(Maps.newHashMap());


        // 查询规则包
        List<CheckerPackageEntity> allCheckerPkgList = checkerPackageRepository.findByToolName(toolName);
        long taskCodeLang = taskInfo.getCodeLang();
        // 项目配置的编程语言是否与该工具支持的语言相符
        boolean toolMatchProjLang = (toolMeta.getLang() & taskCodeLang) != 0;

        boolean isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);
        RLDimensionVO rlDimensionVO = new RLDimensionVO();
        if (isMigrationSuccessful) {
            initDimension(toolName, rlDimensionVO);
        }

        // 工具与语言不符合的，数值全部为0，不做拦截
        if (!toolMatchProjLang) {
            log.info("cov red line for task is not match lang: {} {} {}",
                    taskInfo.getTaskId(), toolMeta.getName(), buildId);
            if (CollectionUtils.isNotEmpty(allCheckerPkgList)) {
                for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList) {
                    redLineVO.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), PASS_COUNT);
                }
            }
        } else {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName)) {
                log.info("cov red line for task is not match tool: {} {} {}",
                        taskInfo.getTaskId(), toolMeta.getName(), buildId);
                redLineVO.setRemainPrompt(FORBIDDEN_COUNT);
                redLineVO.setRemainNormal(FORBIDDEN_COUNT);
                redLineVO.setRemainSerious(FORBIDDEN_COUNT);
                if (CollectionUtils.isNotEmpty(allCheckerPkgList)) {
                    for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList) {
                        redLineVO.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), FORBIDDEN_COUNT);
                    }
                }
            } else {
                // 先初始化全部规则包
                if (CollectionUtils.isNotEmpty(allCheckerPkgList)) {
                    for (CheckerPackageEntity checkerPkgRsp : allCheckerPkgList) {
                        redLineVO.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), 0L);
                    }
                }

                // 查询任务规则包配置情况
                List<CheckerPkgRspVO> checkerPkgList = configCheckerPkgBizService.getConfigCheckerPkg(
                        taskId, toolName, taskCodeLang, toolConfig
                ).getCheckerPackages();

                // 未打开的规则包设置为forbidden
                if (CollectionUtils.isNotEmpty(checkerPkgList)) {
                    for (CheckerPkgRspVO checkerPkgRsp : checkerPkgList) {
                        if (!checkerPkgRsp.getPkgStatus()) {
                            redLineVO.getCheckerPkgCounts().put(checkerPkgRsp.getPkgId(), FORBIDDEN_COUNT);
                        }
                    }
                }

                // 查询规则详情
                Map<String, CheckerDetailVO> allCheckers = checkerPkgList.stream().map(CheckerPkgRspVO::getCheckerList)
                        .filter(CollectionUtils::isNotEmpty).flatMap(Collection::parallelStream)
                        .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));

                // 查询遗留告警列表
                List<CommonDefectEntity> defectDetailList = newDefectList;

                if (CollectionUtils.isNotEmpty(defectDetailList)) {

                    for (CommonDefectEntity covDefect : defectDetailList) {
                        // 统计各级别告警数
                        updateCompileSeverityCount(covDefect, redLineVO);

                        // 统计各规则包告警数
                        CheckerDetailVO checkerPOModel = allCheckers.get(covDefect.getChecker());
                        if (checkerPOModel != null) {
                            String kind = checkerPOModel.getPkgKind();
                            long currentCount = redLineVO.getCheckerPkgCounts().get(kind);
                            redLineVO.getCheckerPkgCounts().put(kind, currentCount + 1);
                        }

                        // 若数据迁移成功，则按规则标签统计维度信息
                        if (isMigrationSuccessful) {
                            calcDimensionForCompileTool(covDefect, allCheckers, rlDimensionVO);
                        }
                    }
                }
            }
        }

        // 更新元数据
        updateCompileMetadata(toolName, redLineVO, metadataModel, metadataCallback);
        updateValue(toolName + "_SECURITY",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.SECURITY.value())),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_MEMORY",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.MEMORY.value())),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_PERFORMANCE",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.PERFORMANCE.value())),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_COMPILE",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.COMPILE.value())),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_SYS_API",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.SYS_API.value())),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_EXPRESSION",
                String.valueOf(redLineVO.getCheckerPkgCounts().get(ComConstants.CheckerPkgKind.EXPRESSION.value())),
                toolName, metadataModel, metadataCallback);

        if (isMigrationSuccessful) {
            setDimensionVO(metadataCallback, rlDimensionVO);
        }

        log.info("finish to get cov analysis result for task: {} {} {}", taskInfo.getTaskId(), toolName, buildId);
    }

    private void getKlocAnalysisResult(TaskDetailVO taskInfo, ToolMetaBaseVO toolMeta, ToolConfigInfoVO toolConfig,
            Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
            List<String> effectiveTools, String buildId, List<CommonDefectEntity> newDefectList) {

        String toolName = ComConstants.Tool.KLOCWORK.name();
        RLCompileDefectVO redLineVO = new RLCompileDefectVO();
        boolean toolMatchProjLang = (toolMeta.getLang() & taskInfo.getCodeLang()) != 0;
        Map<String, CheckerDetailVO> allCheckerMap = checkerService.queryAllChecker(toolName);

        boolean isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskInfo.getTaskId());
        RLDimensionVO rlDimensionVO = new RLDimensionVO();
        if (isMigrationSuccessful) {
            initDimension(toolName, rlDimensionVO);
        }

        if (toolMatchProjLang) {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName)) {
                log.info("kloc red line for task is not match lang: {} {} {}",
                        taskInfo.getTaskId(), toolMeta.getName(), buildId);
                redLineVO.setRemainPrompt(FORBIDDEN_COUNT);
                redLineVO.setRemainNormal(FORBIDDEN_COUNT);
                redLineVO.setRemainSerious(FORBIDDEN_COUNT);
            } else {
                List<CommonDefectEntity> defectList = newDefectList;

                if (CollectionUtils.isNotEmpty(defectList)) {

                    for (CommonDefectEntity defect : defectList) {
                        // 统计各级别告警数
                        updateCompileSeverityCount(defect, redLineVO);

                        // 若数据迁移成功，则按规则标签统计维度信息
                        if (isMigrationSuccessful) {
                            calcDimensionForCompileTool(defect, allCheckerMap, rlDimensionVO);
                        }
                    }
                }
            }
        }

        // 更新元数据
        updateCompileMetadata(toolName, redLineVO, metadataModel, metadataCallback);

        if (isMigrationSuccessful) {
            setDimensionVO(metadataCallback, rlDimensionVO);
        }

        log.info("finish to get kloc analysis result for task: {} {} {}", taskInfo.getTaskId(), toolName, buildId);
    }

    private void calcDimensionForCompileTool(
            CommonDefectEntity defect,
            Map<String, CheckerDetailVO> allCheckerMap,
            RLDimensionVO rlDimensionVO
    ) {
        if (StringUtils.isEmpty(defect.getChecker())) {
            return;
        }

        CheckerDetailVO checkerDetailVO = allCheckerMap.get(defect.getChecker());
        // 规则的维度标签
        String checkerCategory = checkerDetailVO == null ? "" : checkerDetailVO.getCheckerCategory();
        calcDimensionByCheckerTag(
                defect.getSeverity(),
                FileType.NEW,
                checkerCategory,
                rlDimensionVO,
                defect.getEntityId()
        );
    }

    /**
     * 保存各严重级别告警数
     * @param commonDefectEntity
     * @param defect
     */
    protected void updateCompileSeverityCount(
            CommonDefectEntity commonDefectEntity,
            RLCompileDefectVO defect
    ) {
        int severity = commonDefectEntity.getSeverity();
        if ((ComConstants.SERIOUS & severity) != 0) {
            defect.setRemainSerious(defect.getRemainSerious() + 1);
            defect.setNewSerious(defect.getNewSerious() + 1);
        } else if ((ComConstants.NORMAL & severity) != 0) {
            defect.setRemainNormal(defect.getRemainNormal() + 1);
            defect.setNewNormal(defect.getNewNormal() + 1);
        } else if ((ComConstants.PROMPT & severity) != 0) {
            defect.setRemainPrompt(defect.getRemainPrompt() + 1);
            defect.setNewPrompt(defect.getNewPrompt() + 1);
        }
    }

    /**
     * 更新Compile类工具元数据
     *
     * @param toolName
     * @param defect
     * @param metadataModel
     * @param metadataCallback
     */
    protected void updateCompileMetadata(String toolName,
                                         RLCompileDefectVO defect,
                                         Map<String, RedLineVO> metadataModel,
                                         PipelineRedLineCallbackVO metadataCallback) {
        updateValue(toolName + "_SERIOUS", String.valueOf(defect.getRemainSerious()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_NORMAL", String.valueOf(defect.getRemainNormal()),
                toolName, metadataModel, metadataCallback);
        updateValue(toolName + "_PROMPT", String.valueOf(defect.getRemainPrompt()),
                toolName, metadataModel, metadataCallback);

        updateValue(toolName + RedLineConstants.NEW_SERIOUS_SUFFIX,
                String.valueOf(defect.getNewSerious()), toolName, metadataModel, metadataCallback);
        updateValue(toolName + RedLineConstants.NEW_NORMAL_SUFFIX,
                String.valueOf(defect.getNewNormal()), toolName, metadataModel, metadataCallback);
        updateValue(toolName + RedLineConstants.NEW_PROMPT_SUFFIX,
                String.valueOf(defect.getNewPrompt()), toolName, metadataModel, metadataCallback);

        updateValue(toolName + RedLineConstants.HISTORY_SERIOUS_SUFFIX,
                String.valueOf(defect.getHistorySerious()), toolName, metadataModel, metadataCallback);
        updateValue(toolName + RedLineConstants.HISTORY_NORMAL_SUFFIX,
                String.valueOf(defect.getHistoryNormal()), toolName, metadataModel, metadataCallback);
        updateValue(toolName + RedLineConstants.HISTORY_PROMPT_SUFFIX,
                String.valueOf(defect.getHistoryPrompt()), toolName, metadataModel, metadataCallback);
    }
}
