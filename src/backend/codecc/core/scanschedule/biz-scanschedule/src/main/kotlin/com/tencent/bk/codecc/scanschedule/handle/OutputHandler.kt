package com.tencent.bk.codecc.scanschedule.handle

import com.tencent.bk.codecc.scanschedule.constants.ScanConstants
import com.tencent.bk.codecc.scanschedule.pojo.output.OutputVO
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO
import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.api.exception.CodeCCException
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.streams.toList

@Component
class OutputHandler {

    /**
     * 解释输出内容信息为告警列表
     * @param scanRecord
     * @param toolName
     * @param outputVo
     * @return
     */
    fun explainOutputVoToDefectList(scanRecord: ScanRecord, toolName: String, outputVo: OutputVO):
            List<SimpleDefectVO> {
        return outputVo.defects.stream()
            .map { defect ->
                defect.apply {
                    scanId = scanRecord.scanId
                    author = scanRecord.userName
                    this.toolName = toolName
                    createTime = System.currentTimeMillis()
                }
            }
            .toList()
    }

    /**
     * 读取输出对象
     * @param scanRecord
     * @param toolName
     * @return
     */
    fun readOutputVoFromFile(scanRecord: ScanRecord, toolName: String): OutputVO {
        val outputPath = Paths.get(ScanConstants.CONTENT_WORKSPACE_PATH)
            .resolve(scanRecord.scanId)
            .resolve(ScanConstants.SCAN_RESULT_PATH)
            .resolve("${toolName}${ScanConstants.SCAN_OUTPUT_SUFFIX}").toFile()
        if (outputPath.exists()) {
            try {
                val data = outputPath.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return JsonUtil.fromJson(data, OutputVO::class.java)
            } catch (e: IOException) {
                throw CodeCCException("scanId ${scanRecord.scanId} read output from $toolName file failed ${e.message}")
            }
        }
        return OutputVO()
    }
}