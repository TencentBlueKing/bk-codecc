package com.codecc.preci.ui.settings

import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * PreCI 配置界面入口
 *
 * 实现 [Configurable] 接口，负责创建配置 UI 组件并处理配置的应用、重置等操作。
 * 该类会在 IDE 的 Settings 对话框中显示为 "PreCI" 配置项。
 *
 * **配置位置：** Settings → Tools → PreCI
 *
 * @since 1.0
 */
class PreCIConfigurable : Configurable {

    private val logger = PreCILogger.getLogger(PreCIConfigurable::class.java)

    /**
     * 配置 UI 组件
     */
    private var settingsComponent: PreCISettingsComponent? = null

    /**
     * 获取配置页面的显示名称
     *
     * @return 显示名称 "PreCI"
     */
    override fun getDisplayName(): String = "PreCI"

    /**
     * 创建配置 UI 组件
     *
     * 此方法由 IntelliJ Platform 调用，用于创建配置界面
     *
     * @return 配置界面的根 JComponent
     */
    override fun createComponent(): JComponent? {
        logger.info("PreCIConfigurable.createComponent() 被调用")
        settingsComponent = PreCISettingsComponent()
        return settingsComponent?.getPanel()
    }

    /**
     * 检查配置是否被修改
     *
     * 用于判断是否需要启用 "Apply" 按钮
     *
     * @return true 如果配置已被修改，false 否则
     */
    override fun isModified(): Boolean {
        val settings = PreCISettings.getInstance()
        val modified = settingsComponent?.isModified(settings) ?: false
        logger.info("PreCIConfigurable.isModified() 被调用，返回: $modified")
        return modified
    }

    /**
     * 应用配置修改
     *
     * 当用户点击 "Apply" 或 "OK" 按钮时调用，将 UI 中的配置保存到 PreCISettings
     */
    override fun apply() {
        logger.info("PreCIConfigurable.apply() 被调用 - 用户点击了 Apply 或 OK 按钮")
        val settings = PreCISettings.getInstance()
        settingsComponent?.apply(settings)
        logger.info("PreCIConfigurable.apply() 执行完成")
    }

    /**
     * 重置配置
     *
     * 当用户点击 "Reset" 按钮时调用，将 UI 重置为当前保存的配置
     */
    override fun reset() {
        logger.info("PreCIConfigurable.reset() 被调用")
        val settings = PreCISettings.getInstance()
        settingsComponent?.reset(settings)
    }

    /**
     * 释放资源
     *
     * 当配置对话框关闭时调用
     */
    override fun disposeUIResources() {
        logger.info("PreCIConfigurable.disposeUIResources() 被调用 - Settings 对话框关闭")
        settingsComponent = null
    }
}

