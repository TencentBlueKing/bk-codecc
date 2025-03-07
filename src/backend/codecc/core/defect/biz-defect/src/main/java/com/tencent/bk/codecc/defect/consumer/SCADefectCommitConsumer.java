/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.consumer;

import com.alibaba.fastjson.JSONReader;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.constant.SbomRelationshipType;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.LicenseDetailRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAPackageFileInfo;
import com.tencent.bk.codecc.defect.model.sca.SCAPackageVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity;
import com.tencent.bk.codecc.defect.model.sca.SCAScanContext;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomPackageBase;
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomStartEndPointer;
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomInfo;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.sca.SCALicenseService;
import com.tencent.bk.codecc.defect.service.sca.SCASbomService;
import com.tencent.bk.codecc.defect.service.sca.SCAVulnerabilityService;
import com.tencent.bk.codecc.defect.service.sca.SpdxSbomReader;
import com.tencent.bk.codecc.defect.service.statistic.SCADefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.Status;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.guava.Sets;
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
        // 读取SBOM文件信息
        SpdxSbomInfo spdxSbomInfo = SpdxSbomReader.INSTANCE.readFromJsonFile(fileIndex);
        if (spdxSbomInfo == null) {
            log.warn("Can not read raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format(
                    "读取的告警文件失败: %s, %s, %s", streamName, toolName, buildId), null);
        }
        RedisLock locker = null;
        SCASbomAggregateModel newAggregateModel;
        List<SCAVulnerabilityEntity> vulnerabilities;
        List<SCALicenseEntity> licenseEntities;
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
            SCAScanContext scaScanContext = new SCAScanContext(isFullScan, spdxSbomInfo.getIncrementalFiles(),
                    toolBuildStackEntity == null ? Collections.emptySet() : toolBuildStackEntity.getRootPaths(),
                    toolBuildStackEntity == null ? Collections.emptyList() : toolBuildStackEntity.getDeleteFiles(),
                    BooleanUtils.isTrue(commitDefectVO.isReallocate()));

            // 记录files与snippets的代码版本信息
            SCASbomAggregateModel aggregateModel = spdxSbomInfo.getSCASbomAggregateModel(taskId, toolName);
            fillPackagesAndFilesAndSnippetsInfo(aggregateModel, fileChangeRecordsMap, codeRepoIdMap);
            newAggregateModel = uploadSCADefect(taskId, streamName, toolName, buildId, buildEntity, aggregateModel,
                    scaScanContext);
            // 获取更新后的证书与漏洞
            vulnerabilities = scaVulnerabilityService.getNewAndHasEnabledPackageVul(taskId, toolName);
            licenseEntities = scaLicenseService.getNewLicenseByTaskIdAndToolName(taskId, toolName);
        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }

            log.info("defect commit, lock try to finally cost total: {}, {}, {}, {}",
                    System.currentTimeMillis() - tryBeginTime, taskId, toolName, buildId);
        }
        // 快照
        buildSnapshotService.saveSCASnapshot(taskId, toolName, buildId, buildEntity, newAggregateModel, vulnerabilities,
                licenseEntities);
        // 统计
        scaDefectStatisticServiceImpl.statistic(new DefectStatisticModel<>(
                taskVO,
                toolName,
                buildId,
                toolBuildStackEntity,
                vulnerabilities,
                false,
                newAggregateModel,
                licenseEntities
        ));

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
        return true;
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
     * @param taskId 任务唯一标识
     * @param streamName 代码流名称
     * @param toolName 扫描工具名称
     * @param buildId 当前构建ID
     * @param buildEntity 构建实体对象
     * @param aggregateModel SBOM聚合数据模型
     * @param scaScanContext 扫描上下文
     * @return 更新后的SBOM聚合模型，包含最新的组件、文件、代码片段及其关系信息
     * @throws Exception 处理过程中可能抛出的异常
     */
    private SCASbomAggregateModel uploadSCADefect(long taskId, String streamName, String toolName, String buildId,
            BuildEntity buildEntity, SCASbomAggregateModel aggregateModel, SCAScanContext scaScanContext) {
        log.info("[SCA] Start {} upload SCA defect, taskId={}, streamName={}, toolName={}, buildId={}",
                scaScanContext.getFullScan() ? "full" : "incremental", taskId, streamName, toolName, buildId);

        try {

            // 获取并更新SBOM基础信息
            log.debug("[SCA] Start upload sbom info");
            uploadSbomInfo(taskId, toolName, buildId, aggregateModel.getInfo());
            // 获取并更新SBOM代码片段信息
            log.debug("[SCA] Start upload sbom snippets");
            uploadSbomSnippets(taskId, toolName, buildId, aggregateModel.getSnippets(), scaScanContext);
            // 获取并更新SBOM文件信息
            log.debug("[SCA] Start upload sbom files");
            uploadSbomFiles(taskId, toolName, buildId, aggregateModel.getFiles(), scaScanContext);
            // 获取并更新SBOM组件信息
            log.debug("[SCA] Start upload sbom packages");
            uploadSbomPackages(taskId, toolName, buildEntity, aggregateModel.getPackages(), scaScanContext);
            // 获取并更新SBOM关系信息
            log.debug("[SCA] Start upload sbom relationships");
            uploadSbomRelationships(taskId, toolName, buildId, aggregateModel, scaScanContext.getFullScan());
            // 获取保存更新后，新的SBOM聚合信息
            log.debug("[SCA] Start get new sca sbom aggregate model");
            SCASbomAggregateModel newAggregateModel = scaSbomService.getNewSCASbomAggregateModel(taskId, toolName);
            // 更新漏洞
            log.debug("[SCA] Start batch upload vulnerability");
            batchUploadVulnerability(taskId, toolName, streamName, buildId, buildEntity,
                    aggregateModel.getPackages(), newAggregateModel.getPackages());
            // 更新证书
            log.debug("[SCA] Start batch upload license");
            batchUploadLicense(taskId, toolName, buildId, buildEntity, aggregateModel.getPackages(),
                    newAggregateModel.getPackages());
            log.info("[SCA] Finish full upload SCA defect successfully, taskId={}", taskId);
            return newAggregateModel;
        } catch (Exception e) {
            log.error("[SCA] Failed to full upload SCA defect, taskId={}, error={}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 填充组件、文件和代码片段的关系信息
     * 1. 填充文件和代码片段的基础信息
     * 2. 通过依赖关系建立组件与文件/代码片段的关联
     * 3. 记录代码片段与文件关联的信息
     *
     * @param aggregateModel SBOM聚合模型
     * @param fileChangeRecordsMap 文件变更记录映射
     * @param codeRepoIdMap 代码仓库ID映射
     */
    private void fillPackagesAndFilesAndSnippetsInfo(SCASbomAggregateModel aggregateModel,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        log.debug("[SCA] Start filling package-file-snippet relationships");

        List<SCASbomFileEntity> files = aggregateModel.getFiles();
        List<SCASbomSnippetEntity> snippets = aggregateModel.getSnippets();
        // 步骤1: 填充文件和代码片段的基础信息（文件名、代码版本、作者等）
        fillFileAndSnippetInfo(files, snippets, fileChangeRecordsMap, codeRepoIdMap);

        // 步骤2: 处理组件依赖关系
        List<SCASbomPackageEntity> packages = aggregateModel.getPackages();
        List<SCASbomRelationshipEntity> relationships = aggregateModel.getRelationships();

        // 构建依赖关系映射：packageId -> [DEPENDENCY_MANIFEST_OF 关系]
        Map<String, List<SCASbomRelationshipEntity>> dependencyManifestOf = relationships.stream()
                .filter(it -> StringUtils.isNotEmpty(it.getElementId())
                        && StringUtils.isNotEmpty(it.getRelatedElement())
                        && SbomRelationshipType.DEPENDENCY_MANIFEST_OF.name().equals(it.getRelationshipType()))
                .collect(Collectors.groupingBy(SCASbomRelationshipEntity::getRelatedElement));

        // 构建快速查找映射
        Map<String, SCASbomSnippetEntity> idToSnippets = snippets.stream().collect(
                Collectors.toMap(SCASbomSnippetEntity::getElementId, Function.identity(), (v1, v2) -> v1));
        Map<String, SCASbomFileEntity> idToFiles = files.stream().collect(
                Collectors.toMap(SCASbomFileEntity::getElementId, Function.identity(), (v1, v2) -> v1));

        log.debug("[SCA] Processing {} packages with dependency relationships", packages.size());

        // 步骤3: 为每个组件查找依赖来源
        for (SCASbomPackageEntity scaSbomPackage : packages) {
            String packageId = scaSbomPackage.getElementId();
            List<SCASbomRelationshipEntity> packageDependFromRelationships = dependencyManifestOf.get(packageId);

            if (CollectionUtils.isEmpty(packageDependFromRelationships)) {
                log.debug("[SCA] Package {} has no dependency manifest relationships", packageId);
                continue;
            }

            log.debug("[SCA] Processing package {} with {} dependency relationships",
                    packageId, packageDependFromRelationships.size());

            // 记录所有匹配上的文件与代码片段位置
            List<SCAPackageFileInfo> packageFileInfos = new ArrayList<>();
            for (SCASbomRelationshipEntity relationship : packageDependFromRelationships) {
                SCAPackageFileInfo packageFileInfo = new SCAPackageFileInfo();
                if (idToSnippets.containsKey(relationship.getElementId())) {
                    // 处理代码片段依赖
                    SCASbomSnippetEntity snippet = idToSnippets.get(relationship.getElementId());
                    linkPackageToSnippet(packageFileInfo, snippet, idToFiles);
                } else if (idToFiles.containsKey(relationship.getElementId())) {
                    // 处理文件依赖
                    SCASbomFileEntity file = idToFiles.get(relationship.getElementId());
                    linkPackageToFile(packageFileInfo, file);
                }
                if (StringUtils.isNotEmpty(packageFileInfo.getFileElementId())) {
                    packageFileInfos.add(packageFileInfo);
                }
            }

            if (CollectionUtils.isEmpty(packageFileInfos)) {
                return;
            }
            // 根据getFileElementId分组
            fillPackageFileInfo(scaSbomPackage, packageFileInfos, idToFiles);
        }
        log.debug("[SCA] Finished filling package-file-snippet relationships");
    }

    /**
     * 填充软件包文件信息
     *
     * 主要功能:
     * 1. 合并相同fileElementId的包文件信息
     * 2. 根据最后更新时间最大的文件信息设置包的仓库相关信息
     *
     * @param scaSbomPackage 需要填充信息的SBOM包实体
     * @param packageFileInfos 包文件信息列表
     * @param idToFiles 文件ID到SBOM文件实体的映射
     */
    private void fillPackageFileInfo(SCASbomPackageEntity scaSbomPackage, List<SCAPackageFileInfo> packageFileInfos,
            Map<String, SCASbomFileEntity> idToFiles) {
        if (CollectionUtils.isEmpty(packageFileInfos)) {
            return;
        }
        // 按fileElementId对文件信息进行分组
        Map<String, List<SCAPackageFileInfo>> groupedByFileElementId = packageFileInfos.stream()
                .filter(it -> StringUtils.isNotEmpty(it.getFileElementId()))
                .collect(Collectors.groupingBy(SCAPackageFileInfo::getFileElementId));
        // 合并相同fileElementId的文件信息
        List<SCAPackageFileInfo> mergedPackageFileInfos = groupedByFileElementId.values().stream()
                .map(group -> {
                    SCAPackageFileInfo firstFileInfo = group.get(0);
                    // 合并所有snippet元素ID
                    List<String> snippetElementIds = group.stream()
                            .filter(it -> it != null && CollectionUtils.isNotEmpty(it.getSnippetElementIds()))
                            .flatMap(info -> info.getSnippetElementIds().stream())
                            .distinct()
                            .collect(Collectors.toList());
                    // 合并所有作者信息
                    List<String> authors = getAuthorFromPackageFileInfos(group);
                    // 获取最大的最后更新时间
                    long lastUpdateTime = group.stream()
                            .mapToLong(SCAPackageFileInfo::getLastUpdateTime)
                            .max()
                            .orElse(0L);

                    // 创建合并后的文件信息对象
                    SCAPackageFileInfo mergedFileInfo = new SCAPackageFileInfo();
                    mergedFileInfo.setFileName(firstFileInfo.getFileName());
                    mergedFileInfo.setFilePath(firstFileInfo.getFilePath());
                    mergedFileInfo.setRelPath(firstFileInfo.getRelPath());
                    mergedFileInfo.setFileElementId(firstFileInfo.getFileElementId());
                    mergedFileInfo.setSnippetElementIds(snippetElementIds);
                    mergedFileInfo.setAuthors(authors);
                    mergedFileInfo.setLastUpdateTime(lastUpdateTime);

                    return mergedFileInfo;
                })
                .collect(Collectors.toList());

        scaSbomPackage.setFileInfos(mergedPackageFileInfos);
        scaSbomPackage.setAuthor(getAuthorFromPackageFileInfos(mergedPackageFileInfos));

        // 获取最后更新时间最大的文件信息
        Optional<SCAPackageFileInfo> maxLastUpdateTimeFileInfo = mergedPackageFileInfos.stream()
                .max(Comparator.comparingLong(SCAPackageFileInfo::getLastUpdateTime));
        // 根据最新文件的信息设置包的仓库相关信息
        if (maxLastUpdateTimeFileInfo.isPresent()) {
            String fileElementId = maxLastUpdateTimeFileInfo.get().getFileElementId();
            scaSbomPackage.setLastUpdateTime(maxLastUpdateTimeFileInfo.get().getLastUpdateTime());
            SCASbomFileEntity sbomFileEntity = idToFiles.get(fileElementId);
            if (sbomFileEntity != null) {
                scaSbomPackage.setRepoId(sbomFileEntity.getRepoId());
                scaSbomPackage.setRevision(sbomFileEntity.getRevision());
                scaSbomPackage.setBranch(sbomFileEntity.getBranch());
                scaSbomPackage.setUrl(sbomFileEntity.getUrl());
                scaSbomPackage.setSubModule(sbomFileEntity.getSubModule());
            }
        }
    }

    /**
     * 将组件关联到代码片段及其所属文件
     */
    private void linkPackageToSnippet(SCAPackageFileInfo packageFileInfo,
            SCASbomSnippetEntity snippet,
            Map<String, SCASbomFileEntity> idToFiles) {
        if (StringUtils.isNotEmpty(snippet.getSnippetFromFile())) {
            SCASbomFileEntity sbomFileEntity = idToFiles.get(snippet.getSnippetFromFile());
            // 仅记录有路径的文件信息
            if (sbomFileEntity != null && StringUtils.isNotEmpty(sbomFileEntity.getFilePath())) {
                packageFileInfo.setSnippetElementIds(Collections.singletonList(snippet.getElementId()));
                packageFileInfo.setAuthors(Collections.singletonList(snippet.getLastUpdateAuthor()));
                packageFileInfo.setLastUpdateTime(snippet.getLastUpdateTime());
                packageFileInfo.setFileElementId(sbomFileEntity.getElementId());
                fillPackageFileInfoByFile(packageFileInfo, sbomFileEntity);
            } else {
                log.warn("[SCA] Snippet {} references missing file {}",
                        snippet.getElementId(), snippet.getSnippetFromFile());
            }
        }
    }

    /**
     * 将组件直接关联到文件
     */
    private void linkPackageToFile(SCAPackageFileInfo packageFileInfo,
            SCASbomFileEntity file) {
        if (file != null && StringUtils.isNotEmpty(file.getFilePath())) {
            // 仅记录有路径的文件信息
            packageFileInfo.setFileElementId(file.getElementId());
            packageFileInfo.setAuthors(Collections.singletonList(file.getLastUpdateAuthor()));
            packageFileInfo.setLastUpdateTime(file.getLastUpdateTime());
            fillPackageFileInfoByFile(packageFileInfo, file);
        }
    }

    /**
     * 根据SCM_BLAME填充文件代码版本、作者信息
     * 因为 SBOM 文件信息中的文件都是相对路径，所以只能用相对路径匹配
     *
     * @param files
     * @param snippets
     * @param fileChangeRecordsMap
     * @param codeRepoIdMap
     */
    private void fillFileAndSnippetInfo(List<SCASbomFileEntity> files, List<SCASbomSnippetEntity> snippets,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap) {
        // 先将 fileChangeRecordsMap 转为 relPath 的模式，SBOM 文件信息中的文件都是相对路径，所以只能用相对路径匹配
        Map<String, ScmBlameVO> fileChangeRecordsToRelPathMap = fileChangeRecordsMap.values().stream().collect(
                Collectors.toMap(ScmBlameVO::getFileRelPath, Function.identity(), (v1, v2) -> v1));
        // 将代码片段按照文件ElementId分组
        Map<String, List<SCASbomSnippetEntity>> fileIdToSnippets = snippets.stream()
                .filter(it -> StringUtils.isNotBlank(it.getSnippetFromFile()))
                .collect(Collectors.groupingBy(SCASbomSnippetEntity::getSnippetFromFile));
        // 从文件出发，先找到文件的ScmBlameVO，
        for (SCASbomFileEntity file : files) {
            String relPath = file.getFileRelPath();
            // 没有relPath, 跳过
            if (StringUtils.isEmpty(relPath)) {
                file.setFileName(ComConstants.EMPTY_STRING);
                fillSnippetInfoByFile(file, fileIdToSnippets.get(file.getElementId()), null);
                continue;
            }
            if (StringUtils.isNotEmpty(relPath) && relPath.startsWith("./")) {
                // 将./ 转为 /
                relPath = relPath.substring(1);
            }
            int fileNameIndex = relPath.lastIndexOf("/");
            if (fileNameIndex == -1) {
                fileNameIndex = relPath.lastIndexOf("\\");
            }
            if (relPath.endsWith("/") || relPath.endsWith("\\")) {
                file.setFileName(ComConstants.EMPTY_STRING);
            } else {
                file.setFileName(relPath.substring(fileNameIndex + 1));
            }
            ScmBlameVO scmBlameVO = fileChangeRecordsToRelPathMap.get(relPath);
            // 没有找到代码版本信息，跳过
            if (scmBlameVO == null) {
                fillSnippetInfoByFile(file, fileIdToSnippets.get(file.getElementId()), null);
                continue;
            }
            file.setUrl(scmBlameVO.getUrl());
            file.setBranch(scmBlameVO.getBranch());
            file.setRevision(scmBlameVO.getRevision());
            // 设置RepoId
            if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(scmBlameVO.getScmType())) {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(scmBlameVO.getRootUrl());
                if (null != repoSubModuleVO) {
                    file.setRepoId(repoSubModuleVO.getRepoId());
                }
            } else {
                RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(scmBlameVO.getUrl());
                if (repoSubModuleVO != null) {
                    file.setRepoId(repoSubModuleVO.getRepoId());
                    if (org.apache.commons.lang.StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                        file.setSubModule(repoSubModuleVO.getSubModule());
                    } else {
                        file.setSubModule("");
                    }
                }
            }
            // 设置最新更新时间与作者
            if (CollectionUtils.isNotEmpty(scmBlameVO.getChangeRecords())) {
                ScmBlameChangeRecordVO recordVO = scmBlameVO.getChangeRecords().stream()
                        .max(Comparator.comparingLong(ScmBlameChangeRecordVO::getLineUpdateTime)).orElse(null);
                if (recordVO != null) {
                    file.setLastUpdateAuthor(ToolParamUtils.trimUserName(recordVO.getAuthor()));
                    file.setLastUpdateTime(recordVO.getLineUpdateTime());
                }
            } else if (StringUtils.isNotBlank(scmBlameVO.getFileAuthor())) {
                file.setLastUpdateAuthor(ToolParamUtils.trimUserName(scmBlameVO.getFileAuthor()));
                file.setLastUpdateTime(scmBlameVO.getFileUpdateTime());
            }
            // 设置文件关联的代码片段的信息
            fillSnippetInfoByFile(file, fileIdToSnippets.get(file.getElementId()), scmBlameVO);
        }
    }

    /**
     * 根据文件匹配的ScmBlameVO，设置代码片段的更新与最近更新时间
     *
     * @param file
     * @param snippets
     * @param scmBlameVO
     */
    private void fillSnippetInfoByFile(SCASbomFileEntity file, List<SCASbomSnippetEntity> snippets,
            ScmBlameVO scmBlameVO) {
        if (CollectionUtils.isEmpty(snippets)) {
            return;
        }
        // 设置所属文件的relPath
        for (SCASbomSnippetEntity snippet : snippets) {
            snippet.setFileRelPath(file.getFileRelPath());
            snippet.setFilePath(file.getFilePath());
        }
        // 如果没有SCM信息就跳过
        if (scmBlameVO == null || CollectionUtils.isEmpty(scmBlameVO.getChangeRecords())) {
            return;
        }
        List<ScmBlameChangeRecordVO> changeRecords = scmBlameVO.getChangeRecords();
        // 转换成 行号与ScmBlameChangeRecordVO的Map，方便查找
        Map<Integer, ScmBlameChangeRecordVO> lineAuthorMap = getLineAuthorMap(changeRecords);
        for (SCASbomSnippetEntity snippet : snippets) {
            snippet.setFileRelPath(file.getFileRelPath());
            if (CollectionUtils.isEmpty(snippet.getRanges())) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(changeRecords)) {
                long functionLastUpdateTime = 0;
                String author = null;
                // 从代码片段的所有代码范围中，查找最近更新的
                for (SbomStartEndPointer startEndPointer : snippet.getRanges()) {
                    if (startEndPointer.getStartPointer() == null
                            || startEndPointer.getStartPointer().getLineNumber() == null
                            || startEndPointer.getEndPointer() == null
                            || startEndPointer.getEndPointer().getLineNumber() == null) {
                        continue;
                    }
                    // 获取函数涉及的所有行中的最新修改作者作为告警作者
                    for (int i = startEndPointer.getStartPointer().getLineNumber();
                            i <= startEndPointer.getEndPointer().getLineNumber(); i++) {
                        ScmBlameChangeRecordVO recordVO = lineAuthorMap.get(i);
                        if (recordVO != null && recordVO.getLineUpdateTime() > functionLastUpdateTime) {
                            functionLastUpdateTime = recordVO.getLineUpdateTime();
                            author = ToolParamUtils.trimUserName(recordVO.getAuthor());
                        }
                    }
                }
                // 如果找到了，设置告警作者
                if (StringUtils.isNotEmpty(author)) {
                    snippet.setLastUpdateAuthor(author);
                    snippet.setLastUpdateTime(functionLastUpdateTime);
                }
            }
        }
    }

    private void fillPackageFileInfoByFile(SCAPackageFileInfo packageFileInfo, SCASbomFileEntity sbomFileEntity) {
        if (packageFileInfo == null || sbomFileEntity == null) {
            return;
        }
        packageFileInfo.setFileName(sbomFileEntity.getFileName());
        packageFileInfo.setRelPath(sbomFileEntity.getFileRelPath());
        packageFileInfo.setFilePath(sbomFileEntity.getFilePath());
    }


    /**
     * 保存SBOM基础信息
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param sbomInfo
     */
    private void uploadSbomInfo(long taskId, String toolName, String buildId, SCASbomInfoEntity sbomInfo) {
        // 获取当前数据实体，更新
        scaSbomService.upsertSbomInfoByTaskIdAndToolName(taskId, toolName, sbomInfo);
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
            List<SCASbomPackageEntity> upsertPackages, BuildEntity buildEntity, SCAScanContext scaScanContext,
            Long currentTimeMillis) {
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
        oldPackage.setLicenseConcluded(newPackage.getLicenseConcluded());
        oldPackage.setLicenseDeclared(newPackage.getLicenseDeclared());
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

    /**
     * 保存SBOM文件信息
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param sbomFileEntities
     * @param scaScanContext
     */
    private void uploadSbomFiles(long taskId, String toolName, String buildId,
            List<SCASbomFileEntity> sbomFileEntities, SCAScanContext scaScanContext) {
        Set<String> fileElementIds = CollectionUtils.isEmpty(sbomFileEntities) ? Collections.emptySet()
                : sbomFileEntities.stream().map(SCASbomFileEntity::getElementId).filter(
                        Objects::nonNull).collect(Collectors.toSet());
        // 查询旧的Sbom
        List<SCASbomFileEntity> oldSbomFiles = scaSbomService.getFilesByElementIds(taskId, toolName, fileElementIds);
        Map<String, SCASbomFileEntity> oldElementIdToFile = oldSbomFiles.stream().collect(
                Collectors.toMap(SCASbomFileEntity::getElementId, Function.identity()));
        List<SCASbomFileEntity> needUpdateSbomFiles = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(sbomFileEntities)) {
            for (SCASbomFileEntity sbomFileEntity : sbomFileEntities) {
                SCASbomFileEntity oldSbomFile = sbomFileEntity;
                if (oldElementIdToFile.containsKey(sbomFileEntity.getElementId())) {
                    oldSbomFile = oldElementIdToFile.get(sbomFileEntity.getElementId());
                    updateOldSbomFileInfo(sbomFileEntity, oldSbomFile);
                } else {
                    oldSbomFile.applyAuditInfoOnCreate();
                }
                if (oldSbomFile.getStatus() == Status.DISABLE.value()) {
                    oldSbomFile.setStatus(Status.ENABLE.value());
                    oldSbomFile.setFixedBuildId(null);
                }
                oldSbomFile.applyAuditInfoOnUpdate();
                needUpdateSbomFiles.add(oldSbomFile);
            }
        }
        List<SCASbomFileEntity> enabledFiles = scaSbomService.getEnableFilesByTaskIdAndToolName(taskId, toolName);
        if (CollectionUtils.isNotEmpty(enabledFiles)) {
            enabledFiles.stream().filter(it -> !fileElementIds.contains(it.getElementId())
                            && scaScanContext.isFileScanned(it.getFilePath(), it.getFileRelPath()))
                    .forEach(sbomFile -> {
                        sbomFile.setStatus(Status.DISABLE.value());
                        sbomFile.setFixedBuildId(buildId);
                        sbomFile.applyAuditInfoOnUpdate();
                        needUpdateSbomFiles.add(sbomFile);
                    });
        }
        scaSbomService.saveSbomFiles(taskId, needUpdateSbomFiles);
    }

    /**
     * 保存SBOM代码片段信息
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param sbomSnippetEntities
     * @param scaScanContext
     */
    private void uploadSbomSnippets(long taskId, String toolName, String buildId,
            List<SCASbomSnippetEntity> sbomSnippetEntities, SCAScanContext scaScanContext) {
        Set<String> snippetElementIds = CollectionUtils.isEmpty(sbomSnippetEntities) ? Collections.emptySet()
                : sbomSnippetEntities.stream().map(SCASbomSnippetEntity::getElementId).filter(
                        Objects::nonNull).collect(Collectors.toSet());
        // 查询旧的Sbom
        List<SCASbomSnippetEntity> oldSbomSnippets =
                scaSbomService.getSnippetsByElementIds(taskId, toolName, snippetElementIds);
        Map<String, SCASbomSnippetEntity> oldElementIdToFile = oldSbomSnippets.stream().collect(
                Collectors.toMap(SCASbomSnippetEntity::getElementId, Function.identity()));
        List<SCASbomSnippetEntity> needUpdateSbomSnippets = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(sbomSnippetEntities)) {
            for (SCASbomSnippetEntity sbomSnippetEntity : sbomSnippetEntities) {
                SCASbomSnippetEntity oldSbomSnippet = sbomSnippetEntity;
                if (oldElementIdToFile.containsKey(sbomSnippetEntity.getElementId())) {
                    oldSbomSnippet = oldElementIdToFile.get(sbomSnippetEntity.getElementId());
                    updateOldSbomSnippetInfo(sbomSnippetEntity, oldSbomSnippet);
                } else {
                    oldSbomSnippet.applyAuditInfoOnCreate();
                }
                if (oldSbomSnippet.getStatus() == Status.DISABLE.value()) {
                    oldSbomSnippet.setStatus(Status.ENABLE.value());
                    oldSbomSnippet.setFixedBuildId(null);
                }
                oldSbomSnippet.applyAuditInfoOnUpdate();
                needUpdateSbomSnippets.add(oldSbomSnippet);
            }
        }
        List<SCASbomSnippetEntity> enabledSnippets =
                scaSbomService.getEnableSnippetsByTaskIdAndToolName(taskId, toolName);
        if (CollectionUtils.isNotEmpty(enabledSnippets)) {
            enabledSnippets.stream()
                    // 过滤非本次上报的文件路径
                    .filter(it -> !snippetElementIds.contains(it.getElementId())
                            && scaScanContext.isFileScanned(it.getFilePath(), it.getFileRelPath()))
                    .forEach(it -> {
                        it.setStatus(Status.DISABLE.value());
                        it.setFixedBuildId(buildId);
                        it.applyAuditInfoOnUpdate();
                        needUpdateSbomSnippets.add(it);
                    });
        }
        scaSbomService.saveSbomSnippets(taskId, needUpdateSbomSnippets);
    }

    private void updateOldSbomFileInfo(SCASbomFileEntity newSbomFile, SCASbomFileEntity oldSbomFile) {
        if (newSbomFile == null || oldSbomFile == null) {
            return;
        }

        // 显式复制需要更新的字段，保留审计和状态相关字段
        oldSbomFile.setElementId(newSbomFile.getElementId());
        oldSbomFile.setFileName(newSbomFile.getFileName());
        oldSbomFile.setFileRelPath(newSbomFile.getFileRelPath());
        oldSbomFile.setFilePath(newSbomFile.getFilePath());
        oldSbomFile.setChecksums(newSbomFile.getChecksums());
        oldSbomFile.setFileTypes(newSbomFile.getFileTypes());
        oldSbomFile.setDataFileName(newSbomFile.getDataFileName());
        oldSbomFile.setLastUpdateAuthor(newSbomFile.getLastUpdateAuthor());
        oldSbomFile.setLastUpdateTime(newSbomFile.getLastUpdateTime());
        oldSbomFile.setRevision(newSbomFile.getRevision());
        oldSbomFile.setBranch(newSbomFile.getBranch());
        oldSbomFile.setSubModule(newSbomFile.getSubModule());
        oldSbomFile.setUrl(newSbomFile.getUrl());
        oldSbomFile.setRepoId(newSbomFile.getRepoId());

    }

    private void updateOldSbomSnippetInfo(SCASbomSnippetEntity newSbomSnippet, SCASbomSnippetEntity oldSbomSnippet) {
        if (newSbomSnippet == null || oldSbomSnippet == null) {
            return;
        }

        // 显式复制需要更新的字段，保留审计和状态相关字段
        oldSbomSnippet.setElementId(newSbomSnippet.getElementId());
        oldSbomSnippet.setSnippetFromFile(newSbomSnippet.getSnippetFromFile());
        oldSbomSnippet.setRanges(newSbomSnippet.getRanges());
        oldSbomSnippet.setLastUpdateAuthor(newSbomSnippet.getLastUpdateAuthor());
        oldSbomSnippet.setLastUpdateTime(newSbomSnippet.getLastUpdateTime());
        oldSbomSnippet.setFileRelPath(newSbomSnippet.getFileRelPath());
        oldSbomSnippet.setFilePath(newSbomSnippet.getFilePath());

    }

    /**
     * 保存SBOM依赖信息（部分替换）
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param aggregateModel
     * @param isFullUpload
     */
    private void uploadSbomRelationships(long taskId, String toolName, String buildId,
            SCASbomAggregateModel aggregateModel, Boolean isFullUpload) {
        if (BooleanUtils.isTrue(isFullUpload)) {
            scaSbomService.deleteAndInsertNewRelations(taskId, toolName, aggregateModel.getRelationships());
            return;
        }
        // 获取本次的元素ID
        Set<String> elementIds = new HashSet<>();
        elementIds.addAll(aggregateModel.getPackages().stream().map(SCASbomPackageEntity::getElementId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        elementIds.addAll(aggregateModel.getFiles().stream().map(SCASbomFileEntity::getElementId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        elementIds.addAll(aggregateModel.getSnippets().stream().map(SCASbomSnippetEntity::getElementId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        // 更新
        scaSbomService.updateRelationship(taskId, toolName, aggregateModel.getRelationships(), elementIds);


    }

    private void batchUploadVulnerability(Long taskId, String toolName, String streamName, String buildId,
            BuildEntity buildEntity, List<SCASbomPackageEntity> oriPackages,
            List<SCASbomPackageEntity> newPackages) {
        // package信息准备
        Map<String, List<SCASbomPackageEntity>> packageNameToEntities = CollectionUtils.isEmpty(oriPackages)
                ? Collections.emptyMap() : oriPackages.stream().filter(it -> StringUtils.isNotBlank(it.getName()))
                .collect(Collectors.groupingBy(SCASbomPackageEntity::getName));
        Set<String> validPackageNames = CollectionUtils.isEmpty(newPackages) ? Collections.emptySet() :
                newPackages.stream().map(SCASbomPackageEntity::getName).collect(Collectors.toSet());

        // 获取告警文件
        log.info("batch upload vul start: {} {} {} {}", taskId, streamName, toolName, buildId);
        String fileIndex = scmJsonComponent.getRawScaVulIndex(streamName, toolName, buildId);
        if (StringUtils.isEmpty(fileIndex)) {
            log.warn("Can not find raw defect file:{}, {}, {}", streamName, toolName, buildId);
            return;
        }
        File defectFile = new File(fileIndex);
        if (!defectFile.exists()) {
            log.warn("文件不存在: {}", fileIndex);
            return;
        }

        // 通过流式读json文件
        try (FileInputStream fileInputStream = new FileInputStream(defectFile);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                JSONReader reader = new JSONReader(inputStreamReader)) {
            reader.startArray();
            List<SCAVulnerabilityEntity> scaVulnerabilityEntities = new LinkedList<>();
            int cursor = 0;
            Set<String> eachBatchPackageNames = new HashSet<>();
            while (reader.hasNext()) {
                SCAPackageVulnerabilityEntity packageVulnerability =
                        reader.readObject(SCAPackageVulnerabilityEntity.class);
                if (CollectionUtils.isNotEmpty(packageVulnerability.getVulnerabilities())) {
                    List<SCAVulnerabilityEntity> tmpVulList = packageVulnerability.getVulnerabilities();
                    if (CollectionUtils.isEmpty(tmpVulList)) {
                        log.warn("package defectList is empty. {}, {}, {}", taskId, toolName,
                                packageVulnerability.getPackageName());
                        continue;
                    }
                    // 补充漏洞信息
                    fillVulnerabilityInfoByPackage(tmpVulList, validPackageNames, packageNameToEntities);
                    scaVulnerabilityEntities.addAll(tmpVulList);
                    eachBatchPackageNames.add(packageVulnerability.getPackageName());
                    cursor += tmpVulList.size();
                    if (cursor > MAX_PER_BATCH) {
                        // 分批处理包的漏洞
                        processPackageVulnerability(taskId, toolName, buildEntity, eachBatchPackageNames,
                                scaVulnerabilityEntities);
                        cursor = 0;
                        scaVulnerabilityEntities = new LinkedList<>();
                        eachBatchPackageNames = new HashSet<>();
                    }
                } else {
                    log.warn("package vulList is empty. {}, {}, {}", taskId, toolName,
                            packageVulnerability.getPackageName());
                }
            }
            reader.endArray();
            if (!scaVulnerabilityEntities.isEmpty()) {
                processPackageVulnerability(taskId, toolName, buildEntity, eachBatchPackageNames,
                        scaVulnerabilityEntities);
            }
        } catch (IOException e) {
            log.warn("Read defect file exception: {}", fileIndex, e);
        }
        // 读取更新完成后，将本次没有上报的新告警全部标注为已修复
        List<SCAVulnerabilityEntity> needCloedVulList =
                scaVulnerabilityService.getNewVulByPackageNamesNotIn(taskId, toolName, validPackageNames);
        if (CollectionUtils.isEmpty(needCloedVulList)) {
            long curTime = System.currentTimeMillis();
            needCloedVulList.forEach(it -> closedSCAVulnerability(it, curTime, buildEntity));
            log.info("batch closed vul size: {} {} {} {}", taskId, toolName, buildId, needCloedVulList.size());
            scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needCloedVulList);
        }
        log.info("batch upload vul end: {} {} {} {}", taskId, streamName, toolName, buildId);
    }

    private void fillVulnerabilityInfoByPackage(List<SCAVulnerabilityEntity> vulList, Set<String> validPackageNames,
            Map<String, List<SCASbomPackageEntity>> packageNameToEntities) {
        if (CollectionUtils.isEmpty(vulList)) {
            return;
        }
        vulList.forEach(it -> {
            if (StringUtils.isBlank(it.getPackageName()) || !validPackageNames.contains(it.getPackageName())) {
                it.setHasEnabledPackage(false);
            }
            // 补充相关的包名
            if (StringUtils.isNotBlank(it.getPackageName()) && packageNameToEntities.containsKey(it.getPackageName())
                    && CollectionUtils.isNotEmpty(packageNameToEntities.get(it.getPackageName()))) {
                List<SCASbomPackageEntity> packageList = packageNameToEntities.get(it.getPackageName());
                List<String> authors = packageList.stream()
                        .map(SCASbomPackageEntity::getAuthor).filter(CollectionUtils::isNotEmpty)
                        .flatMap(List::stream).filter(StringUtils::isNotBlank).distinct()  // 去重
                        .collect(Collectors.toList());
                // 补充处理人列表，所有相关的组件引入人都需要处理
                it.setAuthor(authors);
                it.setAffectedPackages(packageList.stream().map(sbomPackage ->
                                new SbomPackageBase(sbomPackage.getEntityId(), sbomPackage.getElementId(),
                                        sbomPackage.getName(), sbomPackage.getVersion()))
                        .collect(Collectors.toList()));
            }
        });
    }

    /**
     * 批量处理漏洞
     *
     * @param taskId
     * @param toolName
     * @param buildEntity
     * @param eachBatchPackageNames
     * @param scaVulnerabilityEntities
     */
    private void processPackageVulnerability(Long taskId, String toolName, BuildEntity buildEntity,
            Set<String> eachBatchPackageNames,
            List<SCAVulnerabilityEntity> scaVulnerabilityEntities) {
        // 通过package找到告警
        List<SCAVulnerabilityEntity> oldVulList = scaVulnerabilityService.getVulnerabilityByPackageNames(
                taskId, toolName, eachBatchPackageNames);
        // 建立ID到漏洞的映射，通过漏洞ID锁定告警(CVE-xxx等)
        Map<String, SCAVulnerabilityEntity> oldIdToVulMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(oldVulList)) {
            for (SCAVulnerabilityEntity oldVul : oldVulList) {
                if (CollectionUtils.isEmpty(oldVul.getVulnerabilityIds())) {
                    continue;
                }
                for (String vulnerabilityId : oldVul.getVulnerabilityIds()) {
                    if (StringUtils.isNotEmpty(vulnerabilityId)) {
                        oldIdToVulMap.put(vulnerabilityId, oldVul);
                    }
                }
            }
        }
        long curTime = System.currentTimeMillis();
        List<SCAVulnerabilityEntity> needUpdateVulList = new LinkedList<>();
        if (CollectionUtils.isEmpty(scaVulnerabilityEntities)) {
            List<SCAVulnerabilityEntity> oldNeedUpdateVulList = oldVulList.stream()
                    .filter(it -> it.getStatus() == DefectStatus.NEW.value()).collect(Collectors.toList());
            oldNeedUpdateVulList.forEach(it -> closedSCAVulnerability(it, curTime, buildEntity));
            needUpdateVulList.addAll(oldNeedUpdateVulList);
        } else {
            // 记录匹配上的漏洞ID，用于后续将没有匹配上的老告警标记修复
            Set<String> updateIds = new HashSet<>();
            for (SCAVulnerabilityEntity scaVul : scaVulnerabilityEntities) {
                String vulId = null;
                if (CollectionUtils.isNotEmpty(scaVul.getVulnerabilityIds())) {
                    // 找到漏洞ID匹配的告警
                    vulId = scaVul.getVulnerabilityIds().stream().filter(oldIdToVulMap::containsKey).findFirst()
                            .orElse(null);
                }
                SCAVulnerabilityEntity oldVul = scaVul;
                if (StringUtils.isEmpty(vulId)) {
                    needUpdateVulList.add(oldVul);
                    continue;
                }
                updateIds.add(vulId);
                oldVul = oldIdToVulMap.get(vulId);
                updateOldVulnerabilityInfo(scaVul, oldVul);
                // 更新状态
                if ((oldVul.getStatus() & DefectStatus.FIXED.value()) > 0) {
                    openSCAVulnerability(oldVul);
                }
                needUpdateVulList.add(oldVul);
            }
            if (CollectionUtils.isNotEmpty(oldVulList)) {
                List<SCAVulnerabilityEntity> oldNeedUpdateVulList = oldVulList.stream()
                        .filter(it -> CollectionUtils.isEmpty(it.getVulnerabilityIds())
                                || it.getVulnerabilityIds().stream().noneMatch(updateIds::contains))
                        .filter(it -> it.getStatus() == DefectStatus.NEW.value()).collect(Collectors.toList());
                oldNeedUpdateVulList.forEach(it -> closedSCAVulnerability(it, curTime, buildEntity));
                needUpdateVulList.addAll(oldNeedUpdateVulList);
            }
        }
        scaVulnerabilityService.saveVulnerabilities(taskId, toolName, needUpdateVulList);
        log.info("batch update vul size: {} {} {} {}", taskId, toolName, buildEntity.getBuildId(),
                needUpdateVulList.size());
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
        scaVul.setHasEnabledPackage(false);
        scaVul.setFixedBuildNumber(buildEntity == null ? null : buildEntity.getBuildNo());
    }

    private void batchUploadLicense(Long taskId, String toolName, String buildId, BuildEntity buildEntity,
            List<SCASbomPackageEntity> oriPackages, List<SCASbomPackageEntity> newPackages) {
        log.info("batch upload license start: {} {} {}", taskId, toolName, buildId);

        List<SCALicenseEntity> needUpdateLicenses = new LinkedList<>();
        List<SCALicenseEntity> oldLicenseList = scaLicenseService.getLicenseByTaskIdAndToolName(taskId, toolName);

        // 获取证书详情，这里存在别名，所以需要证书详情辅助识别
        List<String> oriLicenses = getLicenseFromPackage(oriPackages);
        List<String> newLicenses = getLicenseFromPackage(newPackages);
        List<String> currentLicenses = Stream.concat(oriLicenses.stream(), newLicenses.stream())
                .collect(Collectors.toList());
        Map<String, LicenseDetailEntity> licenseDetailEntityMap = getLicenseDetailMapping(currentLicenses);
        List<String> filterLicenses = filterLicense(currentLicenses, licenseDetailEntityMap);

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

    private List<String> getLicenseFromPackage(List<SCASbomPackageEntity> packcages) {
        List<String> licenses = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(packcages)) {
            // 从组件中获取所有证书名称
            licenses = packcages.stream().map(SCASbomPackageEntity::getLicenses)
                    .flatMap(List::stream).map(String::trim).collect(Collectors.toList());
        }
        return licenses;
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
