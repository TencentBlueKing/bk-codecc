package com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import com.tencent.bk.codecc.defect.model.sca.SCAPackageFileInfo
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomChecksum
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomExternalRef
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulAffectedPackage
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulAffectedPackageVersion
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilityRating
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilitySource
import com.tencent.devops.common.api.enum.VulnerabilityRatingMethod
import com.tencent.devops.common.constant.ComConstants.EMPTY_STRING
import com.tencent.devops.common.constant.DefectConstants
import com.tencent.devops.common.util.DateTimeUtils

/**
 * 表示CycloneDX BOM（物料清单）的数据模型类
 *
 * 此类对应CycloneDX规范定义的BOM结构，包含组件、依赖关系和漏洞等信息
 *
 * @property bomFormat BOM格式标识符（固定为"CycloneDX"）
 * @property specVersion CycloneDX规范版本
 * @property serialNumber BOM序列号（可选）
 * @property version BOM版本号（可选）
 * @property metadata BOM元数据信息
 * @property components 组件列表
 * @property externalReferences 外部引用列表
 * @property dependencies 依赖关系列表
 * @property vulnerabilities 漏洞信息列表
 */
class CycloneDX(
    /**
     * CycloneDX
     */
    @JsonProperty("bomFormat")
    var bomFormat: String = EMPTY_STRING,

    /**
     * 版本
     */
    @JsonProperty("specVersion")
    var specVersion: String = EMPTY_STRING,

    /**
     * BOM序列号
     */
    @JsonProperty("serialNumber")
    var serialNumber: String? = null,

    /**
     * BOM版本号
     */
    @JsonProperty("version")
    var version: Int? = null,

    /**
     * BOM元数据信息
     */
    @JsonProperty("metadata")
    var metadata: CycloneDXMetadata? = null,

    /**
     * 组件列表
     */
    @JsonProperty("components")
    var components: List<CycloneDXComponent>? = null,

    /**
     * 外部引用列表
     */
    @JsonProperty("externalReferences")
    var externalReferences: List<CycloneDXExtReference>? = null,

    /**
     * 依赖关系列表
     */
    @JsonProperty("dependencies")
    var dependencies: List<CycloneDXDependency>? = null,

    /**
     * 漏洞信息列表
     */
    @JsonProperty("vulnerabilities")
    var vulnerabilities: List<CycloneDXVulnerability>? = null
) {

    fun getSCASbomAggregateModel(taskId: Long, toolName: String): SCASbomAggregateModel {
        val packages = getPackageToolDefects(taskId, toolName)
        val vulnerabilities = getVulnerabilityToolDefects(taskId, toolName)
        val licenses = getLicenseToolDefects(taskId, toolName)
        return SCASbomAggregateModel(taskId, toolName, packages, vulnerabilities, licenses)
    }

    /**
     * 将许可证信息转换为许可证工具缺陷实体列表
     *
     * @param taskId 关联的任务ID
     * @param toolName 工具名称
     * @return 转换后的许可证工具缺陷实体列表
     */
    fun getLicenseToolDefects(taskId: Long, toolName: String): List<SCALicenseEntity> {
        // 检查许可证列表是否为空
        if (components.isNullOrEmpty()) {
            return emptyList()
        }
        val licenses = components!!.filter { !it.licenses.isNullOrEmpty() }.flatMap {
            it.licenses?.filter { license -> license.license != null && !license.license!!.id.isNullOrEmpty() }
                ?.map { license -> license.license!!.id!! } ?: emptyList()
        }.distinct()

        return licenses.map {
            SCALicenseEntity(
                name = it
            ).also { defect ->
                // 设置实体关联信息
                defect.taskId = taskId
                defect.toolName = toolName
            }
        }
    }


    /**
     * 将漏洞信息转换为漏洞工具缺陷实体列表
     *
     * @param taskId 关联的任务ID
     * @param toolName 工具名称
     * @param executionId 执行ID
     * @return 转换后的漏洞工具缺陷实体列表
     */
    fun getVulnerabilityToolDefects(
        taskId: Long,
        toolName: String
    ): List<SCAVulnerabilityEntity> {
        // 检查漏洞列表是否为空
        if (vulnerabilities.isNullOrEmpty()) {
            return emptyList()
        }
        // 构建bomRef到组件的映射
        val bomRefToComponents = components!!.associateBy { it.bomRef!! }
        return vulnerabilities!!.filter { it.affects != null && it.affects!!.isNotEmpty() }.flatMap {
            it.affects!!.filter { affect -> bomRefToComponents.containsKey(affect.ref) }.mapNotNull { affect ->
                val component = bomRefToComponents[affect.ref] ?: return@mapNotNull null
                SCAVulnerabilityEntity(
                    packageId = getPackageId(component),
                    packageName = component.name,
                    packageVersion = component.version,
                    name = it.id.orEmpty(),
                    vulnerabilityIds = listOf(it.id.orEmpty()),
                    refUrl = it.source?.url,
                    // "2025-06-18T09:38:44Z" 转时间戳
                    modifiedDate = it.updated?.let { update ->
                        DateTimeUtils.convertStringDateToLongTime(update, DateTimeUtils.fullFormatWithT)
                    },
                    publishedDate = it.published?.let { published ->
                        DateTimeUtils.convertStringDateToLongTime(published, DateTimeUtils.fullFormatWithT)
                    },
                    cvssV3 = it.ratings?.firstOrNull { rating ->
                        !rating.method.isNullOrEmpty() && VulnerabilityRatingMethod.CVSSV3s.contains(rating.method)
                    }?.let { rating ->
                        SCAVulnerabilityRating(
                            vector = rating.vector.orEmpty(),
                            score = rating.score ?: 0.0
                        )
                    },
                    cvssV2 = it.ratings?.firstOrNull { rating ->
                        !rating.method.isNullOrEmpty() && rating.method!! == VulnerabilityRatingMethod.CVSSv2.value
                    }?.let { rating ->
                        SCAVulnerabilityRating(
                            vector = rating.vector.orEmpty(),
                            score = rating.score ?: 0.0
                        )
                    },
                    affectedPackages = getVulAffectedPackage(
                        it.affects,
                        component.name,
                        bomRefToComponents,
                        it.recommendation
                    ),
                    source = it.source?.let { source ->
                        listOf(SCAVulnerabilitySource(name = source.name.orEmpty(), url = source.url.orEmpty()))
                    },
                    ratings = it.ratings?.map { rating ->
                        SCAVulnerabilityRating(
                            vector = rating.vector.orEmpty(),
                            method = rating.method.orEmpty(),
                            score = rating.score ?: 0.0
                        )
                    },
                ).also { defect ->
                    // 设置实体关联信息
                    defect.taskId = taskId
                    defect.toolName = toolName
                    defect.severity = getSeverityByRatings(it.ratings)
                }
            }
        }
    }

    /**
     * 根据漏洞评级计算严重等级
     *
     * @param ratings 漏洞评级列表
     * @return 最高严重等级对应的枚举值
     */
    private fun getSeverityByRatings(ratings: List<CycloneDXVulRating>?): Int {
        if (ratings.isNullOrEmpty()) {
            return DefectConstants.SCADefectSeverity.UNKNOWN.value()
        }
        return ratings.filter { !it.severity.isNullOrEmpty() }.maxOf {
            if (it.severity.equals("none", true)) {
                DefectConstants.SCADefectSeverity.LOW.value()
            } else {
                DefectConstants.SCADefectSeverity.getSeverityByValue(it.severity!!)
            }
        }
    }

    /**
     * 获取受漏洞影响的包信息
     *
     * @param affects 漏洞影响范围列表
     * @param bomRefToComponents bomRef到组件的映射
     * @param recommendation 修复建议
     * @return 受影响的包信息列表
     */
    private fun getVulAffectedPackage(
        affects: List<CycloneDXVulAffect>?,
        packageName: String,
        bomRefToComponents: Map<String, CycloneDXComponent>,
        recommendation: String?
    ): List<SCAVulAffectedPackage> {
        if (affects.isNullOrEmpty() || bomRefToComponents.isEmpty()) {
            return emptyList()
        }
        return affects.filter { bomRefToComponents.containsKey(it.ref) }
            .filter { bomRefToComponents[it.ref]!!.name == packageName }.map {
                val component = bomRefToComponents[it.ref]!!
                SCAVulAffectedPackage(
                    packageName = component.name,
                    versions = it.versions?.map { version ->
                        SCAVulAffectedPackageVersion(
                            version = version.version.orEmpty(),
                            range = version.range.orEmpty()
                        )
                    } ?: emptyList(),
                    fixAdvice = recommendation.orEmpty()
                )
            }
    }


    private fun getPackageId(component: CycloneDXComponent) = "${component.name}:${component.version}"

    /**
     * 将组件信息转换为包工具缺陷实体列表
     *
     * @param taskId 关联的任务ID
     * @param toolName 工具名称
     * @return 转换后的包工具缺陷实体列表
     */
    fun getPackageToolDefects(taskId: Long, toolName: String): List<SCASbomPackageEntity> {
        if (components.isNullOrEmpty()) {
            return emptyList()
        }
        // 1. 先flatMap所有组件和路径对
        val idToFilePaths = components!!.flatMap { component ->
            val id = "${component.name}:${component.version}"
            component.properties
                ?.filter { it.name.matches(Regex("codecc:location:\\d+:path")) }
                ?.map { id to it.value } ?: emptyList()
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.distinct() }

        // 2. components 根据 id 去重，取第一个
        val distinctComponents = components!!.distinctBy { "${it.name}:${it.version}" }

        return distinctComponents.map { component ->
            val id = "${component.name}:${component.version}"
            var licenses = component.licenses?.filter { it.license != null }?.mapNotNull { it.license!!.id }
            val filePaths = idToFilePaths[id]
            SCASbomPackageEntity(
                taskId = taskId,
                toolName= toolName,
                status = DefectConstants.DefectStatus.NEW.value(),
            ).apply {
                fileInfos = covertToFileInfo(filePaths)
                checksums = convertToChecksum(component.hashes)
                severity = DefectConstants.SCADefectSeverity.UNKNOWN.value()
                description = component.description
                name = component.name
                version = component.version
                originator = component.publisher
                supplier = component.supplier?.name
                filesAnalyzed = false
                externalRefs = convertToExternalRefs(component.purl)
                licensesConcluded = licenses ?: emptyList()
                licensesDeclared = licenses ?: emptyList()
                licenses = licenses ?: emptyList()
                depth = 1
            }
        }
    }

    private fun convertToChecksum(hashes: List<CycloneDXHash>?): List<SbomChecksum>? {
        return hashes.takeIf { !it.isNullOrEmpty() }?.map {
            SbomChecksum(algorithm = it.alg, checksumValue = it.content)
        }
    }

    private fun convertToExternalRefs(purl: String?): List<SbomExternalRef>? {
        return if (!purl.isNullOrEmpty()) {
            listOf(
                SbomExternalRef(
                    referenceCategory = "PACKAGE-MANAGER",
                    referenceType = "purl",
                    referenceLocator = purl
                )
            )
        } else {
            null
        }
    }

    private fun covertToFileInfo(filePaths: List<String>?): List<SCAPackageFileInfo> {
        if (filePaths.isNullOrEmpty()) {
            return emptyList()
        }
        return filePaths.map {
            SCAPackageFileInfo().apply {
                filePath = it
            }
        }
    }
}
