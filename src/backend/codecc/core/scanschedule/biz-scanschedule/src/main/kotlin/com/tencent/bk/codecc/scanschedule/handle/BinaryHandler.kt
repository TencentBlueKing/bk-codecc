package com.tencent.bk.codecc.scanschedule.handle

import com.google.common.base.Strings
import com.tencent.bk.codecc.scanschedule.constants.ScanConstants
import com.tencent.bk.codecc.scanschedule.utils.EnvUtils
import com.tencent.devops.common.api.ToolMetaDetailVO
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Component
class BinaryHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(BinaryHandler::class.java)
    }

    /**
     * 生成工具二进制命令行
     * @param binaryVO
     * @param scanId
     * @param toolName
     * @return
     */
    fun generateCommand(binaryVO: ToolMetaDetailVO.Binary, scanId: String, toolName: String): String {
        val inputPath = Paths.get(ScanConstants.CONTENT_WORKSPACE_PATH)
            .resolve(scanId)
            .resolve(ScanConstants.SCAN_RESULT_PATH)
            .resolve("${toolName}${ScanConstants.SCAN_INPUT_SUFFIX}")
            .toFile().absolutePath
        val outputPath = Paths.get(ScanConstants.CONTENT_WORKSPACE_PATH)
            .resolve(scanId)
            .resolve(ScanConstants.SCAN_RESULT_PATH)
            .resolve("${toolName}${ScanConstants.SCAN_OUTPUT_SUFFIX}")
            .toFile().absolutePath
        return when (EnvUtils.getOS()) {
            OSType.LINUX -> binaryVO.linuxCommand
            OSType.WINDOWS -> binaryVO.winCommand
            OSType.MAC_OS -> binaryVO.macCommand
            else -> binaryVO.winCommand
        }.replace("{input.json}", inputPath).replace("{output.json}", outputPath)
    }


    /**
     * 执行工具二进制命令
     * @param command
     * @param toolName
     * @param toolVersion
     */
    fun execCommand(command: String, toolName: String, toolVersion: String) {
        logger.info("binary command = [$command]")
        val toolPath = Paths.get(ScanConstants.BINARY_TOOL_INSTALL_PATH)
            .resolve(toolName)
            .resolve(toolVersion)
            .toFile().absolutePath
        if (StringUtils.isNotEmpty(toolPath) && !Paths.get(toolPath).toFile().isDirectory) {
            throw CodeCCException(CommonMessageCode.INVALID_TOOL_NAME,
                "can not found out the tool install path $toolPath.")
        }
        try {
            val envP = arrayOf<String>(
                "PATH=" + System.getenv("PATH"),
            )
            var process = Runtime.getRuntime().exec(command, envP, File(toolPath))
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { `in` ->
                var line: String?
                while (`in`.readLine().also { line = it } != null) {
                    if (Strings.isNullOrEmpty(line)) {
                        continue
                    }
                    logger.info(line)
                }
            }
            process.destroy()
        } catch (e: IOException) {
            throw CodeCCException(CommonMessageCode.INVALID_TOOL_NAME,
                "command run failed: $command, ${e.message}")
        }
    }

    /**
     * 工具二进制包下载方法
     * @param binaryVO
     * @param toolName
     * @return
     */
    fun binaryDownload(binaryVO: ToolMetaDetailVO.Binary, toolName:String): String {
        //1.获取下载二进制连接
        val toolUrl = when (EnvUtils.getOS()) {
            OSType.LINUX -> binaryVO.linuxUrl
            OSType.WINDOWS -> binaryVO.winUrl
            OSType.MAC_OS -> binaryVO.macUrl
            else -> binaryVO.linuxUrl
        }
        logger.info("binary download path = [${toolUrl}]")
        if (StringUtils.isEmpty(toolUrl)) {
            throw CodeCCException(CommonMessageCode.INVALID_TOOL_NAME,
                "download fail! error message: $toolName binary download url is empty.")
        }

        //2.检查二进制路径：不存在则创建，存在则返回
        val toolPath = Paths.get(ScanConstants.BINARY_TOOL_INSTALL_PATH)
            .resolve(toolName)
        val binaryVersionPath = toolPath.resolve(binaryVO.binaryVersion).toFile()
        logger.info("binary install path = [${binaryVersionPath.absolutePath}]")
        if (binaryVersionPath.exists() && binaryVersionPath.listFiles()?.isNotEmpty() == true) {
            return binaryVersionPath.absolutePath
        }
        binaryVersionPath.mkdirs()

        //3.下载并解压二进制工具
        val zipFileName = toolUrl!!.substringAfterLast("/")
        val zipFilePath = toolPath.resolve(zipFileName).toString()
        downloadAndSaveFile(toolUrl, zipFilePath)
        logger.info("unzip tool path success = [${zipFilePath}]")
        com.tencent.devops.common.util.FileUtils.unzipFile(zipFilePath, binaryVersionPath.absolutePath)
        logger.info("chmod tool path success = [${binaryVersionPath.absolutePath}]")
        com.tencent.devops.common.util.FileUtils.chmodPath(
            binaryVersionPath.absolutePath, true, true, true)

        //4.安装完成后，删除二进制工具zip文件
        logger.info("delete zip file ${zipFilePath}")
        FileUtils.forceDeleteOnExit(Paths.get(zipFilePath).toFile())

        return binaryVersionPath.absolutePath
    }

    private fun downloadAndSaveFile(url: String, filePath: String) {
        try {
            URL(url).openStream().use { `in` ->
                Files.copy(`in`, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            throw CodeCCException(CommonMessageCode.INVALID_TOOL_NAME,
                "download fail! error message: $url download failed. ${e.message}")
        }
    }
}