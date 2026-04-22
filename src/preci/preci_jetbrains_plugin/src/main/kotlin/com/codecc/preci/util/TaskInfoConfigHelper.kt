package com.codecc.preci.util

import com.codecc.preci.core.log.PreCILogger
import kotlinx.serialization.json.*
import java.io.File

/**
 * taskInfo.json 读写工具类
 *
 * 负责读取和更新项目目录下 `.codecc/taskInfo.json` 中的 WhitePaths 和 BlackPaths 字段。
 *
 * @since 1.0
 */
object TaskInfoConfigHelper {

    private val logger = PreCILogger.getLogger(TaskInfoConfigHelper::class.java)
    private const val TASK_INFO_PATH = ".codecc/taskInfo.json"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    data class PathFilters(
        val whitePaths: List<String> = emptyList(),
        val blackPaths: List<String> = emptyList()
    )

    /**
     * 读取 taskInfo.json 中的 WhitePaths 和 BlackPaths
     *
     * @param projectRootPath 项目根目录路径
     * @return PathFilters 包含白名单和黑名单路径列表
     */
    fun readPathFilters(projectRootPath: String): PathFilters {
        return try {
            val taskInfoFile = File(projectRootPath, TASK_INFO_PATH)
            if (!taskInfoFile.exists()) {
                logger.info("taskInfo.json 不存在: ${taskInfoFile.absolutePath}")
                return PathFilters()
            }

            val content = taskInfoFile.readText()
            val jsonElement = json.parseToJsonElement(content)
            val jsonObject = jsonElement.jsonObject

            val whitePaths = jsonObject["WhitePaths"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList()

            val blackPaths = jsonObject["BlackPaths"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList()

            logger.info("成功读取 taskInfo.json: WhitePaths=$whitePaths, BlackPaths=$blackPaths")
            PathFilters(whitePaths, blackPaths)
        } catch (e: Exception) {
            logger.error("读取 taskInfo.json 失败: ${e.message}", e)
            PathFilters()
        }
    }

    /**
     * 更新 taskInfo.json 中的 WhitePaths 和 BlackPaths
     *
     * 保留文件中的其他字段不变，仅修改这两个字段。
     *
     * @param projectRootPath 项目根目录路径
     * @param whitePaths 白名单路径列表
     * @param blackPaths 黑名单路径列表
     * @return true 写入成功，false 写入失败
     */
    fun writePathFilters(projectRootPath: String, whitePaths: List<String>, blackPaths: List<String>): Boolean {
        return try {
            val taskInfoFile = File(projectRootPath, TASK_INFO_PATH)
            if (!taskInfoFile.exists()) {
                logger.warn("taskInfo.json 不存在，无法更新: ${taskInfoFile.absolutePath}")
                return false
            }

            val content = taskInfoFile.readText()
            val originalObject = json.parseToJsonElement(content).jsonObject

            val updatedMap = buildJsonObject {
                originalObject.forEach { (key, value) ->
                    when (key) {
                        "WhitePaths" -> put(key, JsonArray(whitePaths.map { JsonPrimitive(it) }))
                        "BlackPaths" -> put(key, JsonArray(blackPaths.map { JsonPrimitive(it) }))
                        else -> put(key, value)
                    }
                }
                if ("WhitePaths" !in originalObject) {
                    put("WhitePaths", JsonArray(whitePaths.map { JsonPrimitive(it) }))
                }
                if ("BlackPaths" !in originalObject) {
                    put("BlackPaths", JsonArray(blackPaths.map { JsonPrimitive(it) }))
                }
            }

            taskInfoFile.writeText(json.encodeToString(JsonObject.serializer(), updatedMap))
            logger.info("成功更新 taskInfo.json: WhitePaths=$whitePaths, BlackPaths=$blackPaths")
            true
        } catch (e: Exception) {
            logger.error("更新 taskInfo.json 失败: ${e.message}", e)
            false
        }
    }

    /**
     * 将逗号分隔的字符串转为路径列表（过滤空白项）
     */
    fun parseCommaSeparated(text: String): List<String> {
        return text.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 将路径列表转为逗号分隔的字符串
     */
    fun joinToCommaSeparated(paths: List<String>): String {
        return paths.joinToString(",")
    }
}
