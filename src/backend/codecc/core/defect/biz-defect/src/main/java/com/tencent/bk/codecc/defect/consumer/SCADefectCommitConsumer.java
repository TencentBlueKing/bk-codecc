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

import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.LicenseDetailRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAPackageFileInfo;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomPackageBase;
import com.tencent.bk.codecc.defect.pojo.sbom.SCAScanContext;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.pojo.sbom.ScaDefectFileInfo;
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomPackage;
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulHitPackage;
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulRatingMethod;
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilityInfo;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService;
import com.tencent.bk.codecc.defect.service.sca.SCASbomService;
import com.tencent.bk.codecc.defect.service.sca.SCAVulnerabilityService;
import com.tencent.bk.codecc.defect.service.sca.SCADefectFileReader;
import com.tencent.bk.codecc.defect.service.statistic.SCADefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("scaDefectCommitConsumer")
@Slf4j
public class SCADefectCommitConsumer extends AbstractDefectCommitConsumer {


    @Autowired
    private SCASbomService scaSbomService;

    @Autowired
    private SCALicenseService scaLicenseService;

    @Autowired
    private SCAVulnerabilityService scaVulnerabilityService;

    @Autowired
    private BuildSnapshotService buildSnapshotService;

    @Autowired
    private SCADefectStatisticServiceImpl scaDefectStatisticServiceImpl;

    @Autowired
    private LicenseDetailRepository licenseDetailRepository;


    @Override
    protected boolean uploadDefects(
            CommitDefectVO commitDefectVO,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap
    ) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        String createFrom = taskVO.getCreateFrom();
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        // 获取扫描详情
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(
                taskId,
                toolName, buildId);


