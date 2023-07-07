package com.tencent.bk.codecc.defect.service;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_CODECC_SCRIPT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_PAAS_CODECC_SCRIPT;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.RedLineMetaRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.RedLineRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.RedLineMetaEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLDimensionVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.FileType;
import com.tencent.devops.common.constant.RedLineConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.BeanUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author warmli
 */
@Slf4j
@Service
public abstract class AbstractRedLineReportService<T extends DefectEntity> implements RedLineReportService<T> {

    @Autowired
    private RedLineRepository redLineRepository;

    @Autowired
    private FileDefectGatherService fileDefectGatherService;

    @Autowired
    protected ToolMetaCacheService toolMetaCacheService;

    @Autowired
    protected IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Autowired
    private CheckerRepository checkerRepository;

    private static RedLineMetaRepository redLineMetaRepository;

    @Autowired
    public void setRedLineMetaRepository(RedLineMetaRepository redLineMetaRepository) {
        AbstractRedLineReportService.redLineMetaRepository = redLineMetaRepository;
    }

    /**
     * 质量红线当前支持单独上报数量的规则
     */
    protected static final Map<String, List<String>> RED_LINE_CHECKERS = ImmutableMap.of(
            "OCCHECK", Lists.newArrayList("MaxLinesPerFunction"),
            "CHECKSTYLE", Lists.newArrayList("MethodLength"),
            "GOML", Lists.newArrayList("golint/fnsize"));

    protected static final Map<String, String> COMMON_META_SUFFIX_CN_NAME_MAP =
            new ImmutableMap.Builder<String, String>()
                    .put(RedLineConstants.HISTORY_SERIOUS_SUFFIX, "接入前严重告警数")
                    .put(RedLineConstants.HISTORY_NORMAL_SUFFIX, "接入前一般告警数")
                    .put(RedLineConstants.HISTORY_PROMPT_SUFFIX, "接入前提示告警数")
                    .put(RedLineConstants.NEW_SERIOUS_SUFFIX, "接入后严重告警数")
                    .put(RedLineConstants.NEW_NORMAL_SUFFIX, "接入后一般告警数")
                    .put(RedLineConstants.NEW_PROMPT_SUFFIX, "接入后示告警数")
                    .put(RedLineConstants.NEW_TOSA, "腾讯开源代码规范告警数")
                    .build();

    /**
     * 红线基础数据cache
     */
    protected static final LoadingCache<String, Map<String, RedLineVO>> RED_LINE_META_CACHE = CacheBuilder.newBuilder()
            .refreshAfterWrite(2, TimeUnit.HOURS)
            .build(new CacheLoader<String, Map<String, RedLineVO>>() {
                @Override
                public Map<String, RedLineVO> load(@NotNull String toolName) {
                    List<RedLineMetaEntity> metaData = redLineMetaRepository.findByDetail(toolName);
                    if (CollectionUtils.isEmpty(metaData)) {
                        log.warn("fail to get red line meta: {}", toolName);
                        return new HashMap<>();
                    }

                    return metaData.stream()
                            .map(meta -> {
                                RedLineVO redLineVO = new RedLineVO();
                                BeanUtils.copyProperties(meta, redLineVO);
                                return redLineVO;
                            }).collect(Collectors.toMap(RedLineVO::getEnName, Function.identity(), (k, v) -> v));
                }
            });

