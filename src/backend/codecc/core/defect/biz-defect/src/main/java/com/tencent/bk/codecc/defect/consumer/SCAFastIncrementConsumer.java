/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService;
import com.tencent.bk.codecc.defect.service.sca.SCASbomService;
import com.tencent.bk.codecc.defect.service.sca.SCAVulnerabilityService;
import com.tencent.bk.codecc.defect.service.statistic.SCADefectStatisticServiceImpl;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.Triple;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SCA告警提交消息队列的消费者
 */
@Component
@Slf4j
public class SCAFastIncrementConsumer extends AbstractFastIncrementConsumer {

    @Autowired
    private SCADefectStatisticServiceImpl scaDefectStatisticServiceImpl;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Autowired
    private SCASbomService scaSbomService;

    @Autowired
    private SCALicenseService scaLicenseService;

    @Autowired
    private SCAVulnerabilityService scaVulnerabilityService;

    @Override
    protected void generateResult(AnalyzeConfigInfoVO analyzeConfigInfoVO) {
        long taskId = analyzeConfigInfoVO.getTaskId();
        String toolName = analyzeConfigInfoVO.getMultiToolType();
        String buildId = analyzeConfigInfoVO.getBuildId();

        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        String baseBuildId = toolBuildInfoService.getBaseBuildIdWhenDefectCommit(
                toolBuildStackEntity, taskId,
                toolName, buildId, true
        );
        SCASbomAggregateModel aggregateModel;
        List<SCAVulnerabilityEntity> vulnerabilities;
        List<SCALicenseEntity> licenseEntities;

        if (StringUtils.isNotEmpty(baseBuildId)) {
            Triple<SCASbomAggregateModel, List<SCAVulnerabilityEntity>, List<SCALicenseEntity>> triple =
                    getDefectListFromSnapshot(taskId, toolName, baseBuildId);
            aggregateModel = triple.getFirst();
            vulnerabilities = triple.getSecond();
            licenseEntities = triple.getThird();
            log.info("lint fast incr, all new defect from snapshot, task id {}, tool name {}, "
                            + "base build id {}, package size {}, vul size {}, license size: {}",
                    taskId, toolName, baseBuildId, aggregateModel.getPackages().size(), vulnerabilities.size(),
                    licenseEntities.size());
        } else {
            /*
             * 这里不能直接从告警表取数据，可能会引入并发脏数据
             * 场景：#1 master、#2 dev、#3 master
             * 假定#2工具侧扫描上报数据延迟，导致写入仓库信息延迟
             * 当#3开启时，判定最后一次仓库没变化，进而走超快增量，开始生成相关快照或者报表数据；而这一刻告警表有可能已经被dev的告警上报所覆盖
             */
            aggregateModel = new SCASbomAggregateModel(taskId, toolName, new SCASbomInfoEntity(taskId, toolName),
                    Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
            vulnerabilities = Lists.newArrayList();
            licenseEntities = Lists.newArrayList();
            log.error("sca fast incr, can not match base build id, task id {}, tool name {}, build id {}",
                    taskId, toolName, buildId);
        }

        // 更新构建告警快照
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        if (null == buildEntity) {
            buildEntity = new BuildEntity();
        }
        log.info("sca fast incr, save build defect, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
        buildSnapshotService.saveSCASnapshot(taskId, toolName, buildId, buildEntity, aggregateModel, vulnerabilities,
                licenseEntities);

        log.info("sca fast incr, statistic, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(analyzeConfigInfoVO.getNameEn());
        // 统计本次扫描的告警
        scaDefectStatisticServiceImpl.statistic(new DefectStatisticModel<>(
                taskVO,
                toolName,
                buildId,
                toolBuildStackEntity,
                vulnerabilities,
                false,
                aggregateModel,
                licenseEntities
        ));

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);

        log.info("sca fast incr, task id {}, tool name {}, build id {}", taskId, toolName, buildId);
    }

    /**
     * 从快照表获取指定构建版本的SCA告警数据
     *
     * @param taskId      任务ID
     * @param toolName    工具名称
     * @param baseBuildId 基准构建ID（用于获取历史快照）
     * @return Triple包含三个元素：
     *         第一项：SCA软件包聚合模型（过滤后仅含NEW状态的包）
     *         第二项：新增漏洞实体列表（从快照获取）
     *         第三项：新增许可证实体列表（从快照获取）
     */
    private Triple<SCASbomAggregateModel, List<SCAVulnerabilityEntity>,
            List<SCALicenseEntity>> getDefectListFromSnapshot(
            Long taskId,
            String toolName,
            String baseBuildId
    ) {
        // 从SCA组件服务获取指定构建版本的SBOM快照
        SCASbomAggregateModel aggregateModel = scaSbomService.getSCASbomSnapshot(taskId, toolName, baseBuildId);

        // 过滤状态为NEW的软件包（排除非新增状态的包）
        List<SCASbomPackageEntity> packages = aggregateModel.getPackages().stream()
                .filter(it -> it.getStatus() == DefectStatus.NEW.value()).collect(Collectors.toList());
        aggregateModel.setPackages(packages);

        // 从漏洞服务获取新增漏洞列表
        List<SCAVulnerabilityEntity> vulnerabilities = scaVulnerabilityService.getNewVulnerabilitiesFromSnapshot(
                taskId, toolName, baseBuildId
        );

        // 从许可证服务获取新增许可证列表
        List<SCALicenseEntity> licenses = scaLicenseService.getNewLicensesFromSnapshot(
                taskId, toolName, baseBuildId
        );

        return new Triple<>(aggregateModel, vulnerabilities, licenses);
    }
}