        // 查找SBOM文件信息
        String fileIndex = scmJsonComponent.getDefectFileIndex(streamName, toolName, buildId);
        if (StringUtils.isEmpty(fileIndex)) {
            log.warn("Can not find raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format(
                    "找不到的告警文件: %s, %s, %s", streamName, toolName, buildId), null);
        }
        //
        // 读取SBOM文件信息
        ScaDefectFileInfo scaDefectFileInfo = SCADefectFileReader.INSTANCE.readFromJsonFile(fileIndex);
        if (scaDefectFileInfo == null) {
            log.warn("Can not read raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format(
                    "读取的告警文件失败: %s, %s, %s", streamName, toolName, buildId), null);
        }
        RedisLock locker = null;
        SCASbomAggregateModel newAggregateModel;
        long tryBeginTime = System.currentTimeMillis();
        try {
            // 非工蜂项目上锁提单过程
            if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equals(createFrom) && !lockModeIsClosed()) {
                Pair<Boolean, RedisLock> pair = continueWithLock(commitDefectVO);
                Boolean beContinue = pair.getFirst();
                locker = pair.getSecond();

                if (!beContinue) {
                    return false;
                }
            }

            // 判断本次是增量还是全量扫描
            boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

            // 构建扫描信息上下文
            SCAScanContext scaScanContext = new SCAScanContext(scaDefectFileInfo, isFullScan,
                    scaDefectFileInfo.getIncrementalFiles(),
                    toolBuildStackEntity == null ? Collections.emptySet() : toolBuildStackEntity.getRootPaths(),
                    toolBuildStackEntity == null ? Collections.emptyList() : toolBuildStackEntity.getDeleteFiles(),
                    BooleanUtils.isTrue(commitDefectVO.isReallocate()));

            // 获取聚合模型
            SCASbomAggregateModel aggregateModel = getAggregateModel(taskId, toolName, buildEntity, scaScanContext,
                    fileChangeRecordsMap, codeRepoIdMap);
            newAggregateModel = uploadSCADefect(taskId, streamName, toolName, buildId, buildEntity, aggregateModel,
                    scaScanContext);
        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }
            log.info("defect commit, lock try to finally cost total: {}, {}, {}, {}",
                    System.currentTimeMillis() - tryBeginTime, taskId, toolName, buildId);
        }
        // 快照
        buildSnapshotService.saveSCASnapshot(taskId, toolName, buildId, buildEntity, newAggregateModel);
        // 统计
        scaDefectStatisticServiceImpl.statistic(new DefectStatisticModel<>(
                taskVO,
                toolName,
                buildId,
                toolBuildStackEntity,
                newAggregateModel.getVulnerabilities(),
                false,
                newAggregateModel
        ));

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
        return true;
    }

    private SCASbomAggregateModel getAggregateModel(long taskId, String toolName, BuildEntity buildEntity,
                                                    SCAScanContext scaScanContext,
                                                    Map<String, ScmBlameVO> fileChangeRecordsMap,
                                                    Map<String, RepoSubModuleVO> codeRepoIdMap) {
        SCASbomAggregateModel aggregateModel = getSbomAggregateBySbomType(taskId, toolName, scaScanContext,
                fileChangeRecordsMap, codeRepoIdMap);
        ScaDefectFileInfo scaDefectFileInfo = scaScanContext.getScaDefectFileInfo();
        if (scaDefectFileInfo.getAnalysisPackageFromSbom() != null && !scaDefectFileInfo.getAnalysisPackageFromSbom()) {
            // 不需要从SBom解析，直接读取Package结果
            List<SCASbomPackageEntity> packages = CollectionUtils.isEmpty(scaDefectFileInfo.getPackages())
                    ? Collections.emptyList() : scaDefectFileInfo.getPackages();
            packages.forEach(pkg -> {
                pkg.setTaskId(taskId);
                pkg.setToolName(toolName);
                pkg.setStatus(DefectStatus.NEW.value());
                if (CollectionUtils.isEmpty(pkg.getLicenses())
                        && (CollectionUtils.isNotEmpty(pkg.getLicensesConcluded())
                        || CollectionUtils.isNotEmpty(pkg.getLicensesDeclared()))) {
                    pkg.setLicenses(new ArrayList<>());
                    if (CollectionUtils.isNotEmpty(pkg.getLicensesConcluded())) {
                        pkg.getLicenses().addAll(pkg.getLicensesConcluded());
                    }
                    if (CollectionUtils.isNotEmpty(pkg.getLicensesDeclared())) {
                        pkg.getLicenses().addAll(pkg.getLicensesDeclared());
                    }
                }
            });
            // 设置文件变更记录
            fillPackageFileInfoByFilePath(packages, fileChangeRecordsMap, codeRepoIdMap);
            aggregateModel.setPackages(packages);
        }
        if (scaDefectFileInfo.getAnalysisLicenseFromSbom() != null && !scaDefectFileInfo.getAnalysisLicenseFromSbom()) {
            // 不需要从SBom解析，直接读取License结果
            List<SCALicenseEntity> licenses = CollectionUtils.isEmpty(scaDefectFileInfo.getLicenses())
                    ? Collections.emptyList() : scaDefectFileInfo.getLicenses();
            licenses.forEach(license -> {
                license.setTaskId(taskId);
                license.setToolName(toolName);
                license.setStatus(DefectStatus.NEW.value());
            });
            aggregateModel.setLicenses(licenses);
        }
        if (scaDefectFileInfo.getAnalysisVulnerabilityFromSbom() != null
                && !scaDefectFileInfo.getAnalysisVulnerabilityFromSbom()) {
            // 不需要从SBom解析，直接读取Vulnerability结果
            aggregateModel.setVulnerabilities(CollectionUtils.isEmpty(scaDefectFileInfo.getVulnerabilities())
                    ? Collections.emptyList() : convertVulInfoToVulDefects(taskId, toolName, buildEntity,
                    scaDefectFileInfo.getVulnerabilities()));
        }
        return aggregateModel;
    }

    private SCASbomAggregateModel getSbomAggregateBySbomType(long taskId, String toolName,
                                                             SCAScanContext scaScanContext,
                                                             Map<String, ScmBlameVO> fileChangeRecordsMap,
                                                             Map<String, RepoSubModuleVO> codeRepoIdMap) {
        SCASbomAggregateModel aggregateModel = null;
        ScaDefectFileInfo scaDefectFileInfo = scaScanContext.getScaDefectFileInfo();
        switch (scaDefectFileInfo.sbomType()) {
            case SPDX:
                // 从SPDX解析
                aggregateModel = scaDefectFileInfo.getSpdxSbom() != null
                        ? scaDefectFileInfo.getSpdxSbom().getSCASbomAggregateModel(taskId, toolName,
                        fileChangeRecordsMap, codeRepoIdMap) : null;
                break;
            case CYCLONEDX:
                // 从CycloneDX解析
                aggregateModel = scaDefectFileInfo.getCycloneDXSbom() != null
                        ? scaDefectFileInfo.getCycloneDXSbom().getSCASbomAggregateModel(taskId, toolName) : null;
                if (aggregateModel != null) {
                    fillPackageFileInfoByFilePath(aggregateModel.getPackages(), fileChangeRecordsMap, codeRepoIdMap);
                }
                break;
            default:
                break;
        }
        if (aggregateModel == null) {
            // 新建
            aggregateModel = new SCASbomAggregateModel(taskId, toolName, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
        }
        return aggregateModel;
    }

    private void fillPackageFileInfoByFilePath(List<SCASbomPackageEntity> packages,
                                               Map<String, ScmBlameVO> fileChangeRecordsMap,
                                               Map<String, RepoSubModuleVO> codeRepoIdMap) {

        // 先将 fileChangeRecordsMap 转为 filePath 的模式，SBOM 文件信息中的文件都是相对路径，所以只能用相对路径匹配
        if (packages == null || packages.isEmpty() || fileChangeRecordsMap == null || fileChangeRecordsMap.isEmpty()) {
            return;
        }

        Map<String, ScmBlameVO> pathToBlame = new HashMap<>(fileChangeRecordsMap.size());
        for (Map.Entry<String, ScmBlameVO> entry : fileChangeRecordsMap.entrySet()) {
            ScmBlameVO blame = entry.getValue();
            if (blame == null) {
                continue;
            }
            String filePath = blame.getFilePath();
            if (StringUtils.isNotEmpty(filePath)) {
                pathToBlame.put(filePath, blame);
            }
        }
        for (SCASbomPackageEntity pkg : packages) {
            List<SCAPackageFileInfo> fileInfos = pkg.getFileInfos();
            if (CollectionUtils.isEmpty(fileInfos)) {
                continue;
            }
            Set<String> authors = new HashSet<>();
            for (SCAPackageFileInfo fileInfo : fileInfos) {
                String filePath = fileInfo.getFilePath();
                if (StringUtils.isNotEmpty(filePath)) {
                    ScmBlameVO blame = pathToBlame.get(filePath);
                    if (blame == null) {
                        continue;
                    }
                    String author = ToolParamUtils.trimUserName(blame.getFileAuthor());
                    authors.add(author);
                    fileInfo.setAuthors(Collections.singletonList(author));
                    fileInfo.setLastUpdateTime(blame.getFileUpdateTime());
                    fileInfo.setRelPath(blame.getFileRelPath());
                    // 根据filePath 获取文件名
                    String fileName = StringUtils.substringAfterLast(filePath, "/");
                    fileInfo.setFileName(fileName);
                    if (pkg.getLastUpdateTime() != 0 && blame.getFileUpdateTime() < pkg.getLastUpdateTime()) {
                        continue;
                    }
                    // 更新版本等信息
                    pkg.setLastUpdateTime(blame.getFileUpdateTime());
                    pkg.setBranch(blame.getBranch());
                    pkg.setUrl(blame.getUrl());
                    pkg.setRevision(blame.getRevision());
                    if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(blame.getScmType())) {
                        RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(blame.getRootUrl());
                        if (repoSubModuleVO != null) {
                            pkg.setRepoId(repoSubModuleVO.getRepoId());
                        }
                    } else {
                        RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(blame.getUrl());
                        if (repoSubModuleVO != null) {
                            pkg.setRepoId(repoSubModuleVO.getRepoId());
                            if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                                pkg.setSubModule(repoSubModuleVO.getSubModule());
                            } else {
                                pkg.setSubModule(ComConstants.EMPTY_STRING);
                            }
                        }
                    }
                }
            }
            pkg.setAuthor(new ArrayList<>(authors));
        }
    }

    /**
     * 获取许可证详情映射关系
     *
     * @param packages 需要获取许可证详情的证书名
     * @return 许可证名称到许可证详情的映射Map，key为许可证名称或别名，value为许可证详情实体
     */
    private Map<String, LicenseDetailEntity> getLicenseDetailMappingByPackage(List<SCASbomPackageEntity> packages) {
        if (CollectionUtils.isEmpty(packages)) {
            return Collections.emptyMap();
        }
        return getLicenseDetailMapping(packages.stream().map(SCASbomPackageEntity::getLicenses)
                .flatMap(List::stream).map(String::trim).collect(Collectors.toList()));
    }

    /**
     * 获取许可证详情映射关系
     *
     * @param licenses 需要获取许可证详情的证书名
     * @return 许可证名称到许可证详情的映射Map，key为许可证名称或别名，value为许可证详情实体
     */
    private Map<String, LicenseDetailEntity> getLicenseDetailMapping(List<String> licenses) {
        // 从组件包中获取所有许可证名称并去重
        Set<String> licenseNames = new HashSet<>(licenses);
        // 根据许可证名称或别名查询许可证详情
        List<LicenseDetailEntity> details = licenseDetailRepository.findByNameInOrAliasIn(licenseNames, licenseNames);
        Map<String, LicenseDetailEntity> licenseNameToDetails = new HashMap<>();
        for (LicenseDetailEntity detail : details) {
            // 添加许可证名称到详情的映射
            licenseNameToDetails.put(detail.getName(), detail);
            // 获取别名列表，并遍历添加到映射
            if (CollectionUtils.isNotEmpty(detail.getAlias())) {
                for (String alias : detail.getAlias()) {
                    licenseNameToDetails.put(alias, detail);
                }
            }
        }
        return licenseNameToDetails;
    }

    /**
     * 上传SCA缺陷检测结果（支持全量/增量模式）
     *
     * @param taskId         任务唯一标识
     * @param streamName     代码流名称
     * @param toolName       扫描工具名称
     * @param buildId        当前构建ID
     * @param buildEntity    构建实体对象
     * @param aggregateModel SBOM聚合数据模型
     * @param scaScanContext 扫描上下文
     * @return 更新后的SBOM聚合模型，包含最新的组件、文件、代码片段及其关系信息
     * @throws Exception 处理过程中可能抛出的异常
     */
    private SCASbomAggregateModel uploadSCADefect(long taskId, String streamName, String toolName, String buildId,
                                                  BuildEntity buildEntity, SCASbomAggregateModel aggregateModel,
                                                  SCAScanContext scaScanContext) {
        log.info("[SCA] Start {} upload SCA defect, taskId={}, streamName={}, toolName={}, buildId={}",
                scaScanContext.getFullScan() ? "full" : "incremental", taskId, streamName, toolName, buildId);

        try {
            // 获取并更新SBOM组件信息
            log.debug("[SCA] Start upload sbom packages");
            uploadSbomPackages(taskId, toolName, buildEntity, aggregateModel.getPackages(), scaScanContext);
            // 获取保存更新后，新的组件信息
            log.debug("[SCA] Start get new sca packages");
            List<SCASbomPackageEntity> packageEntities =
                    scaSbomService.getNewPackagesByTaskIdAndToolName(taskId, toolName);
            // 更新漏洞
            log.debug("[SCA] Start batch upload vulnerability");
            batchUploadVulnerability(taskId, toolName, buildId, buildEntity, aggregateModel.getPackages(),
                    packageEntities, aggregateModel.getVulnerabilities());
            // 更新证书
            log.debug("[SCA] Start batch upload license");
            batchUploadLicense(taskId, toolName, buildId, buildEntity, aggregateModel.getLicenses());
            // 获取更新后的证书与漏洞
            log.debug("[SCA] Start get new vulnerabilities and licenses");
            List<SCAVulnerabilityEntity> vulnerabilities = scaVulnerabilityService.getNewVulDefect(taskId, toolName);
            List<SCALicenseEntity> licenseEntities =
                    scaLicenseService.getNewLicenseByTaskIdAndToolName(taskId, toolName);
            log.info("[SCA] Finish full upload SCA defect successfully, taskId={}", taskId);
            return new SCASbomAggregateModel(taskId, toolName, packageEntities, vulnerabilities, licenseEntities);
        } catch (Exception e) {
            log.error("[SCA] Failed to full upload SCA defect, taskId={}, error={}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 保存SBOM组件信息
     *
     * @param packages
     */
    private void uploadSbomPackages(long taskId, String toolName, BuildEntity buildEntity,
                                    List<SCASbomPackageEntity> packages, SCAScanContext scaScanContext) {
        if (CollectionUtils.isEmpty(packages)) {
            return;
        }
        // 规范Package 证书名称
        Map<String, LicenseDetailEntity> licenseDetailEntityMap = getLicenseDetailMappingByPackage(packages);
        for (SCASbomPackageEntity sbomPackage : packages) {
            if (CollectionUtils.isEmpty(sbomPackage.getLicenses())) {
                continue;
            }
            sbomPackage.setLicenses(sbomPackage.getLicenses().stream()
                    .map(it -> licenseDetailEntityMap.containsKey(it) ? licenseDetailEntityMap.get(it).getName() : it)
                    .collect(Collectors.toList()));
        }
        // 获取组件名称和版本列表
        Map<String, List<String>> nameToVersions = packages.stream()
                .filter(it -> it != null && StringUtils.isNotEmpty(it.getName()))
                .collect(Collectors.groupingBy(SCASbomPackageEntity::getName,
                        Collectors.mapping(SCASbomPackageEntity::getVersion, Collectors.toList())));
        // 获取数据库中的对象
        List<SCASbomPackageEntity> oldScaSbomPackages = scaSbomService.getPackagesByNameAndVersions(
                taskId, toolName, nameToVersions);

        // 默认下，包名+版本唯一确定一个组件，如果工具多报了，也不能抛弃
        Map<String, List<SCASbomPackageEntity>> newPackageMap = packages.stream().collect(
                Collectors.groupingBy(it -> it.getName() + ComConstants.KEY_UNDERLINE + it.getVersion()));
        Map<String, List<SCASbomPackageEntity>> oldPackageMap = oldScaSbomPackages.stream().collect(
                Collectors.groupingBy(it -> it.getName() + ComConstants.KEY_UNDERLINE + it.getVersion()));
        List<SCASbomPackageEntity> upsertPackages = new LinkedList<>();
        long currentTimeMillis = System.currentTimeMillis();
        for (Entry<String, List<SCASbomPackageEntity>> entry : newPackageMap.entrySet()) {
            List<SCASbomPackageEntity> samePackages = entry.getValue();
            List<SCASbomPackageEntity> oldPackages = oldPackageMap.get(entry.getKey());
            // 如果旧的没有，直接新增
            if (oldPackages == null) {
                samePackages.forEach(samePackage -> {
                    samePackage.setCreateTime(currentTimeMillis);
                    if (buildEntity != null) {
                        samePackage.setCreateBuildNumber(buildEntity.getBuildNo());
                    }
                });
                upsertPackages.addAll(samePackages);
            } else {
                // 如果旧的有，则更新
                processSamePackages(samePackages, oldPackages, upsertPackages, buildEntity, scaScanContext,
                        currentTimeMillis);
            }
        }
        // 获取所有的新告警，本次没有上报的都标记为已修复
        List<SCASbomPackageEntity> newPackages = scaSbomService.getNewPackagesByTaskIdAndToolName(taskId, toolName);
        List<SCASbomPackageEntity> needClosedPackages = newPackages.stream()
                // 1.过滤本次没有上报的组件
                .filter(it ->
                        StringUtils.isEmpty(it.getName()) || !nameToVersions.containsKey(it.getName())
                                || !(nameToVersions.get(it.getName()).contains(it.getVersion())
                                || (StringUtils.isEmpty(it.getVersion())
                                && nameToVersions.get(it.getName()).isEmpty())))
                // 2. 如果全量扫描那么全部没有上报的组件标记修复，如果是增量扫描，需要判断引入文件路径是否在本次扫描的列表中
                .filter(it -> scaScanContext.getFullScan()
                        || (CollectionUtils.isNotEmpty(it.getFileInfos())
                        && it.getFileInfos().stream().anyMatch(fileInfo ->
                        scaScanContext.isFileScanned(fileInfo.getFilePath(), fileInfo.getRelPath()))))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(needClosedPackages)) {
            long curTime = System.currentTimeMillis();
            for (SCASbomPackageEntity needClosedPackage : needClosedPackages) {
                closedSCASbomPackage(needClosedPackage, curTime, buildEntity, scaScanContext);
            }
            upsertPackages.addAll(needClosedPackages);
        }
        scaSbomService.saveSbomPackages(taskId, upsertPackages);
    }


    private void processSamePackages(List<SCASbomPackageEntity> newPackages, List<SCASbomPackageEntity> oldPackages,
                                     List<SCASbomPackageEntity> upsertPackages, BuildEntity buildEntity,
                                     SCAScanContext scaScanContext, Long currentTimeMillis) {
        // 按列表一一匹配处理
        long curTime = System.currentTimeMillis();
        for (int i = 0; i < newPackages.size(); i++) {
            SCASbomPackageEntity newPackage = newPackages.get(i);
            if (oldPackages.size() <= i) {
                newPackage.setCreateTime(currentTimeMillis);
                if (buildEntity != null) {
                    newPackage.setCreateBuildNumber(buildEntity.getBuildNo());
                }
                upsertPackages.add(newPackage);
                continue;
            }
            SCASbomPackageEntity oldPackage = oldPackages.get(i);
            updateOldPackageInfo(newPackage, oldPackage, scaScanContext);
            // 记录ID，给到后续的漏洞关联使用
            newPackage.setEntityId(oldPackage.getEntityId());
            // 更新状态
            if ((oldPackage.getStatus() & DefectStatus.FIXED.value()) > 0) {
                openSCASbomPackage(oldPackage, newPackage);
            }
            upsertPackages.add(oldPackage);
        }
        if (oldPackages.size() > newPackages.size()) {
            // 将老告警中的待修复标记为已修复
            List<SCASbomPackageEntity> needUpdateList = oldPackages.subList(newPackages.size(), oldPackages.size())
                    .stream().filter(it -> it.getStatus() == DefectStatus.NEW.value()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(needUpdateList)) {
                for (SCASbomPackageEntity scaSbomPackage : needUpdateList) {
                    closedSCASbomPackage(scaSbomPackage, curTime, buildEntity, scaScanContext);
                }
                upsertPackages.addAll(needUpdateList);
            }
        }
    }

    private void openSCASbomPackage(SCASbomPackageEntity oldPackage, SCASbomPackageEntity newPakcage) {
        // 修复重新打开
        oldPackage.setStatus(DefectStatus.NEW.value());
        oldPackage.setFixedTime(null);
        oldPackage.setFixedBuildNumber(null);
        oldPackage.setFixedBuildId(null);
        // 如果是重新打开，重置FileInfo
        oldPackage.setFileInfos(newPakcage.getFileInfos());
    }

    private void closedSCASbomPackage(SCASbomPackageEntity scaSbomPackage, Long curTime, BuildEntity buildEntity,
                                      SCAScanContext scaScanContext) {
        if (scaScanContext.getFullScan() || CollectionUtils.isEmpty(scaSbomPackage.getFileInfos())) {
            scaSbomPackage.setStatus(DefectStatus.NEW.value() | DefectStatus.FIXED.value());
            scaSbomPackage.setFixedTime(curTime);
            scaSbomPackage.setFixedBuildNumber(buildEntity == null ? null : buildEntity.getBuildNo());
            scaSbomPackage.setFixedBuildId(buildEntity == null ? null : buildEntity.getBuildId());
            return;
        }
        // 增量扫描需要考虑是否还有其他文件引入该组件
        List<SCAPackageFileInfo> fileInfos = scaSbomPackage.getFileInfos().stream().filter(it ->
                        !scaScanContext.isFileScanned(it.getFilePath(), it.getRelPath()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(fileInfos)) {
            // 更新引入文件路径列表
            scaSbomPackage.setFileInfos(fileInfos);
        } else {
            // 没有其他文件引入了，保留引入的文件，文件标识修复
            scaSbomPackage.setStatus(DefectStatus.NEW.value() | DefectStatus.FIXED.value());
            scaSbomPackage.setFixedTime(curTime);
            scaSbomPackage.setFixedBuildNumber(buildEntity == null ? null : buildEntity.getBuildNo());
            scaSbomPackage.setFixedBuildId(buildEntity == null ? null : buildEntity.getBuildId());
        }
    }

    private void updateOldPackageInfo(SCASbomPackageEntity newPackage, SCASbomPackageEntity oldPackage,
                                      SCAScanContext scaScanContext) {
        if (newPackage == null || oldPackage == null) {
            return;
        }
        // 显式复制需要更新的字段
        oldPackage.setName(newPackage.getName());
        oldPackage.setVersion(newPackage.getVersion());
        oldPackage.setElementId(newPackage.getElementId());
        oldPackage.setLineNum(newPackage.getLineNum());
        oldPackage.setRevision(newPackage.getRevision());
        oldPackage.setBranch(newPackage.getBranch());
        oldPackage.setSubModule(newPackage.getSubModule());
        oldPackage.setUrl(newPackage.getUrl());
        oldPackage.setRepoId(newPackage.getRepoId());
        oldPackage.setLastUpdateTime(newPackage.getLastUpdateTime());
        oldPackage.setLicenses(newPackage.getLicenses());
        oldPackage.setChecksums(newPackage.getChecksums());
        oldPackage.setSeverity(newPackage.getSeverity());
        oldPackage.setDescription(newPackage.getDescription());
        oldPackage.setDownloadLocation(newPackage.getDownloadLocation());
        oldPackage.setHomepage(newPackage.getHomepage());
        oldPackage.setOriginator(newPackage.getOriginator());
        oldPackage.setPackageFileName(newPackage.getPackageFileName());
        oldPackage.setSourceInfo(newPackage.getSourceInfo());
        oldPackage.setSummary(newPackage.getSummary());
        oldPackage.setSupplier(newPackage.getSupplier());
        oldPackage.setFilesAnalyzed(newPackage.getFilesAnalyzed());
        oldPackage.setExternalRefs(newPackage.getExternalRefs());
        oldPackage.setLicensesConcluded(newPackage.getLicensesConcluded());
        oldPackage.setLicensesDeclared(newPackage.getLicensesDeclared());
        oldPackage.setDepth(newPackage.getDepth());
        oldPackage.setLastUpdateTime(newPackage.getLastUpdateTime());
        if (scaScanContext.getFullScan()) {
            oldPackage.setFileInfos(newPackage.getFileInfos());
        } else {
            List<SCAPackageFileInfo> fileInfos =
                    CollectionUtils.isEmpty(newPackage.getFileInfos()) ? new ArrayList<>() :
                            newPackage.getFileInfos();
            fileInfos.addAll(CollectionUtils.isEmpty(oldPackage.getFileInfos()) ? Collections.emptyList() :
                    oldPackage.getFileInfos().stream().filter(it ->
                                    !scaScanContext.isFileScanned(it.getFilePath(), it.getRelPath()))
                            .collect(Collectors.toList()));
            oldPackage.setFileInfos(fileInfos);
        }
        if (scaScanContext.getFullScan() && scaScanContext.getReallocate()) {
            oldPackage.setAuthor(newPackage.getAuthor());
        } else if (scaScanContext.getReallocate()) {
            oldPackage.setAuthor(getAuthorFromPackageFileInfos(oldPackage.getFileInfos()));
        }
    }

    /**
     * 从fileInfos中提取合并处理人信息
     *
     * @param fileInfos
     * @return
     */
    private List<String> getAuthorFromPackageFileInfos(List<SCAPackageFileInfo> fileInfos) {
        if (CollectionUtils.isEmpty(fileInfos)) {
            return Collections.emptyList();
        }
        return fileInfos.stream().filter(it -> it != null && CollectionUtils.isNotEmpty(it.getAuthors()))
                .flatMap(info -> info.getAuthors().stream()).distinct().filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    private List<SCAVulnerabilityEntity> convertVulInfoToVulDefects(Long taskId, String toolName,
                                                                    BuildEntity buildEntity,
                                                                    List<SCAVulnerabilityInfo> vulnerabilityInfos) {
        if (CollectionUtils.isEmpty(vulnerabilityInfos)) {
            return Collections.emptyList();
        }
        long curTime = System.currentTimeMillis();
        List<SCAVulnerabilityEntity> vulDefects = new ArrayList<>();
        for (SCAVulnerabilityInfo vulnerabilityInfo : vulnerabilityInfos) {
            if (CollectionUtils.isEmpty(vulnerabilityInfo.getHitPackages())) {
                continue;
            }
            for (SCAVulHitPackage hitPackage : vulnerabilityInfo.getHitPackages()) {
                if (StringUtils.isEmpty(hitPackage.getPackageName())) {
                    continue;
                }
                vulDefects.add(createVulEntityFromVulInfo(taskId, toolName, vulnerabilityInfo, hitPackage,
                        buildEntity, curTime));

            }
        }
        return vulDefects;
    }


    private void batchUploadVulnerability(Long taskId, String toolName, String buildId, BuildEntity buildEntity,
                                          List<SCASbomPackageEntity> oriPackages,
                                          List<SCASbomPackageEntity> newPackages,
                                          List<SCAVulnerabilityEntity> vulnerabilities) {
        log.info("save vulnerability start, taskId={}, toolName={}, buildId={}", taskId, toolName, buildId);
        if (CollectionUtils.isEmpty(vulnerabilities)) {
            closedAllNewVulDefect(taskId, toolName, buildEntity);
            return;
        }
        // package信息准备
        Map<String, SCASbomPackageEntity> packageIdToEntities = CollectionUtils.isEmpty(oriPackages)
                ? Collections.emptyMap() : oriPackages.stream().filter(it -> StringUtils.isNotBlank(it.getName()))
                .collect(Collectors.toMap(SbomPackageBase::getPackageId, Function.identity(),
                        (existing, replacement) -> existing));
        Set<String> validPackageIds = CollectionUtils.isEmpty(newPackages) ? Collections.emptySet() :
                newPackages.stream().map(SbomPackageBase::getPackageId).collect(Collectors.toSet());

        // 获取新的告警
        long curTime = System.currentTimeMillis();
        for (SCAVulnerabilityEntity vulnerability : vulnerabilities) {
            List<String> authors = packageIdToEntities.containsKey(vulnerability.getPackageId())
                    ? packageIdToEntities.get(vulnerability.getPackageId()).getAuthor() : Collections.emptyList();
            vulnerability.setAuthor(authors);
            if (!validPackageIds.contains(vulnerability.getPackageId())) {
                vulnerability.setStatus(DefectStatus.NEW.value() | DefectStatus.FIXED.value());
            }
        }

        // 根据PakcageName分组
        Map<String, List<SCAVulnerabilityEntity>> vulDefectMap = vulnerabilities.stream()
                .filter(it -> StringUtils.isNotBlank(it.getPackageName()))
                .collect(Collectors.groupingBy(SCAVulnerabilityEntity::getPackageName));
        // 分批处理
        List<SCAVulnerabilityEntity> batchVulnerabilities = new ArrayList<>();
        Set<String> packageNames = new HashSet<>();
        for (Entry<String, List<SCAVulnerabilityEntity>> packageNameVulDefectEntry : vulDefectMap.entrySet()) {
            batchVulnerabilities.addAll(packageNameVulDefectEntry.getValue());
            packageNames.add(packageNameVulDefectEntry.getKey());
            if (batchVulnerabilities.size() > ComConstants.COMMON_NUM_10000) {
                processPackageVulnerability(taskId, toolName, buildEntity, packageNames, batchVulnerabilities);
                batchVulnerabilities.clear();
                packageNames.clear();
            }
        }
        // 处理最后一批
        if (CollectionUtils.isNotEmpty(batchVulnerabilities)) {
            processPackageVulnerability(taskId, toolName, buildEntity, packageNames, batchVulnerabilities);
        }
        List<SCAVulnerabilityEntity> needUpdateVulDefects = new ArrayList<>();
        List<SCAVulnerabilityEntity> needClosedDefects =
                scaVulnerabilityService.getVulnerabilityByPackageNamesNotContain(taskId, toolName,
                        vulDefectMap.keySet());
        if (CollectionUtils.isNotEmpty(needClosedDefects)) {
            needClosedDefects.forEach(it -> {
                if (it.getStatus() == DefectStatus.NEW.value()) {
                    closedSCAVulnerability(it, curTime, buildEntity);
                    needUpdateVulDefects.add(it);
                }
            });
        }
        scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needUpdateVulDefects);
        log.info("save vulnerability success, taskId={}, toolName={}, buildId={}, closed size={}",
                taskId, toolName, buildId, needUpdateVulDefects.size());
    }

    private void closedAllNewVulDefect(Long taskId, String toolName, BuildEntity buildEntity) {
        List<SCAVulnerabilityEntity> needClosedDefects = scaVulnerabilityService.getNewVulDefect(taskId, toolName);
        List<SCAVulnerabilityEntity> needUpdateVulDefects = new ArrayList<>();
        long curTime = System.currentTimeMillis();
        if (CollectionUtils.isNotEmpty(needClosedDefects)) {
            needClosedDefects.forEach(it -> {
                closedSCAVulnerability(it, curTime, buildEntity);
                needUpdateVulDefects.add(it);
            });
        }
        scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needUpdateVulDefects);
    }

    private SCAVulnerabilityEntity createVulEntityFromVulInfo(Long taskId,
                                                              String toolName,
                                                              SCAVulnerabilityInfo vulnerabilityInfo,
                                                              SCAVulHitPackage hitPackage,
                                                              BuildEntity buildEntity,
                                                              Long curTime
    ) {
        SCAVulnerabilityEntity defect = new SCAVulnerabilityEntity();
        defect.setTaskId(taskId);
        defect.setToolName(toolName);
        defect.setName(vulnerabilityInfo.getName());
        defect.setPackageName(hitPackage.getPackageName());
        defect.setPackageVersion(hitPackage.getVersion());
        defect.setPackageId(getPackageIdByHitPackage(hitPackage));
        defect.setVulnerabilityIds(vulnerabilityInfo.getVulnerabilityIds());
        int severity = ComConstants.UNKNOWN;
        if (vulnerabilityInfo.getSeverity() == SpdxSbomPackage.SPDX_RISK_LOW) {
            severity = ComConstants.PROMPT;
        } else if (vulnerabilityInfo.getSeverity() == SpdxSbomPackage.SPDX_RISK_MEDIUM) {
            severity = ComConstants.NORMAL;
        } else if (vulnerabilityInfo.getSeverity() == SpdxSbomPackage.SPDX_RISK_HIGH) {
            severity = ComConstants.SERIOUS;
        }
        defect.setSeverity(severity);
        defect.setStatus(DefectStatus.NEW.value());
        defect.setMessage(vulnerabilityInfo.getDescription());
        defect.setSource(vulnerabilityInfo.getSource());
        defect.setAffectedPackages(vulnerabilityInfo.getAffectedPackages());
        defect.setRatings(vulnerabilityInfo.getRatings());
        defect.setCvssV2(Optional.ofNullable(vulnerabilityInfo.getRatings()).orElse(Collections.emptyList()).stream()
                .filter(it -> SCAVulRatingMethod.CVSS_V2.getMethod().equals(it.getMethod()))
                .findFirst().orElse(null));
        defect.setCvssV3(Optional.ofNullable(vulnerabilityInfo.getRatings()).orElse(Collections.emptyList()).stream()
                .filter(it -> SCAVulRatingMethod.Companion.getCVSS_V3_LIST().contains(it.getMethod()))
                .findFirst().orElse(null));
        defect.setModifiedDate(vulnerabilityInfo.getModifiedDate());
        defect.setPublishedDate(vulnerabilityInfo.getPublishedDate());
        defect.setCreateTime(curTime);
        if (buildEntity != null) {
            defect.setCreateBuildNumber(buildEntity.getBuildNo());
        }
        defect.applyAuditInfoOnCreate();
        return defect;
    }


    /**
     * 批量处理漏洞
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param packageNames
     * @param defects
     */
    private void processPackageVulnerability(Long taskId, String toolName, BuildEntity buildEntity,
                                             Set<String> packageNames, List<SCAVulnerabilityEntity> defects) {
        // 通过package找到告警
        List<SCAVulnerabilityEntity> oldVulList = scaVulnerabilityService.getVulnerabilityByPackageNames(
                taskId, toolName, packageNames);

        long curTime = System.currentTimeMillis();
        List<SCAVulnerabilityEntity> needUpdateVulList = new LinkedList<>();
        // 告警为空时处理老告警
        if (CollectionUtils.isEmpty(defects)) {
            if (CollectionUtils.isNotEmpty(oldVulList)) {
                for (SCAVulnerabilityEntity oldVul : oldVulList) {
                    if (oldVul.getStatus() != DefectStatus.NEW.value()) {
                        continue;
                    }
                    closedSCAVulnerability(oldVul, curTime, buildEntity);
                    needUpdateVulList.add(oldVul);
                }
                scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needUpdateVulList);
                log.info("batch update vul size: {} {} {} {}", taskId, toolName, buildEntity.getBuildId(),
                        needUpdateVulList.size());
            }
            return;
        }

        // 先根据PackageId分组
        Map<String, List<SCAVulnerabilityEntity>> newVulByPackageId = defects.stream()
                .filter(it -> StringUtils.isNotBlank(it.getPackageId()))
                .collect(Collectors.groupingBy(SCAVulnerabilityEntity::getPackageId));
        Map<String, List<SCAVulnerabilityEntity>> oldVulByPackageId = oldVulList.stream()
                .filter(it -> StringUtils.isNotBlank(it.getPackageId()))
                .collect(Collectors.groupingBy(SCAVulnerabilityEntity::getPackageId));
        Set<String> matchPackageIds = new HashSet<>();
        for (Entry<String, List<SCAVulnerabilityEntity>> packageIdVulDefectEntry : newVulByPackageId.entrySet()) {
            String packageId = packageIdVulDefectEntry.getKey();
            matchPackageIds.add(packageId);
            needUpdateVulList.addAll(getNeedUpdateOldVulDefects(packageIdVulDefectEntry.getValue(),
                    oldVulByPackageId.get(packageId), curTime, buildEntity));
        }
        // 处理没有匹配上的老告警
        for (Entry<String, List<SCAVulnerabilityEntity>> packageIdVulDefectEntry : oldVulByPackageId.entrySet()) {
            String packageId = packageIdVulDefectEntry.getKey();
            if (matchPackageIds.contains(packageId)) {
                continue;
            }
            for (SCAVulnerabilityEntity oldVul : packageIdVulDefectEntry.getValue()) {
                if (oldVul.getStatus() != DefectStatus.NEW.value()) {
                    continue;
                }
                closedSCAVulnerability(oldVul, curTime, buildEntity);
                needUpdateVulList.add(oldVul);
            }
        }
        scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needUpdateVulList);
        log.info("batch update vul size: {} {} {} {}", taskId, toolName, buildEntity.getBuildId(),
                needUpdateVulList.size());
    }

    private List<SCAVulnerabilityEntity> getNeedUpdateOldVulDefects(List<SCAVulnerabilityEntity> defects,
                                                                    List<SCAVulnerabilityEntity> oldDefects,
                                                                    Long curTime, BuildEntity buildEntity) {
        List<SCAVulnerabilityEntity> needUpdateVulList = new LinkedList<>();
        if (CollectionUtils.isEmpty(defects) && CollectionUtils.isEmpty(oldDefects)) {
            return Collections.emptyList();
        } else if (CollectionUtils.isEmpty(defects) && CollectionUtils.isNotEmpty(oldDefects)) {
            for (SCAVulnerabilityEntity oldDefect : oldDefects) {
                if (oldDefect.getStatus() != DefectStatus.NEW.value()) {
                    continue;
                }
                closedSCAVulnerability(oldDefect, curTime, buildEntity);
                needUpdateVulList.add(oldDefect);
            }
            return needUpdateVulList;
        } else if (CollectionUtils.isNotEmpty(defects) && CollectionUtils.isEmpty(oldDefects)) {
            return defects;
        }
        Map<String, Set<String>> vulIdToOldDefectIdsMap = oldDefects.stream()
                .flatMap(defect -> Optional.ofNullable(defect.getVulnerabilityIds()).orElse(Collections.emptyList())
                        .stream().map(id -> new AbstractMap.SimpleEntry<>(id, defect.getEntityId())))
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        Collectors.mapping(AbstractMap.SimpleEntry::getValue, Collectors.toSet())
                ));
        Map<String, SCAVulnerabilityEntity> idToOldVulDefect = oldDefects.stream()
                .collect(Collectors.toMap(SCAVulnerabilityEntity::getEntityId, Function.identity(),
                        (existing, replacement) -> existing));
        Set<String> matchOldIds = new HashSet<>();
        for (SCAVulnerabilityEntity vulDefect : defects) {
            if (CollectionUtils.isEmpty(vulDefect.getVulnerabilityIds())
                    || vulDefect.getVulnerabilityIds().stream().noneMatch(vulIdToOldDefectIdsMap::containsKey)) {
                needUpdateVulList.add(vulDefect);
                continue;
            }
            List<String> oldIds = vulDefect.getVulnerabilityIds().stream().map(vulIdToOldDefectIdsMap::get)
                    .filter(CollectionUtils::isNotEmpty).flatMap(Collection::stream).collect(Collectors.toList());
            SCAVulnerabilityEntity oldDefect = idToOldVulDefect.entrySet().stream()
                    .filter(it -> oldIds.contains(it.getKey()) && !matchOldIds.contains(it.getKey()))
                    .map(Entry::getValue).findFirst().orElse(null);
            if (oldDefect == null) {
                needUpdateVulList.add(vulDefect);
                continue;
            }
            matchOldIds.add(oldDefect.getEntityId());
            updateOldVulnerabilityInfo(vulDefect, oldDefect);
            if (vulDefect.getStatus() == DefectStatus.NEW.value()
                    && (oldDefect.getStatus() & DefectStatus.FIXED.value()) > 0) {
                openSCAVulnerability(oldDefect);
            } else if (oldDefect.getStatus() == DefectStatus.NEW.value()
                    && (vulDefect.getStatus() & DefectStatus.FIXED.value()) > 0) {
                closedSCAVulnerability(oldDefect, curTime, buildEntity);
            }
            needUpdateVulList.add(oldDefect);
        }
        // 处理没有匹配上的老告警
        for (SCAVulnerabilityEntity oldDefect : oldDefects) {
            if (!matchOldIds.contains(oldDefect.getEntityId())) {
                closedSCAVulnerability(oldDefect, curTime, buildEntity);
                needUpdateVulList.add(oldDefect);
            }
        }
        return needUpdateVulList;
    }


    private String getPackageIdByHitPackage(SCAVulHitPackage hitPackage) {
        if (hitPackage == null) {
            return null;
        }
        String versionStr = hitPackage.getVersion();
        if (StringUtils.isBlank(versionStr)) {
            return hitPackage.getPackageName();
        }
        return hitPackage.getPackageName() + ":" + versionStr;
    }

    private void updateOldVulnerabilityInfo(SCAVulnerabilityEntity newVul, SCAVulnerabilityEntity oldVul) {
        if (newVul == null || oldVul == null) {
            return;
        }
        String[] ignoreFields = new String[]{"entityId", "status", "author", "createdDate", "createdBy", "ignoreTime",
                "ignoreReasonType", "ignoreReason", "ignoreAuthor", "fixedBuildNumber", "fixedTime"};
        BeanUtils.copyProperties(newVul, oldVul, ignoreFields);
    }

    private void openSCAVulnerability(SCAVulnerabilityEntity scaVul) {
        // 修复重新打开
        scaVul.setStatus(DefectStatus.NEW.value());
        scaVul.setFixedTime(null);
        scaVul.setFixedBuildNumber(null);
    }

    private void closedSCAVulnerability(SCAVulnerabilityEntity scaVul, Long curTime, BuildEntity buildEntity) {
        scaVul.setStatus(DefectStatus.NEW.value() | DefectStatus.FIXED.value());
        scaVul.setFixedTime(curTime);
        scaVul.setFixedBuildNumber(buildEntity == null ? null : buildEntity.getBuildNo());
    }

    private void batchUploadLicense(Long taskId, String toolName, String buildId, BuildEntity buildEntity,
                                    List<SCALicenseEntity> licenseEntities) {
        log.info("batch upload license start: {} {} {}", taskId, toolName, buildId);

        List<SCALicenseEntity> needUpdateLicenses = new LinkedList<>();
        List<SCALicenseEntity> oldLicenseList = scaLicenseService.getLicenseByTaskIdAndToolName(taskId, toolName);

        // 获取证书详情，这里存在别名，所以需要证书详情辅助识别
        List<String> newLicenses = licenseEntities.stream().map(SCALicenseEntity::getName)
                .collect(Collectors.toList());
        Map<String, LicenseDetailEntity> licenseDetailEntityMap = getLicenseDetailMapping(newLicenses);
        List<String> filterLicenses = filterLicense(newLicenses, licenseDetailEntityMap);

        long curTime = System.currentTimeMillis();
        // 处理更新证书逻辑
        if (CollectionUtils.isEmpty(filterLicenses) && CollectionUtils.isNotEmpty(oldLicenseList)) {
            // 没有新证书，全部关闭
            List<SCALicenseEntity> needClosedLicense = oldLicenseList.stream()
                    .filter(it -> it.getStatus() == DefectStatus.NEW.value()).collect(Collectors.toList());
            needClosedLicense.forEach(it -> {
                closedSCALicense(it, curTime, buildEntity);
            });
            needUpdateLicenses.addAll(needClosedLicense);
        } else if (CollectionUtils.isNotEmpty(filterLicenses)) {
            // 建立OldLicense
            Map<String, SCALicenseEntity> oldLicenseMap = CollectionUtils.isEmpty(oldLicenseList)
                    ? Collections.emptyMap() : oldLicenseList.stream().collect(
                    Collectors.toMap(SCALicenseEntity::getName, Function.identity()));
            for (String license : filterLicenses) {
                SCALicenseEntity newLicense = new SCALicenseEntity(taskId, toolName, license,
                        newLicenses.contains(license) ? DefectStatus.NEW.value()
                                : (DefectStatus.NEW.value() | DefectStatus.FIXED.value()),
                        newLicenses.contains(license));
                fillSCALicenseInfoByDetail(newLicense, licenseDetailEntityMap.get(license));
                if (!oldLicenseMap.containsKey(license)) {
                    needUpdateLicenses.add(newLicense);
                    continue;
                }
                SCALicenseEntity oldLicense = oldLicenseMap.get(license);
                updateOldLicenseInfo(newLicense, oldLicense);
                // 更新状态
                if ((oldLicense.getStatus() & DefectStatus.FIXED.value()) > 0
                        && newLicense.getStatus() == DefectStatus.NEW.value()) {
                    openSCALicense(oldLicense);
                } else if ((newLicense.getStatus() & DefectStatus.FIXED.value()) > 0
                        && oldLicense.getStatus() == DefectStatus.NEW.value()) {
                    closedSCALicense(oldLicense, curTime, buildEntity);
                }
                needUpdateLicenses.add(oldLicense);
            }
            if (CollectionUtils.isNotEmpty(oldLicenseList)) {
                List<SCALicenseEntity> needClosedLicense = oldLicenseList.stream()
                        .filter(it -> it.getStatus() == DefectStatus.NEW.value()
                                && !filterLicenses.contains(it.getName())).collect(Collectors.toList());
                needClosedLicense.forEach(it -> closedSCALicense(it, curTime, buildEntity));
                needUpdateLicenses.addAll(needClosedLicense);
            }
        }
        if (CollectionUtils.isNotEmpty(needUpdateLicenses)) {
            scaLicenseService.saveLicenses(taskId, toolName, needUpdateLicenses);
        }
        log.info("batch upload license end: {} {} {}", taskId, toolName, buildId);
    }

    /**
     * 对组件的证书进行过滤处理
     * 1. 去除重复的证书名
     * 2. 过滤别名
     *
     * @param licenseNames
     * @param nameToDetails
     * @return
     */
    private List<String> filterLicense(List<String> licenseNames, Map<String, LicenseDetailEntity> nameToDetails) {
        // 对原始列表去重
        Set<String> repeat = new HashSet<>();
        Set<String> filterOriLicenses = new HashSet<>();
        for (String licenseName : licenseNames) {
            if (StringUtils.isBlank(licenseName)) {
                continue;
            }
            if (!nameToDetails.containsKey(licenseName)) {
                filterOriLicenses.add(licenseName);
                continue;
            }
            // 别名过滤
            if (repeat.contains(nameToDetails.get(licenseName).getEntityId())) {
                continue;
            }
            filterOriLicenses.add(nameToDetails.get(licenseName).getName());
            repeat.add(nameToDetails.get(licenseName).getEntityId());
        }
        return new LinkedList<>(filterOriLicenses);
    }

    /**
     * 填充证书信息
     *
     * @param scaLicense
     * @param licenseDetail
     * @return
     */
    private void fillSCALicenseInfoByDetail(SCALicenseEntity scaLicense, LicenseDetailEntity licenseDetail) {
        if (scaLicense == null) {
            return;
        }
        if (licenseDetail == null) {
            scaLicense.setFullName(scaLicense.getName());
            scaLicense.setSeverity(ComConstants.UNKNOWN);
            scaLicense.setGplCompatible(false);
            scaLicense.setOsi(false);
            scaLicense.setFsf(false);
            scaLicense.setSpdx(false);
        } else {
            scaLicense.setFullName(licenseDetail.getFullName());
            scaLicense.setSeverity(licenseDetail.getSeverity());
            scaLicense.setGplCompatible(licenseDetail.getGplCompatible());
            scaLicense.setOsi(licenseDetail.getOsi());
            scaLicense.setFsf(licenseDetail.getFsf());
            scaLicense.setSpdx(licenseDetail.getSpdx());
        }
    }

    private void updateOldLicenseInfo(SCALicenseEntity newLicense, SCALicenseEntity oldLicense) {
        if (newLicense == null || oldLicense == null) {
            return;
        }
        oldLicense.setFullName(newLicense.getFullName());
        oldLicense.setSeverity(newLicense.getSeverity());
        oldLicense.setGplCompatible(newLicense.getGplCompatible());
        oldLicense.setOsi(newLicense.getOsi());
        oldLicense.setFsf(newLicense.getFsf());
        oldLicense.setSpdx(newLicense.getSpdx());
        oldLicense.setHasEnabledPackage(newLicense.getHasEnabledPackage());
    }

    private void openSCALicense(SCALicenseEntity scaLicense) {
        // 修复重新打开
        scaLicense.setStatus(DefectStatus.NEW.value());
        scaLicense.setFixedTime(null);
        scaLicense.setFixedBuildNumber(null);
    }

    private void closedSCALicense(SCALicenseEntity scaLicense, Long curTime, BuildEntity buildEntity) {
        scaLicense.setStatus(DefectStatus.NEW.value() | DefectStatus.FIXED.value());
        scaLicense.setHasEnabledPackage(false);
        scaLicense.setFixedTime(curTime);
        scaLicense.setFixedBuildNumber(buildEntity == null ? null : buildEntity.getBuildNo());
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.SCA.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.SCA.name().toLowerCase(Locale.ENGLISH);
    }
}
