package com.codecc.preci.core.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * PreCI 配置管理服务
 *
 * 负责管理 PreCI 插件的所有用户配置，包括配置的持久化、读取和修改。
 * 该服务使用 IntelliJ Platform 的 [PersistentStateComponent] 机制，
 * 配置会自动保存到 IDE 的配置目录（preci-settings.xml）。
 *
 * **使用方式：**
 * ```kotlin
 * val settings = PreCISettings.getInstance()
 * settings.serverAutoStart = true
 * ```
 *
 * **配置范围：** Application 级别（全局配置，所有项目共享）
 *
 * @since 1.0
 */
@Service(Service.Level.APP)
@State(
    name = "PreCISettings",
    storages = [Storage("preci-settings.xml")]
)
class PreCISettings : PersistentStateComponent<PreCISettingsState> {

    /**
     * 内部配置状态
     */
    private var state = PreCISettingsState()

    /**
     * 获取当前配置状态
     *
     * 此方法由 IntelliJ Platform 调用，用于序列化配置到 XML 文件
     *
     * @return 当前配置状态
     */
    override fun getState(): PreCISettingsState = state

    /**
     * 加载配置状态
     *
     * 此方法由 IntelliJ Platform 调用，用于从 XML 文件反序列化配置
     *
     * @param state 从文件加载的配置状态
     */
    override fun loadState(state: PreCISettingsState) {
        this.state = state
    }

    companion object {
        /**
         * 获取 PreCISettings 服务实例
         *
         * @return PreCISettings 单例
         */
        fun getInstance(): PreCISettings {
            return ApplicationManager.getApplication().getService(PreCISettings::class.java)
        }
    }

    // ========== PreCI CLI 路径配置访问器 ==========

    /**
     * PreCI CLI 可执行文件路径（用户手动指定或自动检测后持久化）
     */
    var preciPath: String
        get() = state.preciPath
        set(value) {
            state.preciPath = value
        }

    // ========== Local Server 相关配置访问器 ==========

    /**
     * 是否自动启动 Local Server
     */
    var serverAutoStart: Boolean
        get() = state.serverAutoStart
        set(value) {
            state.serverAutoStart = value
        }

    /**
     * Server 启动超时时间（秒）
     */
    var serverStartTimeout: Int
        get() = state.serverStartTimeout
        set(value) {
            state.serverStartTimeout = value
        }

    // ========== 鉴权相关配置访问器 ==========

    /**
     * 当前选择的蓝盾项目 ID
     */
    var currentProjectId: String
        get() = state.currentProjectId
        set(value) {
            state.currentProjectId = value
        }

    // ========== 扫描相关配置访问器 ==========

    /**
     * 保存文件时自动扫描
     */
    var scanOnSave: Boolean
        get() = state.scanOnSave
        set(value) {
            state.scanOnSave = value
        }

    /**
     * 默认扫描类型
     */
    var defaultScanType: Int
        get() = state.defaultScanType
        set(value) {
            state.defaultScanType = value
        }

    // ========== VCS 钩子检查配置访问器 ==========

    /**
     * 是否启用 pre-commit 检查
     */
    var preCommitCheckEnabled: Boolean
        get() = state.preCommitCheckEnabled
        set(value) {
            state.preCommitCheckEnabled = value
        }

    /**
     * pre-commit 扫描范围（scanType：0=全部, 102=变更文件）
     */
    var preCommitScanScope: Int
        get() = state.preCommitScanScope
        set(value) {
            state.preCommitScanScope = value
        }

    /**
     * 是否启用 pre-push 检查
     */
    var prePushCheckEnabled: Boolean
        get() = state.prePushCheckEnabled
        set(value) {
            state.prePushCheckEnabled = value
        }

    /**
     * pre-push 扫描范围（scanType：0=全部, 103=变更文件）
     */
    var prePushScanScope: Int
        get() = state.prePushScanScope
        set(value) {
            state.prePushScanScope = value
        }

    // ========== 规则集配置访问器 ==========

    /**
     * 已选择的规则集 ID 列表
     */
    var selectedCheckerSets: MutableList<String>
        get() = state.selectedCheckerSets
        set(value) {
            state.selectedCheckerSets = value
        }

    /**
     * 是否记住规则集选择
     */
    var rememberCheckerSets: Boolean
        get() = state.rememberCheckerSets
        set(value) {
            state.rememberCheckerSets = value
        }

    // ========== 结果展示配置访问器 ==========

    /**
     * 显示成功通知
     */
    var showSuccessNotification: Boolean
        get() = state.showSuccessNotification
        set(value) {
            state.showSuccessNotification = value
        }

    /**
     * 显示错误通知
     */
    var showErrorNotification: Boolean
        get() = state.showErrorNotification
        set(value) {
            state.showErrorNotification = value
        }

    /**
     * 自动打开结果窗口
     */
    var autoOpenResults: Boolean
        get() = state.autoOpenResults
        set(value) {
            state.autoOpenResults = value
        }

    // ========== 高级配置访问器 ==========

    /**
     * 启用调试日志
     *
     * 修改此值后会自动更新 Logback 的日志级别
     */
    var enableDebugLog: Boolean
        get() = state.enableDebugLog
        set(value) {
            state.enableDebugLog = value
            // 动态更新日志级别
            try {
                com.codecc.preci.core.log.LogbackConfigurator.updateLogLevel(value)
            } catch (e: Exception) {
                // 忽略异常，避免影响配置保存
            }
        }

    /**
     * 自定义日志文件路径
     *
     * 修改此值后需要重启 IDE 才能生效
     */
    var customLogPath: String
        get() = state.customLogPath
        set(value) {
            state.customLogPath = value
            // 更新 Logback 配置（需要重启才能生效）
            try {
                com.codecc.preci.core.log.LogbackConfigurator.updateLogPath(value)
            } catch (e: Exception) {
                // 忽略异常，避免影响配置保存
            }
        }

    /**
     * HTTP 请求超时时间（秒）
     */
    var requestTimeout: Int
        get() = state.requestTimeout
        set(value) {
            state.requestTimeout = value
        }

    /**
     * 最大重试次数
     */
    var maxRetries: Int
        get() = state.maxRetries
        set(value) {
            state.maxRetries = value
        }

    // ========== 便捷方法 ==========

    /**
     * 重置所有配置为默认值
     */
    fun resetToDefaults() {
        state = PreCISettingsState()
    }

    /**
     * 添加规则集到已选择列表
     *
     * @param checkerSetId 规则集 ID
     */
    fun addCheckerSet(checkerSetId: String) {
        if (!state.selectedCheckerSets.contains(checkerSetId)) {
            state.selectedCheckerSets.add(checkerSetId)
        }
    }

    /**
     * 从已选择列表中移除规则集
     *
     * @param checkerSetId 规则集 ID
     */
    fun removeCheckerSet(checkerSetId: String) {
        state.selectedCheckerSets.remove(checkerSetId)
    }

    /**
     * 清空已选择的规则集列表
     */
    fun clearCheckerSets() {
        state.selectedCheckerSets.clear()
    }
}