    protected static final Map<String, Consumer<RLDimensionVO>> DIMENSION_CALCULATOR_MAP =
            new HashMap<String, Consumer<RLDimensionVO>>() {{
                // 代码缺陷
                put(CheckerCategory.CODE_DEFECT.name() + FileType.NEW.name() + ComConstants.PROMPT,
                        x -> x.setDefectNewPrompt(defaultValIfNull(x.getDefectNewPrompt()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.NEW.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setDefectNewPrompt(defaultValIfNull(x.getDefectNewPrompt()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.NEW.name() + ComConstants.NORMAL,
                        x -> x.setDefectNewNormal(defaultValIfNull(x.getDefectNewNormal()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.NEW.name() + ComConstants.SERIOUS,
                        x -> x.setDefectNewSerious(defaultValIfNull(x.getDefectNewSerious()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.HISTORY.name() + ComConstants.PROMPT,
                        x -> x.setDefectHistoryPrompt(defaultValIfNull(x.getDefectHistoryPrompt()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.HISTORY.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setDefectHistoryPrompt(defaultValIfNull(x.getDefectHistoryPrompt()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.HISTORY.name() + ComConstants.NORMAL,
                        x -> x.setDefectHistoryNormal(defaultValIfNull(x.getDefectHistoryNormal()) + 1));
                put(CheckerCategory.CODE_DEFECT.name() + FileType.HISTORY.name() + ComConstants.SERIOUS,
                        x -> x.setDefectHistorySerious(defaultValIfNull(x.getDefectHistorySerious()) + 1));

                // 代码规范
                put(CheckerCategory.CODE_FORMAT.name() + FileType.NEW.name() + ComConstants.PROMPT,
                        x -> x.setStandardNewPrompt(defaultValIfNull(x.getStandardNewPrompt()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.NEW.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setStandardNewPrompt(defaultValIfNull(x.getStandardNewPrompt()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.NEW.name() + ComConstants.NORMAL,
                        x -> x.setStandardNewNormal(defaultValIfNull(x.getStandardNewNormal()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.NEW.name() + ComConstants.SERIOUS,
                        x -> x.setStandardNewSerious(defaultValIfNull(x.getStandardNewSerious()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.HISTORY.name() + ComConstants.PROMPT,
                        x -> x.setStandardHistoryPrompt(defaultValIfNull(x.getStandardHistoryPrompt()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.HISTORY.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setStandardHistoryPrompt(defaultValIfNull(x.getStandardHistoryPrompt()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.HISTORY.name() + ComConstants.NORMAL,
                        x -> x.setStandardHistoryNormal(defaultValIfNull(x.getStandardHistoryNormal()) + 1));
                put(CheckerCategory.CODE_FORMAT.name() + FileType.HISTORY.name() + ComConstants.SERIOUS,
                        x -> x.setStandardHistorySerious(defaultValIfNull(x.getStandardHistorySerious()) + 1));

                // 安全漏洞
                put(CheckerCategory.SECURITY_RISK.name() + FileType.NEW.name() + ComConstants.PROMPT,
                        x -> x.setSecurityNewPrompt(defaultValIfNull(x.getSecurityNewPrompt()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.NEW.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setSecurityNewPrompt(defaultValIfNull(x.getSecurityNewPrompt()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.NEW.name() + ComConstants.NORMAL,
                        x -> x.setSecurityNewNormal(defaultValIfNull(x.getSecurityNewNormal()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.NEW.name() + ComConstants.SERIOUS,
                        x -> x.setSecurityNewSerious(defaultValIfNull(x.getSecurityNewSerious()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.HISTORY.name() + ComConstants.PROMPT,
                        x -> x.setSecurityHistoryPrompt(defaultValIfNull(x.getSecurityHistoryPrompt()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.HISTORY.name() + ComConstants.PROMPT_IN_DB,
                        x -> x.setSecurityHistoryPrompt(defaultValIfNull(x.getSecurityHistoryPrompt()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.HISTORY.name() + ComConstants.NORMAL,
                        x -> x.setSecurityHistoryNormal(defaultValIfNull(x.getSecurityHistoryNormal()) + 1));
                put(CheckerCategory.SECURITY_RISK.name() + FileType.HISTORY.name() + ComConstants.SERIOUS,
                        x -> x.setSecurityHistorySerious(defaultValIfNull(x.getSecurityHistorySerious()) + 1));
            }};

    private static Integer defaultValIfNull(Integer integer) {
        return Optional.ofNullable(integer).orElse(0);
    }

    /**
     * 保存质量红线数据
     *
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     */
    @Override
    public void saveRedLineData(TaskDetailVO taskDetailVO, String toolName, String buildId, List<T> newDefectList) {
        saveRedLineData(taskDetailVO, toolName, buildId, newDefectList, null);
    }

    /**
     * 保存质量红线数据
     *
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     */
    @Override
    public void saveRedLineData(TaskDetailVO taskDetailVO, String toolName, String buildId, List<T> newDefectList,
                                RedLineExtraParams<T> extraParams) {
        if (!ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetailVO.getCreateFrom())
                || CollectionUtils.isEmpty(taskDetailVO.getToolConfigInfoList())) {
            log.info("task is not create from pipeline: {} {} {}", taskDetailVO.getTaskId(), buildId, toolName);
            return;
        }

        // 筛选出正在使用的工具，注意：当连续执行两次不同工具的构建时第一次构建可能受第二次影响
        Set<String> effectiveTools = taskDetailVO.getToolConfigInfoList()
                .stream()
                .filter(it -> it.getFollowStatus() != ComConstants.FOLLOW_STATUS.WITHDRAW.value())
                .map(ToolConfigInfoVO::getToolName)
                .collect(Collectors.toSet());

        PipelineRedLineCallbackVO pipelineRedLineCallbackVO = getRedLineIndicators(
                taskDetailVO,
                Lists.newArrayList(effectiveTools),
                toolName,
                buildId,
                newDefectList,
                extraParams
        );

        if (pipelineRedLineCallbackVO == null || CollectionUtils.isEmpty(pipelineRedLineCallbackVO.getData())) {
            log.info("red line callback is empty: {} {} {}", taskDetailVO.getTaskId(), toolName, buildId);
            return;
        }

        List<RedLineEntity> redLineEntities = pipelineRedLineCallbackVO.getData()
                .stream()
                .distinct()
                .map(redLineVO -> {
                    RedLineEntity redLineEntity = new RedLineEntity();
                    BeanUtils.copyProperties(redLineVO, redLineEntity);
                    redLineEntity.setBuildId(buildId);
                    redLineEntity.setTaskId(taskDetailVO.getTaskId());
                    redLineEntity.applyAuditInfo("system", "system");

                    if (redLineVO.getDimensionByChecker() != null) {
                        RedLineEntity.Dimension dimensionEntity = new RedLineEntity.Dimension();
                        BeanUtils.copyProperties(redLineVO.getDimensionByChecker(), dimensionEntity);
                        redLineEntity.setDimensionByChecker(dimensionEntity);

                        log.info("saveRedLineData, task id: {}, build id: {}, tool name: {}, vo: {}, entity: {}",
                                taskDetailVO.getTaskId(), buildId, toolName, redLineVO.getDimensionByChecker(),
                                dimensionEntity);
                    }

                    return redLineEntity;
                }).collect(Collectors.toList());

        redLineRepository.saveAll(redLineEntities);
    }

    /**
     * 查询质量红线指标数据
     *
     * @param taskDetailVO
     * @param effectiveTools
     * @return
     */
    private PipelineRedLineCallbackVO getRedLineIndicators(
            TaskDetailVO taskDetailVO,
            List<String> effectiveTools,
            String toolName,
            String buildId,
            List<T> newDefectList,
            RedLineExtraParams<T> extraParams
    ) {
        log.info("get red line indicators : {} {} {}", taskDetailVO.getTaskId(), toolName, buildId);
        // 拼装请求数据
        PipelineRedLineCallbackVO metadataCallback = new PipelineRedLineCallbackVO();
        String elementType = ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(
                taskDetailVO.getCreateFrom()
        ) ? LINUX_PAAS_CODECC_SCRIPT : LINUX_CODECC_SCRIPT;
        metadataCallback.setElementType(elementType);
        metadataCallback.setData(Lists.newArrayList());

        // 如果当前任务工具存在收敛告警文件，那么相应质量红线指标为null
        FileDefectGatherVO fileDefectGatherVO = fileDefectGatherService.getFileDefectGather(
                new HashMap<Long, List<String>>() {{
                    put(taskDetailVO.getTaskId(), Lists.newArrayList(toolName));
                }}
        );

        if (fileDefectGatherVO != null) {
            log.info("file defect gather and not create red line data: {} {} {}",
                    taskDetailVO.getTaskId(), toolName, buildId);
            return metadataCallback;
        }

        // 获取所有工具配置信息
        Map<String, ToolConfigInfoVO> toolConfigBaseMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskDetailVO.getToolConfigInfoList())) {
            for (ToolConfigInfoVO toolConfigInfoVO : taskDetailVO.getToolConfigInfoList()) {
                toolConfigBaseMap.put(toolConfigInfoVO.getToolName(), toolConfigInfoVO);
            }
        }

        // 查询元数据
        ToolMetaBaseVO toolMeta = toolMetaCacheService.getToolBaseMetaCache(toolName);
        ToolConfigInfoVO toolConfig = toolConfigBaseMap.get(toolName);
        log.info("[ red line ] current reporting tool: {} {} {}", taskDetailVO.getTaskId(), buildId, toolMeta);

        if (toolMeta == null) {
            return metadataCallback;
        }

        // 查询元数据模板
        Map<String, RedLineVO> metadataModel = RED_LINE_META_CACHE.getUnchecked(toolName);
        // 按工具统计指标
        getAnalysisResult(taskDetailVO, toolMeta, toolConfig, metadataModel, metadataCallback, effectiveTools,
                buildId, newDefectList, extraParams);

        return metadataCallback;
    }

    /**
     * 拼装元数据
     *
     * @param metadataKey
     * @param value
     * @param metadataModel
     * @param metadataCallback
     */
    protected void updateValue(String metadataKey, String value, String toolName, Map<String, RedLineVO> metadataModel,
            PipelineRedLineCallbackVO metadataCallback) {
        RedLineVO redLineVO = new RedLineVO();
        if (!metadataModel.containsKey(metadataKey)) {
            String cnName = metadataKey;
            String metaCnName = COMMON_META_SUFFIX_CN_NAME_MAP.get(StringUtils.removeStart(metadataKey, toolName));
            if (metaCnName != null) {
                cnName = toolName + metaCnName;
            } else {
                log.info("fail to get common meta suffix cn name: {}", StringUtils.removeStart(metadataKey, toolName));
            }

            redLineVO.setEnName(metadataKey);
            redLineVO.setCnName(cnName);
            redLineVO.setDetail(toolName);
            redLineVO.setExtra("");
            redLineVO.setMsg("");
            redLineVO.setType("INT");
        } else {
            BeanUtils.copyProperties(metadataModel.get(metadataKey), redLineVO);
        }
        redLineVO.setValue(value);
        metadataCallback.getData().add(redLineVO);
    }

    /**
     * 根据规则标签统计维度信息
     *
     * @param severity
     * @param fileType
     * @param checkerCategory
     * @param rlDimensionVO
     * @param defectEntityId
     */
    protected void calcDimensionByCheckerTag(
            int severity,
            ComConstants.FileType fileType,
            String checkerCategory,
            RLDimensionVO rlDimensionVO,
            String defectEntityId
    ) {
        String key = checkerCategory + fileType.name() + severity;
        Consumer<RLDimensionVO> calculator = DIMENSION_CALCULATOR_MAP.get(key);

        if (calculator != null) {
            calculator.accept(rlDimensionVO);
        } else {
            log.warn("get calculator fail, defect entity id: {}", defectEntityId);
        }
    }

    /**
     * 初始化维度数据
     * 注：
     * 1、虽然规则只会涉及维度里的其中1个指标，但整体维度下的所有指标均需带出0值
     * 2、null在红线中有特别的含义，表示用户红线指标有配置，但用户的规则没有对应的指标
     *
     * @param toolName
     * @param rlDimensionVO
     */
    protected void initDimension(String toolName, RLDimensionVO rlDimensionVO) {
        Set<String> checkerCategorySet = checkerRepository.findByToolName(toolName).stream()
                .map(CheckerDetailEntity::getCheckerCategory)
                .collect(Collectors.toSet());

        for (String checkerCategory : checkerCategorySet) {
            if (CheckerCategory.CODE_DEFECT.name().equalsIgnoreCase(checkerCategory)) {
                rlDimensionVO.initDefect();
            } else if (CheckerCategory.CODE_FORMAT.name().equalsIgnoreCase(checkerCategory)) {
                rlDimensionVO.initStandard();
            } else if (CheckerCategory.SECURITY_RISK.name().equalsIgnoreCase(checkerCategory)) {
                rlDimensionVO.initSecurity();
            }
        }
    }

    protected void setDimensionVO(PipelineRedLineCallbackVO metadataCallback, RLDimensionVO rlDimensionVO) {
        if (CollectionUtils.isEmpty(metadataCallback.getData())) {
            return;
        }

        // 记录到单工具类XXX_NEW_PROMPT维度
        metadataCallback.getData().stream()
                .filter(x -> x.getEnName().endsWith(RedLineConstants.NEW_PROMPT_SUFFIX))
                .findFirst()
                .ifPresent(x -> x.setDimensionByChecker(rlDimensionVO));
    }
}
