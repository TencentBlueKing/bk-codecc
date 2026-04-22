package com.codecc.preci.service.scan

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * 扫描服务接口
 *
 * 负责代码扫描任务的全生命周期管理，包括项目初始化、扫描执行、进度监控和结果展示。
 * 本接口是 PreCI Local Server 扫描相关 API 的客户端封装。
 *
 * **核心功能：**
 *
 * 1. **项目初始化**
 *    - 初始化项目配置，准备代码扫描所需的配置和环境
 *    - 自动检测项目根目录和语言类型
 *
 * 2. **代码扫描**（待实现）
 *    - 全量扫描：扫描整个项目的所有代码文件
 *    - 目标扫描：扫描用户选定的文件或目录
 *    - 增量扫描：支持 pre-commit 和 pre-push 模式的增量扫描
 *
 * 3. **扫描进度监控**（待实现）
 *    - 实时显示扫描进度
 *    - 支持取消正在进行的扫描任务
 *
 * 4. **扫描结果管理**（待实现）
 *    - 获取扫描结果（代码缺陷列表）
 *    - 结果过滤和导航
 *
 * **使用示例：**
 * ```kotlin
 * val scanService = ScanService.getInstance(project)
 *
 * // 项目初始化
 * val result = scanService.initProject()
 * when (result) {
 *     is InitResult.Success -> println("初始化成功: ${result.response.rootPath}")
 *     is InitResult.Failure -> println("初始化失败: ${result.message}")
 * }
 * ```
 *
 * **线程安全性：**
 * - 所有公共方法都是挂起函数，支持协程
 * - 可以在任意协程作用域中调用
 *
 * @since 1.0
 */
@Service(Service.Level.PROJECT)
interface ScanService {

