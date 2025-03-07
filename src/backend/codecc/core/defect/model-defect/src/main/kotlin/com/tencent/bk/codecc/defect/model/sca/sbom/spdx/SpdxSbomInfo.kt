package com.tencent.bk.codecc.defect.model.sca.sbom.spdx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomFileEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomInfoEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomRelationshipEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomSnippetEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomCreationInfo
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.util.BeanUtils

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

    /**
     * 增量文件列表
     */
    @JsonProperty("incrementalFiles")
    var incrementalFiles: List<String>? = null
) {

    fun getSCASbomAggregateModel(taskId: Long, toolName: String): SCASbomAggregateModel {
        val info = getSbomInfo(taskId, toolName)
        val packages = getSbomPackages(taskId, toolName)
        val files = getSbomFiles(taskId, toolName)
        val snippets = getSbomSnippets(taskId, toolName)
        val relationships = getSbomRelationships(taskId, toolName)
        return SCASbomAggregateModel(taskId, toolName, info, packages, files, snippets, relationships)
    }

    private fun getSbomInfo(taskId: Long, toolName: String): SCASbomInfoEntity {
        val sbomInfo = SCASbomInfoEntity(taskId, toolName)
        BeanUtils.copyProperties(this, sbomInfo)
        return sbomInfo
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
}
