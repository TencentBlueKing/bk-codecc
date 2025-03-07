package com.tencent.bk.codecc.defect.service.impl.redline;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_CODECC_SCRIPT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.LINUX_PAAS_CODECC_SCRIPT;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.RedLineRepository;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.RedLineEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.service.AbstractRedLineReportService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.RedLineConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.BeanUtils;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class RedLineReportServiceImpl extends AbstractRedLineReportService<DefectEntity> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedLineRepository redLineRepository;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    private static final List<String> DIMENSION_META_SUFFIX_LIST = Lists.newArrayList(
            RedLineConstants.HISTORY_SERIOUS_SUFFIX,
            RedLineConstants.HISTORY_NORMAL_SUFFIX,
            RedLineConstants.HISTORY_PROMPT_SUFFIX,
            RedLineConstants.NEW_SERIOUS_SUFFIX,
            RedLineConstants.NEW_NORMAL_SUFFIX,
            RedLineConstants.NEW_PROMPT_SUFFIX);

    @Override
    @Deprecated
    public void getAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo, ToolConfigInfoVO toolConfig,
            Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
            List<String> effectiveTools, String buildId, List<DefectEntity> newDefectList,
                                  RedLineExtraParams<DefectEntity> extraParams) {

    }

    /**
     * 统计更新质量红线数据(维度)
     *
     * @param taskId
     * @param buildId
     */
    public void updateDimensionRedLineData(Long taskId, String buildId) {
        boolean isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);

        if (isMigrationSuccessful) {
            updateDimensionRedLineData_V2(taskId, buildId);
        } else {
            updateDimensionRedLineData_Old(taskId, buildId);
        }
    }

    /**
     * 数据迁移后，红线维度指标按规则标签统计
     *
     * @param taskId
     * @param buildId
     */
    private void updateDimensionRedLineData_V2(Long taskId, String buildId) {
        List<RedLineEntity> redLineList = redLineRepository.findByBuildIdAndTaskId(buildId, taskId);
        redLineList = filterByLatestBuild(redLineList);
        List<RedLineEntity.Dimension> dimensionList = getRedLineDimension(redLineList);

        if (CollectionUtils.isEmpty(dimensionList)) {
            return;
        }

        // key: 指标名, value: 累计后的指标值
        Map<String, Integer> dimensionMap = Maps.newHashMapWithExpectedSize(18);
        Field[] fields = RedLineEntity.Dimension.class.getDeclaredFields();

        for (RedLineEntity.Dimension dimension : dimensionList) {
            for (Field field : fields) {
                int val = 0;
                try {
                    field.setAccessible(true);
                    Object obj = field.get(dimension);
                    if (obj == null) {
                        continue;
                    }

                    val = (Integer) obj;
                } catch (Throwable e) {
                    log.error("get value by reflection fail", e);
                }

                String key = field.getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class)
                        .value()
                        .toUpperCase(Locale.ENGLISH);
                Integer existVal = dimensionMap.getOrDefault(key, 0);
                dimensionMap.put(key, existVal + val);
            }
        }

        Map<String, RedLineVO> redLineMetaMap = getRedLineMetaForDimension();
        List<RedLineEntity> redLineEntityToSaveList = Lists.newArrayList();

        for (Entry<String, Integer> entry : dimensionMap.entrySet()) {
            String dimensionEnName = entry.getKey();
            RedLineVO redLineMeta = redLineMetaMap.get(dimensionEnName);
            if (redLineMeta == null) {
                log.warn("get tool red line meta fail: {}", dimensionEnName);
                continue;
            }

            RedLineEntity redLineEntity = new RedLineEntity();
            BeanUtils.copyProperties(redLineMeta, redLineEntity);
            redLineEntity.setBuildId(buildId);
            redLineEntity.setTaskId(taskId);
            redLineEntity.setValue(String.valueOf(entry.getValue()));
            redLineEntity.applyAuditInfoOnCreate();
            redLineEntityToSaveList.add(redLineEntity);
        }

        if (CollectionUtils.isNotEmpty(redLineEntityToSaveList)) {
            redLineRepository.saveAll(redLineEntityToSaveList);
        }
    }

    private Map<String, RedLineVO> getRedLineMetaForDimension() {
        List<String> dimensions = Stream.of(
                ToolType.DEFECT.name(),
                ToolType.STANDARD.name(),
                ToolType.SECURITY.name()
        ).collect(Collectors.toList());

        Map<String, RedLineVO> retMap = Maps.newHashMap();

        for (String dimension : dimensions) {
            Map<String, RedLineVO> metaMap = RED_LINE_META_CACHE.getUnchecked(dimension);
            retMap.putAll(metaMap);
        }

        return retMap;
    }

    private List<RedLineEntity.Dimension> getRedLineDimension(List<RedLineEntity> redLineList) {
        if (CollectionUtils.isEmpty(redLineList)) {
            return Lists.newArrayList();
        }

        Predicate<RedLineEntity> dimensionFilter = x ->
                x.getDimensionByChecker() != null && x.getEnName().endsWith(RedLineConstants.NEW_PROMPT_SUFFIX);

        return redLineList.stream()
                .filter(dimensionFilter)
                .map(RedLineEntity::getDimensionByChecker)
                .collect(Collectors.toList());
    }

    private void updateDimensionRedLineData_Old(Long taskId, String buildId) {
        log.info("start to update dimension red line data: {}, {}", taskId, buildId);
        Map<String, RedLineEntity> redLineMap = new HashMap<>();
        Set<String> toolNames = Arrays.stream(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER)
                .split(ComConstants.STRING_SPLIT)).collect(Collectors.toSet());

        // 以现有工具的红线数据为准，统计维度红线数据
        List<RedLineEntity> redLineResultList = redLineRepository.findByBuildId(buildId);
        int beforeSize = redLineResultList.size();
        redLineResultList = filterByLatestBuild(redLineResultList);
        int afterSize = redLineResultList.size();
        if (beforeSize != afterSize) {
            log.info("updateDimensionRedLineData duplicate data exists, taskId: {}, buildId: {}, before: {}, after: {}",
                    taskId, buildId, beforeSize, afterSize);
        }

        for (RedLineEntity entity : redLineResultList) {
            String enName = entity.getEnName();
            // 只统计新的、存量的数据
            Optional<String> suffix = DIMENSION_META_SUFFIX_LIST.stream()
                    .filter(enName::endsWith)
                    .findFirst();

            if (!suffix.isPresent()) {
                continue;
            }

            String type = getToolType(enName, suffix.get(), toolNames);
            // 可能存在维度类型的，需过滤掉
            if (StringUtils.isEmpty(type)) {
                continue;
            }

            String dimensionEnName = type + suffix.get();
            // 获取或者新建维度红线指标
            RedLineEntity redLineEntity = redLineMap.get(dimensionEnName);
            if (redLineEntity == null) {
                RedLineVO redLineMeta = RED_LINE_META_CACHE.getUnchecked(type).get(dimensionEnName);
                if (redLineMeta == null) {
                    log.warn("get tool red line meta fail: {}", dimensionEnName);
                    continue;
                }
                redLineEntity = new RedLineEntity();
                BeanUtils.copyProperties(redLineMeta, redLineEntity);
                redLineEntity.setBuildId(buildId);
                redLineEntity.setTaskId(taskId);
                redLineEntity.setValue("0");
                redLineEntity.applyAuditInfoOnCreate();
            }

            // 同维度数据相加
            Integer newValue = Integer.parseInt(redLineEntity.getValue()) + Integer.parseInt(entity.getValue());
            redLineEntity.setValue(String.valueOf(newValue));
            redLineMap.put(dimensionEnName, redLineEntity);
        }

        if (CollectionUtils.isNotEmpty(redLineMap.entrySet())) {
            redLineRepository.saveAll(redLineMap.values());
            log.info("finish to update dimension red line data: {}, {}, {}", taskId, buildId, redLineMap.size());
        }
    }

    /**
     * 考虑到重试，仅获取同buildId下最后一次构建的指标数据
     *
     * @param redLineList
     * @return
     */
    private List<RedLineEntity> filterByLatestBuild(List<RedLineEntity> redLineList) {
        // 新版本均填充了审计信息，旧版数据则不走去重逻辑了
        if (CollectionUtils.isEmpty(redLineList)
                || redLineList.get(0).getCreatedDate() == null) {
            return redLineList;
        }

        // 若多次重试，仅取出最后一次构建的相关红线指标
        HashMap<String, RedLineEntity> latestRedLineMap = Maps.newHashMapWithExpectedSize(redLineList.size());

        for (RedLineEntity newEntity : redLineList) {
            RedLineEntity oldEntity = latestRedLineMap.get(newEntity.getEnName());
            // 同一构建号下，流水线设计的重试是不可以并发的，所以重复的KEY取最大时间戳即可
            if (oldEntity != null) {
                if (oldEntity.getCreatedDate() != null
                        && newEntity.getCreatedDate() != null
                        && newEntity.getCreatedDate() > oldEntity.getCreatedDate()) {
                    latestRedLineMap.put(newEntity.getEnName(), newEntity);
                }
            } else {
                latestRedLineMap.put(newEntity.getEnName(), newEntity);
            }
        }

        return Lists.newArrayList(latestRedLineMap.values());
    }

    /**
     * 获取流水线质量红线数据
     *
     * @param taskDetailVO
     * @param buildId
     * @return
     */
    public PipelineRedLineCallbackVO getPipelineCallback(TaskDetailVO taskDetailVO, String buildId) {
        long taskId = taskDetailVO.getTaskId();
        PipelineRedLineCallbackVO metadataCallback = new PipelineRedLineCallbackVO();
        String elementType = ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().equals(taskDetailVO.getCreateFrom())
                ? LINUX_PAAS_CODECC_SCRIPT : LINUX_CODECC_SCRIPT;
        metadataCallback.setElementType(elementType);
        metadataCallback.setData(Lists.newArrayList());
        List<RedLineEntity> redLineEntities = redLineRepository.findByBuildId(buildId);

        if (CollectionUtils.isEmpty(redLineEntities)) {
            return metadataCallback;
        }

        // 筛选出当前任务产生的红线数据，避免同一条流水线下多个codecc任务时数据上报重复
        if (redLineEntities.stream().allMatch(it -> it.getTaskId() != null && it.getTaskId() != 0L)) {
            redLineEntities = redLineEntities.stream().filter(redLineEntity ->
                    redLineEntity.getTaskId() == taskId).collect(Collectors.toList());
        }

        redLineEntities = filterByLatestBuild(redLineEntities);

        for (RedLineEntity redLineEntity : redLineEntities) {
            RedLineVO redLineVO = new RedLineVO();
            BeanUtils.copyProperties(redLineEntity, redLineVO);
            metadataCallback.getData().add(redLineVO);
        }

        return metadataCallback;
    }

    private String getToolType(String enName, String suffix, Set<String> toolNames) {
        String toolName = enName.replace(suffix, "");
        if (!toolNames.contains(toolName)) {
            return "";
        }
        return toolMetaCacheService.getToolBaseMetaCache(toolName).getType();
    }
}