    /**
     * 初始化项目
     *
     * 分两阶段完成初始化：
     * 1. 调用 `POST /task/init` 获取项目配置和工具列表
     * 2. 对返回的每个工具，调用 `POST /task/reload/tool/{toolName}` 逐个下载
     *
     * **初始化流程：**
     * 1. 获取当前项目的根目录路径（Project.basePath）
     * 2. 调用 `/task/init` 接口，传递 currentPath 和 rootPath，获得工具列表
     * 3. 遍历工具列表，逐个调用 `/task/reload/tool/{toolName}` 下载工具
     * 4. 通过 [onProgress] 回调实时报告进度
     *
     * **注意事项：**
     * - 如果项目已初始化，再次调用会更新配置
     * - Local Server 需要处于运行状态
     * - 工具下载过程中某个工具失败不会中断整体流程，会继续下载剩余工具
     *
     * @param rootPath 指定的项目根目录，如果为 null 则自动使用 Project.basePath
     * @param onProgress 进度回调，在每个阶段切换时被调用，可用于更新 UI 进度条
     * @return 初始化结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun initProject(rootPath: String? = null, onProgress: ((InitProgress) -> Unit)? = null): InitResult

    /**
     * 执行全量扫描
     *
     * 调用 PreCI Local Server 的 `POST /task/scan` 接口执行全量扫描（scanType=0）。
     * 全量扫描会扫描整个项目的所有代码文件。
     *
     * **扫描流程：**
     * 1. 获取当前项目的根目录路径（Project.basePath）
     * 2. 调用 `/task/scan` 接口，传递 scanType=0（全量扫描）
     * 3. Local Server 启动扫描任务并返回扫描信息
     * 4. 返回扫描启动结果（包含使用的工具列表、扫描文件数等）
     *
     * **注意事项：**
     * - 项目需要先完成初始化（调用 initProject）
     * - Local Server 需要处于运行状态
     * - 如果已有扫描任务正在进行，Server 会返回错误
     *
     * @param rootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 扫描启动结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun fullScan(rootDir: String? = null): ScanResult

    /**
     * 执行目标扫描
     *
     * 调用 PreCI Local Server 的 `POST /task/scan` 接口执行目标扫描（scanType=100）。
     * 目标扫描只扫描用户指定的文件或目录。
     *
     * **扫描流程：**
     * 1. 验证 paths 参数非空
     * 2. 获取当前项目的根目录路径
     * 3. 调用 `/task/scan` 接口，传递 scanType=100 和 paths 列表
     * 4. Local Server 启动扫描任务并返回扫描信息
     * 5. 返回扫描启动结果
     *
     * **注意事项：**
     * - paths 必须为绝对路径
     * - 项目需要先完成初始化（调用 initProject）
     * - Local Server 需要处于运行状态
     * - 如果已有扫描任务正在进行，Server 会返回错误
     *
     * @param paths 要扫描的文件或目录的绝对路径列表，不能为空
     * @param rootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 扫描启动结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun targetScan(paths: List<String>, rootDir: String? = null): ScanResult

    /**
     * 执行增量扫描（pre-commit）
     *
     * 调用 PreCI Local Server 的 `POST /task/scan` 接口执行 pre-commit 增量扫描（scanType=102）。
     * pre-commit 扫描只扫描 Git 暂存区中的变更文件。
     *
     * **扫描流程：**
     * 1. 获取当前项目的根目录路径
     * 2. 调用 `/task/scan` 接口，传递 scanType=102（pre-commit 扫描）
     * 3. Local Server 会自动通过 Git 获取暂存区的变更文件
     * 4. 启动扫描任务并返回扫描信息
     * 5. 返回扫描启动结果
     *
     * **注意事项：**
     * - 项目需要是 Git 仓库
     * - 项目需要先完成初始化（调用 initProject）
     * - Local Server 需要处于运行状态
     * - 如果暂存区没有变更文件，Server 会返回错误
     * - 如果已有扫描任务正在进行，Server 会返回错误
     *
     * @param rootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 扫描启动结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun preCommitScan(rootDir: String? = null): ScanResult

    /**
     * 执行增量扫描（pre-push）
     *
     * 调用 PreCI Local Server 的 `POST /task/scan` 接口执行 pre-push 增量扫描（scanType=103）。
     * pre-push 扫描只扫描本地已提交但未推送到远程的变更文件。
     *
     * **扫描流程：**
     * 1. 获取当前项目的根目录路径
     * 2. 调用 `/task/scan` 接口，传递 scanType=103（pre-push 扫描）
     * 3. Local Server 会自动通过 Git 获取未推送的变更文件
     * 4. 启动扫描任务并返回扫描信息
     * 5. 返回扫描启动结果
     *
     * **注意事项：**
     * - 项目需要是 Git 仓库
     * - 项目需要先完成初始化（调用 initProject）
     * - Local Server 需要处于运行状态
     * - 如果没有未推送的变更文件，Server 会返回错误
     * - 如果已有扫描任务正在进行，Server 会返回错误
     *
     * @param rootDir 项目根目录，如果为 null 则自动使用 Project.basePath
     * @return 扫描启动结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun prePushScan(rootDir: String? = null): ScanResult

    /**
     * 查询扫描进度
     *
     * 调用 PreCI Local Server 的 `GET /task/scan/progress` 接口查询当前扫描任务的进度状态。
     * 可用于实时监控扫描进度，了解各检查工具的执行情况。
     *
     * **查询流程：**
     * 1. 调用 `/task/scan/progress` 接口
     * 2. Local Server 返回当前扫描任务的状态信息
     * 3. 包含项目根目录、各工具状态映射和整体状态
     * 4. 返回查询结果
     *
     * **状态说明：**
     * - `running`：扫描进行中
     * - `done`：扫描已完成
     * - 空字符串：无扫描任务
     *
     * **注意事项：**
     * - Local Server 需要处于运行状态
     * - 如果没有正在进行的扫描任务，返回的状态为空字符串
     * - toolStatuses 映射中的每个工具也有自己的状态（running/done）
     *
     * @return 扫描进度查询结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun getScanProgress(): ScanProgressResult

    /**
     * 查询扫描结果
     *
     * 调用 PreCI Local Server 的 `POST /task/scan/result` 接口查询扫描结果（代码缺陷列表）。
     * 返回指定路径前缀下的所有代码缺陷信息。
     *
     * **查询流程：**
     * 1. 获取当前项目的根目录路径（Project.basePath）
     * 2. 调用 `/task/scan/result` 接口，传递 path 参数
     * 3. Local Server 返回匹配路径前缀的所有缺陷列表
     * 4. 每个缺陷包含：工具名、规则名、描述、文件路径、行号等信息
     * 5. 返回查询结果
     *
     * **缺陷信息字段：**
     * - `toolName`：检查工具名称（如 golangci-lint）
     * - `checkerName`：检查规则名称（如 errcheck）
     * - `description`：问题描述信息
     * - `filePath`：问题所在文件的完整路径
     * - `line`：问题所在行号
     *
     * **注意事项：**
     * - 项目需要先完成扫描
     * - Local Server 需要处于运行状态
     * - path 参数通常传递项目根目录，用于过滤结果
     * - 如果扫描尚未完成，返回的缺陷列表可能不完整
     *
     * @param path 查询的路径前缀，如果为 null 则自动使用 Project.basePath
     * @return 扫描结果查询结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun getScanResult(path: String? = null): ScanResultQueryResult

    /**
     * 取消扫描
     *
     * 调用 PreCI Local Server 的 `GET /task/scan/cancel` 接口取消当前正在进行的扫描任务。
     * 适用于用户需要中断长时间运行的扫描任务的场景。
     *
     * **取消流程：**
     * 1. 调用 `/task/scan/cancel` 接口
     * 2. Local Server 停止当前扫描任务
     * 3. 返回被取消扫描任务的项目根目录
     * 4. 返回取消结果
     *
     * **注意事项：**
     * - Local Server 需要处于运行状态
     * - 如果没有正在进行的扫描任务，Server 会返回错误
     * - 取消后，部分工具可能已经完成扫描，这些工具的结果仍然可用
     * - 取消操作是异步的，实际停止可能需要几秒钟时间
     *
     * @return 取消扫描结果，包含成功或失败信息
     *
     * @since 1.0
     */
    suspend fun cancelScan(): CancelScanResult

    companion object {
        /**
         * 获取 ScanService 实例
         *
         * 从 IntelliJ Platform 的服务容器中获取项目级的 ScanService 实例。
         *
         * @param project 当前项目
         * @return ScanService 实例
         *
         * @since 1.0
         */
        @JvmStatic
        fun getInstance(project: Project): ScanService {
            return project.getService(ScanService::class.java)
        }
    }
}

