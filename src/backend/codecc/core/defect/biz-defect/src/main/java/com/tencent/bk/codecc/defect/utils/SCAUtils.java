package com.tencent.bk.codecc.defect.utils;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.BuildSCASbomPackageRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.BuildSCAVulnerabilityRepository;
import com.tencent.bk.codecc.defect.model.sca.BuildSCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.BuildSCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.service.LintQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SCAUtils {

    /**
     * 根据当次构建任务执行成功的工具列表，查询告警快照的 SCA 实体id列表
     * @param taskId
     * @param toolNameList
     * @param buildId
     * @param scaDimensionList
     * @return
     */
    public static Set<String> getPackageEntityIdsByBuildId(
            Long taskId,
            List<String> toolNameList,
            String buildId,
            List<String> scaDimensionList
    ) {
        // 查询执行成功的工具
        Set<String> successTools = getCommitSuccessTools(taskId, toolNameList, buildId);
        if (CollectionUtils.isEmpty(successTools)) {
            log.info("build {} commit success tools are empty", buildId);
            return Sets.newHashSet();
        }
        BuildSCASbomPackageRepository buildSCASbomPackageRepository =
                SpringContextUtil.Companion.getBean(BuildSCASbomPackageRepository.class);

        // 查询执行成功的工具告警快照的指定维度的 SCA 实体id列表
        Set<String> scaEntityIds = Sets.newHashSet();

        // 成功执行的工具的告警快照的组件实体id列表
        if (scaDimensionList.contains(ComConstants.SCADimenstion.PACKAGE.name())) {
            List<BuildSCASbomPackageEntity> retList =
                    buildSCASbomPackageRepository.findByTaskIdAndToolNameInAndBuildId(taskId, successTools, buildId);
            Set<String> packageEntityIds =
                    retList.stream().map(BuildSCASbomPackageEntity::getId).collect(Collectors.toSet());
            scaEntityIds.addAll(packageEntityIds);
        }

        return scaEntityIds;
    }

    /**
     * 查询当次构建任务执行成功的工具告警快照的 SCA 实体id列表
     * @param taskId
     * @param toolNameList
     * @param buildId
     * @param scaDimensionList
     * @return
     */
    public static Set<String> getVulnerabilityEntityIdsByBuildId(
            Long taskId,
            List<String> toolNameList,
            String buildId,
            List<String> scaDimensionList
    ) {
        // 查询执行成功的工具
        Set<String> successTools = getCommitSuccessTools(taskId, toolNameList, buildId);
        if (CollectionUtils.isEmpty(successTools)) {
            return Sets.newHashSet();
        }

        BuildSCAVulnerabilityRepository buildSCAVulnerabilityRepository =
                SpringContextUtil.Companion.getBean(BuildSCAVulnerabilityRepository.class);
        // 查询执行成功的工具告警快照的指定维度的 SCA 实体id列表
        Set<String> scaEntityIds = Sets.newHashSet();

        // 成功执行的工具的告警快照的漏洞实体id列表
        if (scaDimensionList.contains(ComConstants.SCADimenstion.VULNERABILITY.name())) {
            List<BuildSCAVulnerabilityEntity> retList =
                    buildSCAVulnerabilityRepository.findByTaskIdAndToolNameInAndBuildId(taskId, successTools, buildId);
            Set<String> vulnerabilityEntityIds =
                    retList.stream().map(BuildSCAVulnerabilityEntity::getId).collect(Collectors.toSet());
            scaEntityIds.addAll(vulnerabilityEntityIds);
        }

        return scaEntityIds;
    }

    /**
     * 查询当次构建任务执行成功的工具列表
     * @param taskId
     * @param toolNameList
     * @param buildId
     * @return
     */
    public static Set<String> getCommitSuccessTools(
            long taskId,
            List<String> toolNameList,
            String buildId
    ) {
        if (StringUtils.isEmpty(buildId)) {
            return Sets.newHashSet();
        }

        TaskLogService taskLogService =
                SpringContextUtil.Companion.getBean(TaskLogService.class);

        // 查询工具执行成功与否
        Map<String, Boolean> commitResult =
                taskLogService.defectCommitSuccess(
                        taskId,
                        toolNameList,
                        buildId,
                        ComConstants.Step4MutliTool.COMMIT.value()
                );

        // 筛出成功执行的工具
        return commitResult.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * 获取业务处理类型需要处理的告警状态
     * @param bizType
     * @return
     */
    public static Set<String> getStatusConditionByBizType(String bizType) {
        if (bizType.contains(ComConstants.BusinessType.IGNORE_DEFECT.value())) {
            // 对已经修复的BUG也可以进行忽略
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                    String.valueOf(ComConstants.DefectStatus.FIXED.value()));

        } else if (bizType.contains(ComConstants.BusinessType.REVERT_IGNORE.value())) {
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));

        } else if (bizType.contains(ComConstants.BusinessType.MARK_DEFECT.value())) {
            // 对于恢复忽略再标记的需要开放忽略
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
        } else if (bizType.contains(ComConstants.BusinessType.ASSIGN_DEFECT.value())) {
            // 对已忽略也可以进行处理人修改
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                    String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
        } else if (bizType.contains(ComConstants.BusinessType.CHANGE_IGNORE_TYPE.value())) {
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
        }
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
    }

}
