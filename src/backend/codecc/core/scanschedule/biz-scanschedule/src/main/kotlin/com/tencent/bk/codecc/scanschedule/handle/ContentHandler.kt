package com.tencent.bk.codecc.scanschedule.handle

import com.tencent.bk.codecc.scanschedule.constants.ScanConstants
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Component
class ContentHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(ContentHandler::class.java)
    }

    /**
     * 保存文本内容到文件
     * @param scanRecord
     * @return
     */
    fun saveContentToFile(scanRecord: ScanRecord): String {
        val scanFilePath = Paths.get(
            ScanConstants.CONTENT_WORKSPACE_PATH,
            scanRecord.scanId,
            ScanConstants.SCAN_FILE_NAME)
            .toFile().absolutePath
        return if (FileUtils.saveContentToFile(scanFilePath, scanRecord.content)) {
            logger.info("scanId ${scanRecord.scanId} save content to file success = [$scanFilePath]")
            scanFilePath
        } else {
            logger.error("scanId ${scanRecord.scanId} save content to file failed = [$scanFilePath]")
            ""
        }
    }

    /**
     * 删除文本内容文件
     * @param scanRecord
     * @return
     */
    fun removeContentFile(scanRecord: ScanRecord): Boolean {
        val scanFilePath = Paths.get(
            ScanConstants.CONTENT_WORKSPACE_PATH,
            scanRecord.scanId,
            ScanConstants.SCAN_FILE_NAME)
            .toFile().absolutePath
        return try {
            Files.deleteIfExists(Paths.get(scanFilePath))
        } catch (e: IOException) {
            throw CodeCCException("scanFile $scanFilePath delete failed: ${e.message}")
        }
    }
}