package com.tencent.bk.codecc.defect.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 查询类补偿处理
 */
@Component
@Slf4j
public class QueryWarningLogicComponent {

    private static final int FIXED_VAL_IN_DB = DefectStatus.NEW.value() | DefectStatus.FIXED.value();
    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;
    @Autowired
    private BuildDefectV2Dao buildDefectV2Dao;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    /**
     * Lint后置补偿
     *
     * @param sourceList
     * @param buildId
     * @return
     */
    public List<LintDefectV2Entity> postHandleLintDefect(List<LintDefectV2Entity> sourceList, String buildId) {
        if (CollectionUtils.isEmpty(sourceList) || StringUtils.isEmpty(buildId)) {
            return sourceList;
        }

        long taskId = sourceList.get(0).getTaskId();
        Set<String> defectIdSet = getDefectIdSet(sourceList);
        Map<String, BuildDefectV2Entity> buildDefectMap = getSnapshotMap(taskId, buildId, defectIdSet);
        List<LintDefectV2Entity> retList = Lists.newArrayList();

        for (LintDefectV2Entity defect : sourceList) {
            String defectIdentity = commonDefectMigrationService.matchToolNameSet().contains(defect.getToolName())
                    ? defect.getId()
                    : defect.getEntityId();

            // 快照查，不存在对应的告警快照则剔除
            BuildDefectV2Entity snapshotDefect = buildDefectMap.get(defectIdentity);

            if (snapshotDefect == null) {
                continue;
            }

            convertToNewStatusIfFixed(defect);
            defect.setRevision(snapshotDefect.getRevision());
            defect.setBranch(snapshotDefect.getBranch());
            defect.setSubModule(snapshotDefect.getSubModule());
            defect.setLineNum(snapshotDefect.getLineNum());
            retList.add(defect);
        }

        return retList;
    }

    /**
     * Common类告警后置补偿
     *
     * @param sourceList
     * @param buildId
     * @return
     */
    public List<CommonDefectEntity> postHandleCommonDefect(List<CommonDefectEntity> sourceList, String buildId) {
        if (CollectionUtils.isEmpty(sourceList) || StringUtils.isEmpty(buildId)) {
            return sourceList;
        }

        long taskId = sourceList.get(0).getTaskId();
        Set<String> defectIdSet = getDefectIdSet(sourceList);
        Map<String, BuildDefectV2Entity> buildDefectMap = getSnapshotMap(taskId, buildId, defectIdSet);
        List<CommonDefectEntity> retList = Lists.newArrayList();

        for (CommonDefectEntity defect : sourceList) {
            // 快照查，不存在对应的告警快照则剔除
            BuildDefectV2Entity snapshotDefect = buildDefectMap.get(defect.getId());
            if (snapshotDefect == null) {
                continue;
            }

            convertToNewStatusIfFixed(defect);
            defect.setRevision(snapshotDefect.getRevision());
            defect.setLineNum(snapshotDefect.getLineNum());
            retList.add(defect);
        }

        return retList;
    }

    /**
     * CCN类告警后置补偿
     *
     * @param sourceList
     * @param buildId
     * @return
     */
    public List<CCNDefectEntity> postHandleCCNDefect(List<CCNDefectEntity> sourceList, String buildId) {
        if (CollectionUtils.isEmpty(sourceList) || StringUtils.isEmpty(buildId)) {
            return sourceList;
        }

        long taskId = sourceList.get(0).getTaskId();
        Set<String> defectIdSet = getDefectIdSet(sourceList);
        Map<String, BuildDefectV2Entity> buildDefectMap = getSnapshotMap(taskId, buildId, defectIdSet);
        List<CCNDefectEntity> retList = Lists.newArrayList();

        for (CCNDefectEntity defect : sourceList) {
            // 快照查，不存在对应的告警快照则剔除
            BuildDefectV2Entity snapshotDefect = buildDefectMap.get(defect.getEntityId());
            if (snapshotDefect == null) {
                continue;
            }

            convertToNewStatusIfFixed(defect);
            defect.setRevision(snapshotDefect.getRevision());
            defect.setBranch(snapshotDefect.getBranch());
            defect.setSubModule(snapshotDefect.getSubModule());
            defect.setStartLines(snapshotDefect.getStartLines());
            defect.setEndLines(snapshotDefect.getEndLines());
            retList.add(defect);
        }

        return retList;
    }

    /**
     * 获取告警快照Map
     *
     * @param taskId
     * @param buildId
     * @param defectIdSet
     * @return
     */
    private Map<String, BuildDefectV2Entity> getSnapshotMap(Long taskId,
            String buildId,
            Set<String> defectIdSet) {

        List<BuildDefectV2Entity> buildDefectList =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndDefectIdIn(taskId, buildId, defectIdSet);
        Map<String, BuildDefectV2Entity> buildDefectMap = buildDefectList.stream()
                .collect(Collectors.toMap(BuildDefectV2Entity::getDefectId, Function.identity(), (k1, k2) -> k1));

        log.info("getSnapshotDefectMap, task id: {}, defect id set size: {}, snapshot list size: {}",
                taskId,
                defectIdSet.size(),
                buildDefectList.size());

        return buildDefectMap;
    }

    /**
     * 获取告警唯一标识Set
     *
     * @param defectList
     * @return
     */
    private <T extends DefectEntity> Set<String> getDefectIdSet(List<T> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return Sets.newHashSet();
        }

        Function<DefectEntity, String> dataMapper = defectList.get(0) instanceof CommonDefectEntity
                ? x -> ((CommonDefectEntity) x).getId()
                : x -> {
                    if (x instanceof LintDefectV2Entity) {
                        LintDefectV2Entity defect = (LintDefectV2Entity) x;
                        if (commonDefectMigrationService.matchToolNameSet().contains(defect.getToolName())) {
                            return defect.getId();
                        }
                    }
                    return x.getEntityId();
                };

        return defectList.stream()
                .map(dataMapper)
                .collect(Collectors.toSet());
    }

    /**
     * 快照查，"已修复"修正为“待修复”
     */
    private void convertToNewStatusIfFixed(DefectEntity defect) {
        if (defect.getStatus() == FIXED_VAL_IN_DB) {
            defect.setStatus(DefectStatus.NEW.value());
            if (defect instanceof LintDefectV2Entity) {
                ((LintDefectV2Entity) defect).setMarkButNoFixed(false);
            } else if (defect instanceof CommonDefectEntity) {
                ((CommonDefectEntity) defect).setMarkButNoFixed(false);
            } else if (defect instanceof CCNDefectEntity) {
                ((CCNDefectEntity) defect).setMarkButNoFixed(false);
            }
        }
    }
}
