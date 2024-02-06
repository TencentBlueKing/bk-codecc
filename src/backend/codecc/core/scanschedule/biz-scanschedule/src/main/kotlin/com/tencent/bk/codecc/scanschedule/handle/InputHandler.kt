package com.tencent.bk.codecc.scanschedule.handle

import com.tencent.bk.codecc.scanschedule.constants.ScanConstants
import com.tencent.bk.codecc.scanschedule.pojo.input.InputVO
import com.tencent.bk.codecc.scanschedule.pojo.input.OpenCheckers
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord
import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

@Component
class InputHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(InputHandler::class.java)
    }

    /**
     * 保存输入内容到文件
     * @param scanRecord
     * @param inputVO
     * @return
     */
    fun saveInputVoToFile(scanRecord: ScanRecord, inputVO: InputVO): String {
        val inputPath = Paths.get(ScanConstants.CONTENT_WORKSPACE_PATH)
            .resolve(scanRecord.scanId)
            .resolve(ScanConstants.SCAN_RESULT_PATH)
            .resolve("${inputVO.toolName}${ScanConstants.SCAN_INPUT_SUFFIX}")
            .toFile().absolutePath
        return if (FileUtils.saveContentToFile(inputPath, JsonUtil.toJson(inputVO))) {
            logger.info("scanId ${scanRecord.scanId} save ${inputVO.toolName} inputVO to file success: $inputPath")
            inputPath
        } else {
            logger.error("scanId ${scanRecord.scanId} save ${inputVO.toolName} inputVO to file failed: $inputPath")
            ""
        }
    }

    /**
     * 生成输入对象内容
     * @param scanRecord
     * @param toolName
     * @param toolOpenCheckers
     * @return
     */
    fun generateInputVo(scanRecord: ScanRecord, toolName: String, toolOpenCheckers: List<OpenCheckers>): InputVO {
        val projectPath = Paths.get(
            ScanConstants.CONTENT_WORKSPACE_PATH,
            scanRecord.scanId).toAbsolutePath().toString()
        val scanFilePath = Paths.get(projectPath,
            ScanConstants.SCAN_FILE_NAME).toAbsolutePath().toString()
        val inputVo = readInputVoFromFile(scanRecord, toolName)
        with(inputVo) {
            this.scanPath = projectPath
            this.incrementalFiles = arrayListOf(scanFilePath)
            this.openCheckers = toolOpenCheckers
            this.toolName = toolName
        }
        return inputVo
    }

    private fun readInputVoFromFile(scanRecord: ScanRecord, toolName: String): InputVO {
        val inputPath = Paths.get(ScanConstants.CONTENT_WORKSPACE_PATH)
            .resolve(scanRecord.scanId)
            .resolve(ScanConstants.SCAN_RESULT_PATH)
            .resolve("${toolName}${ScanConstants.SCAN_INPUT_SUFFIX}").toFile()
        if (inputPath.exists()) {
            try {
                val data = inputPath.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return JsonUtil.fromJson(data, InputVO::class.java)
            } catch (e: IOException) {
                throw CodeCCException("scanId ${scanRecord.scanId} get $toolName inputVO from file failed ${e.message}")
            }
        }
        return InputVO()
    }
}