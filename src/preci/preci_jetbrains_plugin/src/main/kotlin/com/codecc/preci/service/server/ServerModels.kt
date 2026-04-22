package com.codecc.preci.service.server

/**
 * PreCI Local Server 的运行状态枚举
 *
 * 表示 PreCI Local Server 在不同阶段的状态，用于内部状态管理和 UI 展示。
 *
 * @since 1.0
 */
enum class ServerState {
    /**
     * PreCI CLI 未安装
     *
     * 表示系统中未检测到 PreCI CLI 工具，无法执行任何 PreCI 相关操作。
     */
    NOT_INSTALLED,

    /**
     * 服务已停止
     *
     * PreCI CLI 已安装，但 Local Server 未运行。
     */
    STOPPED,

    /**
     * 服务启动中
     *
     * 正在执行 `preci server start` 命令，等待服务就绪。
     */
    STARTING,

    /**
     * 服务运行中
     *
     * Local Server 正在运行，可以接受 API 请求。
     */
    RUNNING,

    /**
     * 服务停止中
     *
     * 正在执行停止操作（调用 `/shutdown` 接口或执行 `preci server stop` 命令）。
     */
    STOPPING,

    /**
     * 异常状态
     *
     * 服务处于不可预期的错误状态，需要用户干预。
     */
    ERROR
}

/**
 * 服务启动结果的密封类
 *
 * 表示 PreCI Local Server 启动操作的结果，包含成功和失败两种情况。
 *
 * @since 1.0
 */
sealed class ServerStartResult {
    /**
     * 启动成功
     *
     * @property port Local Server 运行的端口号
     */
    data class Success(val port: Int) : ServerStartResult()

    /**
     * 启动失败
     *
     * @property message 失败原因描述
     */
    data class Failure(val message: String?) : ServerStartResult()
}

/**
 * 服务停止结果的密封类
 *
 * 表示 PreCI Local Server 停止操作的结果。
 *
 * @since 1.0
 */
sealed class ServerStopResult {
    /**
     * 停止成功
     */
    data object Success : ServerStopResult()

    /**
     * 停止失败
     *
     * @property message 失败原因描述
     */
    data class Failure(val message: String?) : ServerStopResult()
}

/**
 * 安装结果的密封类
 *
 * 表示 PreCI CLI 下载和安装操作的结果。
 *
 * @since 1.0
 */
sealed class InstallResult {
    /**
     * 下载解压成功，返回安装脚本路径供终端执行
     *
     * @property installScriptPath 安装脚本的绝对路径
     */
    data class Success(val installScriptPath: String) : InstallResult()

    /**
     * 安装超时
     *
     * 下载或安装过程超过预期时间。
     */
    data object Timeout : InstallResult()

    /**
     * 安装失败
     *
     * @property message 失败原因描述
     */
    data class Failure(val message: String?) : InstallResult()
}

/**
 * 服务状态变更监听器接口
 *
 * 用于监听 PreCI Local Server 状态变化，实现此接口的类可以接收状态变更通知。
 * 主要用于 UI 组件（如状态栏、工具窗口）实时更新显示。
 *
 * **使用示例：**
 * ```kotlin
 * val listener = object : ServerStateChangeListener {
 *     override fun onStateChanged(oldState: ServerState, newState: ServerState) {
 *         println("Server state changed from $oldState to $newState")
 *     }
 * }
 * serverManagementService.addStateChangeListener(listener)
 * ```
 *
 * @since 1.0
 */
interface ServerStateChangeListener {
    /**
     * 当服务状态发生变更时触发
     *
     * @param oldState 变更前的状态
     * @param newState 变更后的状态
     */
    fun onStateChanged(oldState: ServerState, newState: ServerState)
}

/**
 * PreCI 安装包信息
 *
 * 根据当前操作系统和 CPU 架构，确定应该下载的 PreCI 安装包。
 *
 * @property fileName 安装包文件名
 * @property displayName 用于 UI 展示的操作系统名称
 * @property downloadUrl 完整的下载 URL
 * @property isWindows 是否为 Windows 系统
 * @since 1.0
 */
data class PreCIPackageInfo(
    val fileName: String,
    val displayName: String,
    val downloadUrl: String,
    val isWindows: Boolean
) {
    companion object {
        private const val BASE_URL =
            "https://bkrepo.woa.com/generic/bkdevops/static/gw/resource/preci/v2/latest/"

        /**
         * 根据当前操作系统和架构自动检测对应的安装包信息
         *
         * @return 匹配当前系统的安装包信息
         * @throws IllegalStateException 如果操作系统不受支持
         */
        fun detect(): PreCIPackageInfo {
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()

            return when {
                osName.contains("win") -> PreCIPackageInfo(
                    "preci_win.zip", "Windows", "${BASE_URL}preci_win.zip", true
                )
                osName.contains("mac") || osName.contains("darwin") -> {
                    if (osArch == "aarch64" || osArch == "arm64") {
                        PreCIPackageInfo(
                            "preci_mac_arm64.zip",
                            "macOS (Apple Silicon)",
                            "${BASE_URL}preci_mac_arm64.zip",
                            false
                        )
                    } else {
                        PreCIPackageInfo(
                            "preci_mac_amd64.zip",
                            "macOS (Intel)",
                            "${BASE_URL}preci_mac_amd64.zip",
                            false
                        )
                    }
                }
                osName.contains("linux") -> PreCIPackageInfo(
                    "preci_linux.zip", "Linux", "${BASE_URL}preci_linux.zip", false
                )
                else -> throw IllegalStateException("不支持的操作系统: $osName")
            }
        }
    }
}

