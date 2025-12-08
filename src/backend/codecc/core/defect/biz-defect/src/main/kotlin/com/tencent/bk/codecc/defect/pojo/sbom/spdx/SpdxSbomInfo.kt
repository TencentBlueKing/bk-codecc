package com.tencent.bk.codecc.defect.pojo.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.constant.SbomRelationshipType
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import com.tencent.bk.codecc.defect.model.sca.SCAPackageFileInfo
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomCreationInfo
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.service.utils.ToolParamUtils
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

/**
 * SBOM 生成信息
 */
class SpdxSbomInfo(
    /**
     * SBOM ID
     */
    @JsonProperty("SPDXID")
    var elementId: String? = null,
    /**
     * 版本
     */
    @JsonProperty("spdxVersion")
    var version: String? = null,
    /**
     * 创建信息
     */
    @JsonProperty("creationInfo")
    var createInfo: SbomCreationInfo? = null,
    /**
     * 名称
     */
    @JsonProperty("name")
    var name: String? = null,
    /**
     * 描述
     */
    @JsonProperty("comment")
    var comment: String? = null,
    /**
     * 数据许可证
     */
    @JsonProperty("dataLicense")
    var dataLicense: String? = null,
    /**
     * 组件列表
     */
    @JsonProperty("packages")
    var packages: List<SpdxSbomPackage>? = null,

    /**
     * 文件列表
     */
    @JsonProperty("files")
    var files: List<SpdxSbomFile>? = null,

    /**
     * 代码片段列表
     */
    @JsonProperty("snippets")
    var snippets: List<SpdxSbomSnippet>? = null,

    /**
     * 组件列表
     */
    @JsonProperty("relationships")
    var relationships: List<SpdxSbomRelationship>? = null,
) {

    fun getSCASbomAggregateModel(
        taskId: Long,
        toolName: String,
        fileChangeRecordsMap: Map<String, ScmBlameVO>,
        codeRepoIdMap: Map<String, RepoSubModuleVO>
    ): SCASbomAggregateModel {
        val packages = getSbomPackages(taskId, toolName)
        val files = getSbomFiles(taskId, toolName)
        val snippets = getSbomSnippets(taskId, toolName)
        val relationships = getSbomRelationships(taskId, toolName)
        val aggregateModel = SCASbomAggregateModel(taskId, toolName, packages, emptyList(),
            getLicenses(taskId, toolName, packages))

        // 填充组件、文件和代码片段的关系信息
        fillPackagesAndFilesAndSnippetsInfo(
            files, snippets, packages, relationships,
            fileChangeRecordsMap, codeRepoIdMap
        )

        return aggregateModel
    }

    private fun getSbomPackages(taskId: Long, toolName: String): List<SCASbomPackageEntity> {
        if (packages.isNullOrEmpty()) {
            return emptyList()
        }
        val sbomPackage = mutableListOf<SCASbomPackageEntity>()
        packages!!.forEach {
            sbomPackage.add(it.getSbomPackage(taskId, toolName, DefectStatus.NEW.value()))
        }
        return sbomPackage
    }

    private fun getLicenses(
        taskId: Long,
        toolName: String,
        packages: List<SCASbomPackageEntity>
    ): List<SCALicenseEntity> {
        if (packages.isEmpty()) {
            return emptyList()
        }
        return packages.flatMap { it.licenses }.map {
            SCALicenseEntity(taskId = taskId, toolName = toolName, name = it)
        }
    }

    private fun getSbomFiles(taskId: Long, toolName: String): List<SCASbomFileEntity> {
        if (files.isNullOrEmpty()) {
            return emptyList()
        }
        val sbomFiles = mutableListOf<SCASbomFileEntity>()
        files!!.forEach {
            sbomFiles.add(it.getSbomFile(taskId, toolName))
        }
        return sbomFiles
    }

    private fun getSbomSnippets(taskId: Long, toolName: String): List<SCASbomSnippetEntity> {
        if (snippets.isNullOrEmpty()) {
            return emptyList()
        }
        val sbomSnippets = mutableListOf<SCASbomSnippetEntity>()
        snippets!!.forEach {
            sbomSnippets.add(it.getSbomSnippet(taskId, toolName))
        }
        return sbomSnippets
    }

    private fun getSbomRelationships(taskId: Long, toolName: String): List<SCASbomRelationshipEntity> {
        if (relationships.isNullOrEmpty()) {
            return emptyList()
        }
        val sbomRelationship = mutableListOf<SCASbomRelationshipEntity>()
        relationships!!.forEach {
            sbomRelationship.add(it.getSbomRelationship(taskId, toolName))
        }
        return sbomRelationship
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpdxSbomInfo::class.java)
    }

    /**
     * 填充组件、文件和代码片段的关系信息
     * 1. 填充文件和代码片段的基础信息
     * 2. 通过依赖关系建立组件与文件/代码片段的关联
     * 3. 记录代码片段与文件关联的信息
     *
     * @param aggregateModel       SBOM聚合模型
     * @param fileChangeRecordsMap 文件变更记录映射
     * @param codeRepoIdMap        代码仓库ID映射
     */
    private fun fillPackagesAndFilesAndSnippetsInfo(
        files: List<SCASbomFileEntity>,
        snippets: List<SCASbomSnippetEntity>,
        packages: List<SCASbomPackageEntity>,
        relationships: List<SCASbomRelationshipEntity>,
        fileChangeRecordsMap: Map<String, ScmBlameVO>,
        codeRepoIdMap: Map<String, RepoSubModuleVO>
    ) {
        log.debug("[SCA] Start filling package-file-snippet relationships")
        // 步骤1: 填充文件和代码片段的基础信息（文件名、代码版本、作者等）
        fillFileAndSnippetInfo(files, snippets, fileChangeRecordsMap, codeRepoIdMap)

        // 步骤2: 处理组件依赖关系

        // 构建依赖关系映射：packageId -> [DEPENDENCY_MANIFEST_OF 关系]
        val dependencyManifestOf = relationships.stream()
            .filter {
                StringUtils.isNotEmpty(it.elementId) &&
                        StringUtils.isNotEmpty(it.relatedElement) &&
                        SbomRelationshipType.DEPENDENCY_MANIFEST_OF.name == it.relationshipType
            }
            .collect(Collectors.groupingBy(SCASbomRelationshipEntity::relatedElement))

        // 构建快速查找映射
        val idToSnippets = snippets.associateBy { it.elementId!! }
        val idToFiles = files.associateBy { it.elementId!! }

        log.debug("[SCA] Processing {} packages with dependency relationships", packages.size)

        // 步骤3: 为每个组件查找依赖来源
        for (scaSbomPackage in packages) {
            val packageId = scaSbomPackage.elementId
            val packageDependFromRelationships = dependencyManifestOf[packageId]

            if (CollectionUtils.isEmpty(packageDependFromRelationships)) {
                log.debug("[SCA] Package {} has no dependency manifest relationships", packageId)
                continue
            }

            log.debug(
                "[SCA] Processing package {} with {} dependency relationships",
                packageId, packageDependFromRelationships!!.size
            )

            // 记录所有匹配上的文件与代码片段位置
            val packageFileInfos = mutableListOf<SCAPackageFileInfo>()
            for (relationship in packageDependFromRelationships) {
                val packageFileInfo = SCAPackageFileInfo()
                if (idToSnippets.containsKey(relationship.elementId)) {
                    // 处理代码片段依赖
                    val snippet = idToSnippets[relationship.elementId]!!
                    linkPackageToSnippet(packageFileInfo, snippet, idToFiles)
                } else if (idToFiles.containsKey(relationship.elementId)) {
                    // 处理文件依赖
                    val file = idToFiles[relationship.elementId]!!
                    linkPackageToFile(packageFileInfo, file)
                }
                if (StringUtils.isNotEmpty(packageFileInfo.fileElementId)) {
                    packageFileInfos.add(packageFileInfo)
                }
            }

            if (CollectionUtils.isEmpty(packageFileInfos)) {
                return
            }
            // 根据getFileElementId分组
            fillPackageFileInfo(scaSbomPackage, packageFileInfos, idToFiles)
        }
        log.debug("[SCA] Finished filling package-file-snippet relationships")
    }

    /**
     * 填充软件包文件信息
     *
     * 主要功能:
     * 1. 合并相同fileElementId的包文件信息
     * 2. 根据最后更新时间最大的文件信息设置包的仓库相关信息
     *
     * @param scaSbomPackage   需要填充信息的SBOM包实体
     * @param packageFileInfos 包文件信息列表
     * @param idToFiles        文件ID到SBOM文件实体的映射
     */
    private fun fillPackageFileInfo(
        scaSbomPackage: SCASbomPackageEntity,
        packageFileInfos: List<SCAPackageFileInfo>,
        idToFiles: Map<String, SCASbomFileEntity>
    ) {
        if (CollectionUtils.isEmpty(packageFileInfos)) {
            return
        }
        // 按fileElementId对文件信息进行分组
        val groupedByFileElementId = packageFileInfos.stream()
            .filter { StringUtils.isNotEmpty(it.fileElementId) }
            .collect(Collectors.groupingBy(SCAPackageFileInfo::fileElementId))

        // 合并相同fileElementId的文件信息
        val mergedPackageFileInfos = groupedByFileElementId.values.stream()
            .map { group ->
                val firstFileInfo = group[0]
                // 合并所有snippet元素ID
                val snippetElementIds = group.stream()
                    .filter { it != null && CollectionUtils.isNotEmpty(it.snippetElementIds) }
                    .flatMap { info -> info.snippetElementIds!!.stream() }
                    .distinct()
                    .collect(Collectors.toList())
                // 合并所有作者信息
                val authors = getAuthorFromPackageFileInfos(group)
                // 获取最大的最后更新时间
                val lastUpdateTime = group.stream()
                    .mapToLong(SCAPackageFileInfo::lastUpdateTime)
                    .max()
                    .orElse(0L)

                // 创建合并后的文件信息对象
                SCAPackageFileInfo().apply {
                    fileName = firstFileInfo.fileName
                    filePath = firstFileInfo.filePath
                    relPath = firstFileInfo.relPath
                    fileElementId = firstFileInfo.fileElementId
                    this.snippetElementIds = snippetElementIds
                    this.authors = authors
                    this.lastUpdateTime = lastUpdateTime
                }
            }
            .collect(Collectors.toList())

        scaSbomPackage.fileInfos = mergedPackageFileInfos
        scaSbomPackage.author = getAuthorFromPackageFileInfos(mergedPackageFileInfos)

        // 获取最后更新时间最大的文件信息
        val maxLastUpdateTimeFileInfo = mergedPackageFileInfos.stream()
            .max(Comparator.comparingLong(SCAPackageFileInfo::lastUpdateTime))

        // 根据最新文件的信息设置包的仓库相关信息
        if (maxLastUpdateTimeFileInfo.isPresent) {
            val fileElementId = maxLastUpdateTimeFileInfo.get().fileElementId
            scaSbomPackage.lastUpdateTime = maxLastUpdateTimeFileInfo.get().lastUpdateTime
            val sbomFileEntity = idToFiles[fileElementId]
            if (sbomFileEntity != null) {
                scaSbomPackage.repoId = sbomFileEntity.repoId
                scaSbomPackage.revision = sbomFileEntity.revision
                scaSbomPackage.branch = sbomFileEntity.branch
                scaSbomPackage.url = sbomFileEntity.url
                scaSbomPackage.subModule = sbomFileEntity.subModule
            }
        }
    }

    /**
     * 将组件关联到代码片段及其所属文件
     */
    private fun linkPackageToSnippet(
        packageFileInfo: SCAPackageFileInfo,
        snippet: SCASbomSnippetEntity,
        idToFiles: Map<String, SCASbomFileEntity>
    ) {
        if (StringUtils.isNotEmpty(snippet.snippetFromFile)) {
            val sbomFileEntity = idToFiles[snippet.snippetFromFile]
            // 仅记录有路径的文件信息
            if (sbomFileEntity != null && StringUtils.isNotEmpty(sbomFileEntity.filePath)) {
                packageFileInfo.snippetElementIds = listOf(snippet.elementId!!)
                packageFileInfo.authors = listOf(snippet.lastUpdateAuthor ?: "")
                packageFileInfo.lastUpdateTime = snippet.lastUpdateTime
                packageFileInfo.fileElementId = sbomFileEntity.elementId
                fillPackageFileInfoByFile(packageFileInfo, sbomFileEntity)
            } else {
                log.warn(
                    "[SCA] Snippet {} references missing file {}",
                    snippet.elementId, snippet.snippetFromFile
                )
            }
        }
    }

    /**
     * 将组件直接关联到文件
     */
    private fun linkPackageToFile(
        packageFileInfo: SCAPackageFileInfo,
        file: SCASbomFileEntity?
    ) {
        if (file != null && StringUtils.isNotEmpty(file.filePath)) {
            // 仅记录有路径的文件信息
            packageFileInfo.fileElementId = file.elementId
            packageFileInfo.authors = listOf(file.lastUpdateAuthor ?: "")
            packageFileInfo.lastUpdateTime = file.lastUpdateTime
            fillPackageFileInfoByFile(packageFileInfo, file)
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
    private fun fillFileAndSnippetInfo(
        files: List<SCASbomFileEntity>,
        snippets: List<SCASbomSnippetEntity>,
        fileChangeRecordsMap: Map<String, ScmBlameVO>,
        codeRepoIdMap: Map<String, RepoSubModuleVO>
    ) {
        // 先将 fileChangeRecordsMap 转为 relPath 的模式，SBOM 文件信息中的文件都是相对路径，所以只能用相对路径匹配
        val fileChangeRecordsToRelPathMap = fileChangeRecordsMap.values.stream().collect(
            Collectors.toMap(ScmBlameVO::getFileRelPath, Function.identity()) { v1, _ -> v1 }
        )
        // 将代码片段按照文件ElementId分组
        val fileIdToSnippets = snippets.stream()
            .filter { StringUtils.isNotBlank(it.snippetFromFile) }
            .collect(Collectors.groupingBy(SCASbomSnippetEntity::snippetFromFile))

        // 从文件出发，先找到文件的ScmBlameVO
        for (file in files) {
            var relPath = file.fileRelPath
            // 没有relPath, 跳过
            if (StringUtils.isEmpty(relPath)) {
                file.fileName = ComConstants.EMPTY_STRING
                fillSnippetInfoByFile(file, fileIdToSnippets[file.elementId], null)
                continue
            }
            if (StringUtils.isNotEmpty(relPath) && relPath!!.startsWith("./")) {
                // 将./ 转为 /
                relPath = relPath.substring(1)
            }
            var fileNameIndex = relPath!!.lastIndexOf("/")
            if (fileNameIndex == -1) {
                fileNameIndex = relPath.lastIndexOf("\\")
            }
            if (relPath.endsWith("/") || relPath.endsWith("\\")) {
                file.fileName = ComConstants.EMPTY_STRING
            } else {
                file.fileName = relPath.substring(fileNameIndex + 1)
            }
            val scmBlameVO = fileChangeRecordsToRelPathMap[relPath]
            // 没有找到代码版本信息，跳过
            if (scmBlameVO == null) {
                fillSnippetInfoByFile(file, fileIdToSnippets[file.elementId], null)
                continue
            }
            file.url = scmBlameVO.url
            file.branch = scmBlameVO.branch
            file.revision = scmBlameVO.revision
            // 设置RepoId
            if (ComConstants.CodeHostingType.SVN.name.equals(scmBlameVO.scmType, ignoreCase = true)) {
                val repoSubModuleVO = codeRepoIdMap[scmBlameVO.rootUrl]
                if (repoSubModuleVO != null) {
                    file.repoId = repoSubModuleVO.repoId
                }
            } else {
                val repoSubModuleVO = codeRepoIdMap[scmBlameVO.url]
                if (repoSubModuleVO != null) {
                    file.repoId = repoSubModuleVO.repoId
                    if (StringUtils.isNotEmpty(repoSubModuleVO.subModule)) {
                        file.subModule = repoSubModuleVO.subModule
                    } else {
                        file.subModule = ""
                    }
                }
            }
            // 设置最新更新时间与作者
            if (CollectionUtils.isNotEmpty(scmBlameVO.changeRecords)) {
                val recordVO = scmBlameVO.changeRecords!!.stream()
                    .max(Comparator.comparingLong(ScmBlameChangeRecordVO::getLineUpdateTime))
                    .orElse(null)
                if (recordVO != null) {
                    file.lastUpdateAuthor = ToolParamUtils.trimUserName(recordVO.author)
                    file.lastUpdateTime = recordVO.lineUpdateTime
                }
            } else if (StringUtils.isNotBlank(scmBlameVO.fileAuthor)) {
                file.lastUpdateAuthor = ToolParamUtils.trimUserName(scmBlameVO.fileAuthor)
                file.lastUpdateTime = scmBlameVO.fileUpdateTime
            }
            // 设置文件关联的代码片段的信息
            fillSnippetInfoByFile(file, fileIdToSnippets[file.elementId], scmBlameVO)
        }
    }

    /**
     * 根据文件匹配的ScmBlameVO，设置代码片段的更新与最近更新时间
     *
     * @param file
     * @param snippets
     * @param scmBlameVO
     */
    private fun fillSnippetInfoByFile(
        file: SCASbomFileEntity,
        snippets: List<SCASbomSnippetEntity>?,
        scmBlameVO: ScmBlameVO?
    ) {
        if (CollectionUtils.isEmpty(snippets)) {
            return
        }
        // 设置所属文件的relPath
        for (snippet in snippets!!) {
            snippet.fileRelPath = file.fileRelPath
            snippet.filePath = file.filePath
        }
        // 如果没有SCM信息就跳过
        if (scmBlameVO == null || CollectionUtils.isEmpty(scmBlameVO.changeRecords)) {
            return
        }
        val changeRecords = scmBlameVO.changeRecords!!
        // 转换成 行号与ScmBlameChangeRecordVO的Map，方便查找
        val lineAuthorMap = getLineAuthorMap(changeRecords)
        for (snippet in snippets) {
            snippet.fileRelPath = file.fileRelPath
            if (CollectionUtils.isEmpty(snippet.ranges)) {
                continue
            }
            if (CollectionUtils.isNotEmpty(changeRecords)) {
                var functionLastUpdateTime = 0L
                var author: String? = null
                // 从代码片段的所有代码范围中，查找最近更新的
                for (startEndPointer in snippet.ranges!!) {
                    if (startEndPointer.startPointer == null ||
                        startEndPointer.startPointer!!.lineNumber == null ||
                        startEndPointer.endPointer == null ||
                        startEndPointer.endPointer!!.lineNumber == null
                    ) {
                        continue
                    }
                    // 获取函数涉及的所有行中的最新修改作者作为告警作者
                    for (i in startEndPointer.startPointer!!.lineNumber!!..startEndPointer.endPointer!!.lineNumber!!) {
                        val recordVO = lineAuthorMap[i]
                        if (recordVO != null && recordVO.lineUpdateTime > functionLastUpdateTime) {
                            functionLastUpdateTime = recordVO.lineUpdateTime
                            author = ToolParamUtils.trimUserName(recordVO.author)
                        }
                    }
                }
                // 如果找到了，设置告警作者
                if (StringUtils.isNotEmpty(author)) {
                    snippet.lastUpdateAuthor = author
                    snippet.lastUpdateTime = functionLastUpdateTime
                }
            }
        }
    }

    private fun fillPackageFileInfoByFile(
        packageFileInfo: SCAPackageFileInfo,
        sbomFileEntity: SCASbomFileEntity
    ) {
        if (packageFileInfo == null || sbomFileEntity == null) {
            return
        }
        packageFileInfo.fileName = sbomFileEntity.fileName
        packageFileInfo.relPath = sbomFileEntity.fileRelPath
        packageFileInfo.filePath = sbomFileEntity.filePath
    }

    /**
     * 从fileInfos中提取合并处理人信息
     *
     * @param fileInfos
     * @return
     */
    private fun getAuthorFromPackageFileInfos(fileInfos: List<SCAPackageFileInfo>): List<String> {
        if (CollectionUtils.isEmpty(fileInfos)) {
            return emptyList()
        }
        return fileInfos.stream()
            .filter { it != null && CollectionUtils.isNotEmpty(it.authors) }
            .flatMap { info -> info.authors!!.stream() }
            .distinct()
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList())
    }

    /**
     * 获取行号到作者信息的映射
     */
    private fun getLineAuthorMap(changeRecords: List<ScmBlameChangeRecordVO>): Map<Int, ScmBlameChangeRecordVO> {
        val lineAuthorMap = mutableMapOf<Int, ScmBlameChangeRecordVO>()
        for (changeRecord in changeRecords) {
            val lines = changeRecord.lines
            if (CollectionUtils.isNotEmpty(lines)) {
                for (line in lines!!) {
                    when (line) {
                        is Int -> {
                            lineAuthorMap[line] = changeRecord
                        }

                        is List<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val lineScope = line as List<Int>
                            for (i in lineScope[0]..lineScope[1]) {
                                lineAuthorMap[i] = changeRecord
                            }
                        }
                    }
                }
            }
        }
        return lineAuthorMap
    }
}
