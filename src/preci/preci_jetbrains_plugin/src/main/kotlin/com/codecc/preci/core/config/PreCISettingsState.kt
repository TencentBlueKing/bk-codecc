package com.codecc.preci.core.config

/**
 * PreCI 配置状态数据类
 *
 * 存储用户的配置偏好，所有字段都使用 var 以支持 IntelliJ Platform 的 XML 序列化机制。
 * 该类的实例会被自动序列化并持久化到 IDE 的配置目录中。
 *
 * **配置分类：**
 * - Local Server 相关配置：服务器的启动和管理配置
 * - 扫描相关配置：代码扫描的默认行为
 * - 规则集配置：规则集的选择和记忆
 * - 结果展示配置：扫描结果的展示方式
 * - 高级配置：调试和网络相关配置
 *
 * @since 1.0
 */
data class PreCISettingsState(
    // ========== PreCI CLI 路径配置 ==========

    /**
     * PreCI CLI 可执行文件路径
     *
     * 用户可在 Settings → Tools → PreCI 中手动指定 preci 的绝对路径。
     * 为空时插件会自动检测。
     */
    var preciPath: String = "",

    // ========== Local Server 相关配置 ==========
    
    /**
     * 是否自动启动 Local Server
     *
     * 当启用时，插件会在 IDE 启动或需要使用 PreCI 功能时自动启动 Local Server
     */
    var serverAutoStart: Boolean = false,
    
    /**
     * Server 启动超时时间（秒）
     *
     * 等待 Local Server 启动的最大时间，超时后会提示用户启动失败
     */
    var serverStartTimeout: Int = 30,
    
    // ========== 鉴权相关配置 ==========
    
    /**
     * 当前选择的蓝盾项目 ID
     *
     * 存储用户在设置页面选择的蓝盾项目 ID，用于关联项目和规则集配置
     */
    var currentProjectId: String = "",
    
    // ========== 扫描相关配置 ==========
    
    /**
     * 保存文件时自动扫描
     *
     * 当启用时，在保存文件后会自动触发目标扫描（扫描当前文件）
     */
    var scanOnSave: Boolean = false,
    
    /**
     * 默认扫描类型
     *
     * 取值：
     * - 1: 全量扫描
     * - 2: 目标扫描
     * - 3: pre-commit 增量扫描
     * - 4: pre-push 增量扫描
     */
    var defaultScanType: Int = 1,

    // ========== VCS 钩子检查配置 ==========

    /**
     * 是否启用 pre-commit 检查
     *
     * 当启用时，在 commit 前会触发代码扫描，发现缺陷时可阻止提交
     */
    var preCommitCheckEnabled: Boolean = false,

    /**
     * pre-commit 扫描范围对应的 scanType
     *
     * 取值：0=全部文件, 102=变更文件（暂存区）
     */
    var preCommitScanScope: Int = 102,

    /**
     * 是否启用 pre-push 检查
     *
     * 当启用时，在 push 前会触发代码扫描，发现缺陷时可阻止推送
     */
    var prePushCheckEnabled: Boolean = false,

    /**
     * pre-push 扫描范围对应的 scanType
     *
     * 取值：0=全部文件, 103=变更文件（未推送）
     */
    var prePushScanScope: Int = 103,

    // ========== 规则集配置 ==========
    
    /**
     * 已选择的规则集 ID 列表
     *
     * 存储用户上次选择的规则集 ID，用于下次扫描时自动应用
     */
    var selectedCheckerSets: MutableList<String> = mutableListOf(),
    
    /**
     * 是否记住规则集选择
     *
     * 当启用时，插件会记住用户的规则集选择，并在下次扫描时自动应用
     */
    var rememberCheckerSets: Boolean = true,
    
    // ========== 结果展示配置 ==========
    
    /**
     * 显示成功通知
     *
     * 当扫描成功完成且没有发现问题时，是否显示通知
     */
    var showSuccessNotification: Boolean = true,
    
    /**
     * 显示错误通知
     *
     * 当扫描失败或发生错误时，是否显示通知
     */
    var showErrorNotification: Boolean = true,
    
    /**
     * 自动打开结果窗口
     *
     * 当扫描完成后，是否自动打开结果工具窗口显示扫描结果
     */
    var autoOpenResults: Boolean = true,
    
    // ========== 高级配置 ==========
    
    /**
     * 启用调试日志
     *
     * 当启用时，会输出更详细的调试日志（DEBUG 和 TRACE 级别），用于问题排查。
     * 日志会输出到：${user.home}/.preci/logs/preci.log
     */
    var enableDebugLog: Boolean = false,
    
    /**
     * 自定义日志文件路径
     *
     * 指定日志文件的保存路径。如果为空字符串，则使用默认路径：
     * ${user.home}/.preci/logs/preci.log
     *
     * 注意：修改路径后需要重启 IDE 才能生效
     */
    var customLogPath: String = "",
    
    /**
     * HTTP 请求超时时间（秒）
     *
     * 与 Local Server 通信时的读写超时时间
     */
    var requestTimeout: Int = 30,
    
    /**
     * 最大重试次数
     *
     * 当网络请求失败时，自动重试的最大次数
     */
    var maxRetries: Int = 3
)

