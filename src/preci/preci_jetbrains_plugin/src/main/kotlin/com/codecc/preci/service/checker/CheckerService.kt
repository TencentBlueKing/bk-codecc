package com.codecc.preci.service.checker

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * 规则集服务接口
 *
 * 负责代码检查规则集的管理，包括规则集的查看、选择和配置持久化。
 * 本接口是 PreCI Local Server 规则集相关 API 的客户端封装。
 *
 * **核心功能：**
 *
 * 1. **规则集列表查询**
 *    - 获取所有可用的规则集
 *    - 展示规则集 ID、名称和对应的检查工具
 *
 * 2. **规则集选择**
 *    - 选择单个或多个规则集
 *    - 将选择应用到项目的代码扫描配置
 *
 * 3. **配置持久化**
 *    - 保存用户的规则集选择偏好
 *    - 加载上次保存的规则集配置
 *
 * **使用示例：**
 * ```kotlin
 * val checkerService = CheckerService.getInstance(project)
 *
 * // 获取规则集列表
 * when (val result = checkerService.getCheckerSetList()) {
 *     is CheckerSetListResult.Success -> {
 *         result.checkerSets.forEach { checkerSet ->
 *             println("规则集: ${checkerSet.name} (${checkerSet.toolName})")
 *         }
 *     }
 *     is CheckerSetListResult.Failure -> {
 *         println("获取规则集失败: ${result.message}")
 *     }
 * }
 *
 * // 选择规则集
 * when (val result = checkerService.selectCheckerSets(listOf("golang_standard", "security_basic"))) {
 *     is CheckerSetSelectResult.Success -> {
 *         println("成功选择 ${result.selectedSets.size} 个规则集")
 *     }
 *     is CheckerSetSelectResult.Failure -> {
 *         println("选择规则集失败: ${result.message}")
 *     }
 * }
 *
 * // 保存配置
 * checkerService.saveSelectedCheckerSets(listOf("golang_standard", "security_basic"))
 *
 * // 加载配置
 * val savedSets = checkerService.loadSelectedCheckerSets()
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 * - 可以在任意协程作用域中调用
 *
 * @since 1.0
 */
@Service(Service.Level.PROJECT)
interface CheckerService {

    /**
     * 获取规则集列表
     *
     * 调用 PreCI Local Server 的 `GET /checker/set/list` 接口获取所有可用的规则集。
     *
     * **查询流程：**
     * 1. 调用 `/checker/set/list` 接口
     * 2. Local Server 返回所有可用的规则集列表
     * 3. 每个规则集包含：ID、名称、对应的检查工具
     * 4. 返回查询结果
     *
     * **返回数据说明：**
     * - `checkerSetId`：规则集唯一标识 ID（如 `golang_standard`）
     * - `checkerSetName`：规则集显示名称（如 `Go 标准规则集`）
     * - `toolName`：规则集对应的检查工具（如 `golangci-lint`）
     *
     * **注意事项：**
     * - Local Server 需要处于运行状态
     * - 规则集列表可能为空（如果没有配置任何规则集）
     *
     * @return 规则集列表查询结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun getCheckerSetList(): CheckerSetListResult

    /**
     * 获取规则集列表（同步阻塞版本）
     *
     * 与 [getCheckerSetList] 功能相同，但使用同步阻塞方式调用。
     * 适用于在 IntelliJ Platform 的后台任务 (Task.Backgroundable) 中使用，
     * 避免在后台线程中使用协程导致的潜在问题。
     *
     * **使用场景：**
     * - 在 `Task.Backgroundable.run()` 方法中调用
     * - 在非协程环境中需要获取规则集列表时
     *
     * @return 规则集列表查询结果，包含成功或失败信息
     *
     * @since 1.0
     */
    fun getCheckerSetListBlocking(): CheckerSetListResult

    /**
     * 选择规则集
     *
     * 调用 PreCI Local Server 的 `POST /checker/set/select` 接口选择要使用的规则集。
     * 选择的规则集将应用到后续的代码扫描中。
     *
     * **选择流程：**
     * 1. 获取当前项目的根目录路径（Project.basePath）
     * 2. 调用 `/checker/set/select` 接口，传递项目根目录和规则集 ID 列表
     * 3. Local Server 保存选择的规则集配置
     * 4. 返回选择结果，包含项目根目录和成功选择的规则集列表
     *
     * **注意事项：**
     * - 支持选择多个规则集
     * - 如果传入空列表，将清除所有已选择的规则集（使用默认规则集）
     * - Local Server 需要处于运行状态
     * - 项目需要先完成初始化（调用 `initProject`）
     *
     * @param checkerSetIds 要选择的规则集 ID 列表，不能为 null（但可以为空列表）
     * @param projectRootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 规则集选择结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun selectCheckerSets(
        checkerSetIds: List<String>,
        projectRootDir: String? = null
    ): CheckerSetSelectResult

    /**
     * 选择规则集（同步阻塞版本）
     *
     * 与 [selectCheckerSets] 功能相同，但使用同步阻塞方式调用。
     * 适用于在 IntelliJ Platform 的后台任务 (Task.Backgroundable) 中使用，
     * 避免在后台线程中使用协程导致的潜在问题。
     *
     * **使用场景：**
     * - 在 `Task.Backgroundable.run()` 方法中调用
     * - 在非协程环境中需要选择规则集时
     *
     * @param checkerSetIds 要选择的规则集 ID 列表，不能为 null（但可以为空列表）
     * @param projectRootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 规则集选择结果，包含成功或失败信息
     *
     * @since 1.0
     */
    fun selectCheckerSetsBlocking(
        checkerSetIds: List<String>,
        projectRootDir: String? = null
    ): CheckerSetSelectResult

    /**
     * 取消选择规则集
     *
     * 调用 PreCI Local Server 的 `POST /checker/set/unselect` 接口取消已选择的规则集。
     * 服务端会删除对应的规则集配置文件。
     *
     * @param checkerSetIds 要取消选择的规则集 ID 列表
     * @param projectRootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 规则集取消选择结果
     *
     * @since 1.0
     */
    suspend fun unselectCheckerSets(
        checkerSetIds: List<String>,
        projectRootDir: String? = null
    ): CheckerSetSelectResult

    /**
     * 取消选择规则集（同步阻塞版本）
     *
     * 与 [unselectCheckerSets] 功能相同，但使用同步阻塞方式调用。
     *
     * @param checkerSetIds 要取消选择的规则集 ID 列表
     * @param projectRootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 规则集取消选择结果
     *
     * @since 1.0
     */
    fun unselectCheckerSetsBlocking(
        checkerSetIds: List<String>,
        projectRootDir: String? = null
    ): CheckerSetSelectResult

    /**
     * 保存规则集选择配置
     *
     * 将用户选择的规则集 ID 列表保存到本地配置中。
     * 下次启动 IDE 时，可以通过 [loadSelectedCheckerSets] 加载保存的配置。
     *
     * **持久化机制：**
     * - 使用 IntelliJ Platform 的 `PersistentStateComponent` 机制
     * - 配置保存在 IDE 的配置目录中
     * - 配置在 IDE 重启后仍然有效
     *
     * **注意事项：**
     * - 此方法只保存配置到本地，不会调用 Local Server
     * - 要使选择生效，需要调用 [selectCheckerSets] 通知 Local Server
     *
     * @param checkerSetIds 要保存的规则集 ID 列表
     * @return 持久化结果，包含成功或失败信息
     *
     * @since 1.0
     */
    fun saveSelectedCheckerSets(checkerSetIds: List<String>): CheckerSetPersistResult

    /**
     * 加载已保存的规则集配置
     *
     * 从本地配置中加载上次保存的规则集 ID 列表。
     * 如果没有保存的配置，返回空列表。
     *
     * **使用场景：**
     * - IDE 启动时自动加载上次的规则集选择
     * - 打开规则集选择对话框时显示已选择的规则集
     *
     * @return 已保存的规则集 ID 列表，如果没有保存的配置则返回空列表
     *
     * @since 1.0
     */
    fun loadSelectedCheckerSets(): List<String>

    /**
     * 清除已保存的规则集配置
     *
     * 从本地配置中清除已保存的规则集选择。
     * 清除后，[loadSelectedCheckerSets] 将返回空列表。
     *
     * @return 清除结果，包含成功或失败信息
     *
     * @since 1.0
     */
    fun clearSelectedCheckerSets(): CheckerSetPersistResult

    /**
     * 同步规则集配置到 Local Server
     *
     * 加载本地保存的规则集配置，并调用 [selectCheckerSets] 同步到 Local Server。
     * 这是一个便捷方法，用于在 IDE 启动或项目打开时自动应用保存的规则集配置。
     *
     * **同步流程：**
     * 1. 调用 [loadSelectedCheckerSets] 加载本地配置
     * 2. 如果有保存的配置，调用 [selectCheckerSets] 同步到 Local Server
     * 3. 如果没有保存的配置，不执行任何操作
     *
     * **注意事项：**
     * - 仅当启用了"记住规则集选择"选项时才会执行同步
     * - Local Server 需要处于运行状态
     *
     * @return 同步结果，如果没有保存的配置则返回 null
     *
     * @since 1.0
     */
    suspend fun syncCheckerSetsToServer(): CheckerSetSelectResult?

    /**
     * 检查是否启用了"记住规则集选择"功能
     *
     * @return 如果启用了记住功能返回 true，否则返回 false
     *
     * @since 1.0
     */
    fun isRememberCheckerSetsEnabled(): Boolean

    /**
     * 设置"记住规则集选择"功能的状态
     *
     * @param enabled 是否启用记住功能
     *
     * @since 1.0
     */
    fun setRememberCheckerSetsEnabled(enabled: Boolean)

    companion object {
        /**
         * 获取 CheckerService 实例
         *
         * 从 IntelliJ Platform 的服务容器中获取项目级的 CheckerService 实例。
         *
         * @param project 当前项目
         * @return CheckerService 实例
         *
         * @since 1.0
         */
        @JvmStatic
        fun getInstance(project: Project): CheckerService {
            return project.getService(CheckerService::class.java)
        }
    }
}
