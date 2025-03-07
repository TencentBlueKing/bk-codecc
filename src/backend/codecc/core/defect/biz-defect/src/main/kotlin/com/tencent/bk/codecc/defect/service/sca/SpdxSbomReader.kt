package com.tencent.bk.codecc.defect.service.sca

import com.alibaba.fastjson.JSONReader
import com.tencent.bk.codecc.defect.model.sca.sbom.SbomCreationInfo
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomFile
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomInfo
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomPackage
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomRelationship
import com.tencent.bk.codecc.defect.model.sca.sbom.spdx.SpdxSbomSnippet
import com.tencent.devops.common.codecc.util.JsonUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object SpdxSbomReader {

    private const val FILE_SIZE_THRESHOLD_FOR_STREAMING: Long = 1 * 1024 * 1024

    private val logger: Logger = LoggerFactory.getLogger(SpdxSbomReader::class.java)

    fun readFromJsonFile(jsonFilePath: String): SpdxSbomInfo? {
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
            JsonUtil.to(spdxSbomJsonStr, SpdxSbomInfo::class.java)
        } else {
            // 大于1M 使用JSONReader逐个元素读取
            readJsonFileByStream(jsonFile)
        }
    }

    private fun readJsonFileByStream(jsonFile: File): SpdxSbomInfo? {
        try {
            val spdxSbomInfo = SpdxSbomInfo()
            FileInputStream(jsonFile).use { fileInputStream ->
                InputStreamReader(fileInputStream, StandardCharsets.UTF_8).use { inputStreamReader ->
                    JSONReader(inputStreamReader).use { reader ->
                        reader.startObject()
                        while (reader.hasNext()) {
                            val key = reader.readString()
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

                                "incrementalFiles" -> {
                                    spdxSbomInfo.incrementalFiles = readStringArrayByStream(reader)
                                }

                                else -> reader.readObject()
                            }
                        }
                        reader.endObject()
                    }
                }
            }
            return spdxSbomInfo
        } catch (e: IOException) {
            logger.error("Read defect file exception: {}", jsonFile, e)
        }
        return null
    }

    private fun <T> readObjectByStream(reader: JSONReader, clazz: Class<T>): T =
        JsonUtil.to(reader.readString(), clazz)

    private fun <T> readArrayByStream(reader: JSONReader, clazz: Class<T>): List<T> {
        val arrays = mutableListOf<T>()
        reader.startArray()
        while (reader.hasNext()) {
            arrays.add(JsonUtil.to(reader.readString(), clazz))
        }
        reader.endArray()
        return arrays
    }

    private fun readStringArrayByStream(reader: JSONReader): List<String> {
        val arrays = mutableListOf<String>()
        reader.startArray()
        while (reader.hasNext()) {
            arrays.add(reader.readString())
        }
        reader.endArray()
        return arrays
    }
}
