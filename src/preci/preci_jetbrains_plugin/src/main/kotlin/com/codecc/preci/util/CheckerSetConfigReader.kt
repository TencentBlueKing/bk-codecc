package com.codecc.preci.util

import com.codecc.preci.core.log.PreCILogger
import java.io.File

/**
 * 规则集配置读取工具类
 *
 * 负责从项目目录的 `.codecc/checkerset/` 文件夹中读取已选择的规则集配置。
 * PreCI Local Server 在选择规则集后，会将规则集配置以 JSON 文件形式保存在该目录下。
 *
 * **配置路径：** `项目根目录/.codecc/checkerset/`
 *
 * **文件命名规则：** 每个规则集对应一个 JSON 文件，文件名为规则集 ID，后缀为 `.json`
 * - 例如：`golang_standard.json`、`security_basic.json`
 *
 * **读取逻辑：**
 * 1. 检查 `.codecc/checkerset/` 目录是否存在
 * 2. 读取目录下所有 `.json` 文件
 * 3. 提取文件名（去除 `.json` 后缀）作为规则集 ID
 * 4. 返回规则集 ID 列表
 *
 * **使用场景：**
 * - 在设置页面初始化时，读取当前项目已选择的规则集
 * - 在扫描前，检查是否已配置规则集
 *
 * **使用示例：**
 * ```kotlin
 * val projectRoot = "/path/to/project"
 * val selectedSets = CheckerSetConfigReader.getSelectedCheckerSets(projectRoot)
 * // selectedSets: ["golang_standard", "security_basic"]
 * ```
 *
 * @since 1.0
 */
object CheckerSetConfigReader {

    private val logger = PreCILogger.getLogger(CheckerSetConfigReader::class.java)

    /**
     * 获取项目已选择的规则集 ID 列表
     *
     * 从项目根目录的 `.codecc/checkerset/` 文件夹中读取所有 `.json` 文件，
     * 提取文件名（忽略 `.json` 后缀）作为规则集 ID。
     *
     * **读取流程：**
     * 1. 检查项目根目录是否存在
     * 2. 检查 `.codecc/checkerset/` 目录是否存在
     * 3. 读取目录下所有 `.json` 文件
     * 4. 提取文件名（去除 `.json` 后缀）
     * 5. 返回规则集 ID 列表
     *
     * **注意事项：**
     * - 如果项目根目录不存在，返回空列表
     * - 如果 `.codecc/checkerset/` 目录不存在，返回空列表
     * - 如果目录为空或没有 `.json` 文件，返回空列表
     * - 忽略子目录和非 `.json` 文件
     *
     * @param projectRootPath 项目根目录路径
     * @return 规则集 ID 列表，如果没有配置或读取失败则返回空列表
     *
     * @since 1.0
     */
    fun getSelectedCheckerSets(projectRootPath: String): List<String> {
        return try {
            // 1. 检查项目根目录是否存在
            val projectRoot = File(projectRootPath)
            if (!projectRoot.exists() || !projectRoot.isDirectory) {
                logger.warn("项目根目录不存在或不是目录: $projectRootPath")
                return emptyList()
            }

            // 2. 检查 .codecc/checkerset/ 目录是否存在
            val checkerSetDir = File(projectRoot, ".codecc/checkerset")
            if (!checkerSetDir.exists() || !checkerSetDir.isDirectory) {
                logger.info("规则集配置目录不存在: ${checkerSetDir.absolutePath}")
                return emptyList()
            }

            // 3. 读取目录下所有 .json 文件
            val jsonFiles = checkerSetDir.listFiles { file ->
                file.isFile && file.extension == "json"
            }

            if (jsonFiles == null || jsonFiles.isEmpty()) {
                logger.info("规则集配置目录为空: ${checkerSetDir.absolutePath}")
                return emptyList()
            }

            // 4. 提取文件名（去除 .json 后缀）
            val checkerSetIds = jsonFiles.map { file ->
                file.nameWithoutExtension
            }.sorted() // 排序以保证顺序一致性

            logger.info("成功读取规则集配置，共 ${checkerSetIds.size} 个: $checkerSetIds")
            checkerSetIds

        } catch (e: SecurityException) {
            logger.error("读取规则集配置失败 (权限不足): ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            logger.error("读取规则集配置失败 (未知异常): ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 检查项目是否已配置规则集
     *
     * 通过检查 `.codecc/checkerset/` 目录是否存在且包含 `.json` 文件来判断。
     *
     * @param projectRootPath 项目根目录路径
     * @return true 如果已配置规则集，false 否则
     *
     * @since 1.0
     */
    fun hasCheckerSetConfig(projectRootPath: String): Boolean {
        return getSelectedCheckerSets(projectRootPath).isNotEmpty()
    }

    /**
     * 获取规则集配置目录路径
     *
     * 返回 `.codecc/checkerset/` 目录的绝对路径。
     *
     * @param projectRootPath 项目根目录路径
     * @return 规则集配置目录的绝对路径
     *
     * @since 1.0
     */
    fun getCheckerSetConfigDir(projectRootPath: String): String {
        return File(projectRootPath, ".codecc/checkerset").absolutePath
    }
}

