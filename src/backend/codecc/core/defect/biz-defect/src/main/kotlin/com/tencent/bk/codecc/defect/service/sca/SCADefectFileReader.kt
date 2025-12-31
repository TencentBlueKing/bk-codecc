package com.tencent.bk.codecc.defect.service.sca

import com.alibaba.fastjson2.JSONReader
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity
import com.tencent.bk.codecc.defect.pojo.sbom.ScaDefectFileInfo
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomCreationInfo
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDX
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDXDependency
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDXExtReference
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDXMetadata
import com.tencent.bk.codecc.defect.pojo.sbom.cyclonedx.CycloneDXVulnerability
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomFile
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomInfo
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomPackage
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomRelationship
import com.tencent.bk.codecc.defect.pojo.sbom.spdx.SpdxSbomSnippet
import com.tencent.bk.codecc.defect.model.sca.vulnerability.SCAVulnerabilityInfo
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.constant.DefectConstants.SCASbomType
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object SCADefectFileReader {

    private const val FILE_SIZE_THRESHOLD_FOR_STREAMING: Long = 1 * 1024 * 1024

    private val logger: Logger = LoggerFactory.getLogger(SCADefectFileReader::class.java)

    fun readFromJsonFile(jsonFilePath: String): ScaDefectFileInfo? {
        if (StringUtils.isEmpty(jsonFilePath)) {
            return null
        }
        val jsonFile = File(jsonFilePath)
        if (!jsonFile.exists()) {
            return null
        }
        return if (jsonFile.length() <= FILE_SIZE_THRESHOLD_FOR_STREAMING) {
            // 小于1M直接读取，转换JSON
            val spdxSbomJsonStr = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8)
            JsonUtil.to(spdxSbomJsonStr, ScaDefectFileInfo::class.java).apply {
                if (sbom != null && (sbomType.isNullOrEmpty() || sbomType == SCASbomType.SPDX.value())) {
                    spdxSbom = JsonUtil.to(JsonUtil.toJson(sbom), SpdxSbomInfo::class.java)
                } else if (sbom != null && sbomType == SCASbomType.CYCLONEDX.value()) {
                    cycloneDXSbom = JsonUtil.to(JsonUtil.toJson(sbom), CycloneDX::class.java)
                }
            }
        } else {
            // 大于1M 使用JSONReader逐个元素读取
            readJsonFileByStream(jsonFile)
        }
    }

    private fun readJsonFileByStream(jsonFile: File): ScaDefectFileInfo {
        val scaDefectFileInfo = readBaseInfoByStream(jsonFile)
        try {
            FileInputStream(jsonFile).use { fileInputStream ->
                JSONReader.of(fileInputStream, StandardCharsets.UTF_8).use { reader ->
                    if (!reader.nextIfObjectStart()) {
                        return scaDefectFileInfo
                    }
                    while (!reader.nextIfObjectEnd()) {
                        val key = reader.readFieldName()
                        when (key) {
                            "sbom" -> {
                                if (scaDefectFileInfo.sbomType.isNullOrEmpty() ||
                                    scaDefectFileInfo.sbomType == SCASbomType.SPDX.value()
                                ) {
                                    scaDefectFileInfo.spdxSbom = readSbomFromStream(reader, jsonFile.name)
                                } else if (scaDefectFileInfo.sbomType == SCASbomType.CYCLONEDX.value()) {
                                    scaDefectFileInfo.cycloneDXSbom =
                                        readCyCloneDxSbomFromStream(reader, jsonFile.name)
                                }
                            }

                            "vulnerabilities" -> {
                                scaDefectFileInfo.vulnerabilities =
                                    readArrayByStream(reader, SCAVulnerabilityInfo::class.java)
                            }

                            "packages" -> {
                                scaDefectFileInfo.packages =
                                    readArrayByStream(reader, SCASbomPackageEntity::class.java)
                            }

                            "licenses" -> {
                                scaDefectFileInfo.licenses =
                                    readArrayByStream(reader, SCALicenseEntity::class.java)
                            }

                            else -> reader.skipValue()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Read defect file exception: {}", jsonFile.name, e)
        }
        return scaDefectFileInfo
    }

    private fun readBaseInfoByStream(jsonFile: File): ScaDefectFileInfo {
        val scaDefectFileInfo = ScaDefectFileInfo()
        try {
            FileInputStream(jsonFile).use { fileInputStream ->
                JSONReader.of(fileInputStream, StandardCharsets.UTF_8).use { reader ->
                    if (!reader.nextIfObjectStart()) {
                        return scaDefectFileInfo
                    }
                    while (!reader.nextIfObjectEnd()) {
                        val key = reader.readFieldName()
                        when (key) {
                            "analysisPackageFromSbom" -> {
                                scaDefectFileInfo.analysisPackageFromSbom = reader.readBool()
                            }

                            "analysisLicenseFromSbom" -> {
                                scaDefectFileInfo.analysisLicenseFromSbom = reader.readBool()
                            }

                            "analysisVulnerabilityFromSbom" -> {
                                scaDefectFileInfo.analysisVulnerabilityFromSbom = reader.readBool()
                            }

                            "sbomType" -> {
                                scaDefectFileInfo.sbomType = reader.readString()
                            }

                            "incrementalFiles" -> {
                                scaDefectFileInfo.incrementalFiles = readStringArrayByStream(reader)
                            }

                            else -> reader.skipValue()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Read defect file exception: {}", jsonFile.name, e)
        }
        return scaDefectFileInfo
    }

    private fun readSbomFromStream(reader: JSONReader, fileName: String): SpdxSbomInfo? {
        try {
            val spdxSbomInfo = SpdxSbomInfo()
            if (!reader.nextIfObjectStart()) {
                return null
            }
            while (!reader.nextIfObjectEnd()) {
                val key = reader.readFieldName()
                when (key) {
                    "SPDXID" -> spdxSbomInfo.elementId = reader.readString()
                    "spdxVersion" -> spdxSbomInfo.version = reader.readString()
                    "creationInfo" -> {
                        spdxSbomInfo.createInfo =
                            readObjectByStream(reader, SbomCreationInfo::class.java)
                    }

                    "name" -> spdxSbomInfo.name = reader.readString()
                    "comment" -> spdxSbomInfo.comment = reader.readString()
                    "dataLicense" -> spdxSbomInfo.dataLicense = reader.readString()
                    "packages" -> {
                        spdxSbomInfo.packages = readArrayByStream(reader, SpdxSbomPackage::class.java)
                    }

                    "files" -> {
                        spdxSbomInfo.files = readArrayByStream(reader, SpdxSbomFile::class.java)
                    }

                    "snippets" -> {
                        spdxSbomInfo.snippets = readArrayByStream(reader, SpdxSbomSnippet::class.java)
                    }

                    "relationships" -> {
                        spdxSbomInfo.relationships =
                            readArrayByStream(reader, SpdxSbomRelationship::class.java)
                    }

                    else -> reader.skipValue()
                }
            }
            return spdxSbomInfo
        } catch (e: IOException) {
            logger.error("Read defect file exception: {}", fileName, e)
        }
        return null
    }

    private fun readCyCloneDxSbomFromStream(reader: JSONReader, fileName: String): CycloneDX? {
        try {
            val cycloneDX = CycloneDX()
            if (!reader.nextIfObjectStart()) {
                return null
            }
            while (!reader.nextIfObjectEnd()) {
                val key = reader.readFieldName()
                when (key) {
                    "bomFormat" -> cycloneDX.bomFormat = reader.readString()
                    "specVersion" -> cycloneDX.specVersion = reader.readString()
                    "serialNumber" -> cycloneDX.serialNumber = reader.readString()
                    "version" -> cycloneDX.version = reader.readInt32()
                    "metadata" -> cycloneDX.metadata = readObjectByStream(reader, CycloneDXMetadata::class.java)
                    "externalReferences" -> {
                        cycloneDX.externalReferences = readArrayByStream(reader, CycloneDXExtReference::class.java)
                    }

                    "dependencies" -> {
                        cycloneDX.dependencies = readArrayByStream(reader, CycloneDXDependency::class.java)
                    }

                    "vulnerabilities" -> {
                        cycloneDX.vulnerabilities = readArrayByStream(reader, CycloneDXVulnerability::class.java)
                    }

                    else -> reader.skipValue()
                }
            }
            return cycloneDX
        } catch (e: IOException) {
            logger.error("Read defect file exception: {}", fileName, e)
        }
        return null
    }

    private fun <T> readObjectByStream(reader: JSONReader, clazz: Class<T>): T =
        reader.read(clazz)

    private fun <T> readArrayByStream(reader: JSONReader, clazz: Class<T>): List<T> {
        val arrays = mutableListOf<T>()
        if (!reader.nextIfArrayStart()) {
            return arrays
        }
        while (!reader.nextIfArrayEnd()) {
            arrays.add(reader.read(clazz))
        }
        return arrays
    }

    private fun readStringArrayByStream(reader: JSONReader): List<String> {
        val arrays = mutableListOf<String>()
        if (!reader.nextIfArrayStart()) {
            return arrays
        }
        while (!reader.nextIfArrayEnd()) {
            arrays.add(reader.readString())
        }
        return arrays
    }
}
