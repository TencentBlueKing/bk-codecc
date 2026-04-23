package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResultQueryResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

/**
 * PreCI 告警高亮监听器
 *
 * 当用户打开文件时，自动从 PreCI Local Server 获取该文件的告警信息，
 * 并在对应的代码行上显示告警标记和提示信息。
 *
 * **功能说明：**
 * - 监听文件打开事件
 * - 异步调用 ScanService.getScanResult() 获取扫描结果
 * - 过滤出当前文件的告警
 * - 根据严重程度在代码行上显示不同颜色的告警标记：
 *   - 严重告警（severity=1）：红色波浪线 + 错误图标
 *   - 一般告警（severity=2）：黄色波浪线 + 警告图标
 *   - 提示告警（severity=4）：蓝绿色波浪线 + 信息图标
 *
 * **使用场景：**
 * - 用户打开文件时自动显示告警
 * - 实时查看代码问题，无需手动刷新
 * - 与 IDE 内置的问题高亮保持一致的用户体验
 *
 * **注意事项：**
 * - 需要 PreCI Local Server 正在运行
 * - 如果从未执行过扫描，可能没有告警信息
 * - 告警信息来自 Local Server 的缓存，可能不是最新的
 * - 使用异步加载，不会阻塞 UI 线程
 *
 * **实现原理：**
 * 1. 注册为 FileEditorManagerListener
 * 2. 在 fileOpened 事件中异步获取告警
 * 3. 使用 MarkupModel 在编辑器中添加高亮
 * 4. 在 fileClosed 事件中清理高亮
 *
 * @since 1.0
 */
class PreCIDefectHighlightListener(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : FileEditorManagerListener {

    private val logger = PreCILogger.getLogger(PreCIDefectHighlightListener::class.java)

    /**
     * 存储每个文件的高亮器引用，用于在文件关闭时清理
     *
     * Key: 文件路径
     * Value: 该文件的高亮器列表
     */
    private val fileHighlighters = ConcurrentHashMap<String, MutableList<RangeHighlighter>>()

    /**
     * 存储每个文件的 Inlay 引用，用于在文件关闭时清理
     *
     * Key: 文件路径
     * Value: 该文件的 Inlay 列表
     */
    private val fileInlays = ConcurrentHashMap<String, MutableList<com.intellij.openapi.editor.Inlay<*>>>()

    /**
     * 已注册鼠标监听器的编辑器集合
     *
     * 使用 WeakHashMap 作为底层存储，确保编辑器被关闭/回收后不会阻止 GC。
     * 外层包装 synchronizedSet 保证线程安全（registerMouseListenerIfNeeded 在 EDT 调用，
     * dispose 可能在其他线程调用）。
     *
     * 注意：注册的 EditorMouseListener 不在 dispose 中显式移除，
     * 因为 Editor 销毁时会自动清理其监听器。
     */
    private val registeredEditors: MutableSet<Editor> = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<Editor, Boolean>())
    )


    /**
     * 文件打开事件处理
     *
     * 当用户打开文件时，异步获取该文件的告警并显示高亮。
     * 同时触发 Inlay Hints 刷新，在代码行上方显示告警信息块。
     *
     * @param source FileEditorManager 实例
     * @param file 打开的文件
     */
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val filePath = file.path
        logger.debug("文件打开: $filePath")

        // 在后台线程中异步获取告警
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val defects = getFileDefects(filePath)

                if (defects.isNotEmpty()) {
                    logger.info("获取到文件告警: $filePath, defects.size=${defects.size}")

                    // 在 UI 线程中添加高亮和块状提示，支持延迟重试
                    addHighlightsWithRetry(source, file, defects, maxRetries = 3)
                } else {
                    logger.debug("文件无告警: $filePath")
                }
            } catch (e: Exception) {
                logger.error("获取文件告警异常: $filePath", e)
            }
        }
    }

    /**
     * 延迟重试添加高亮
     *
     * 当编辑器还没准备好时，会在延迟后重试。
     *
     * @param source FileEditorManager 实例
     * @param file 打开的文件
     * @param defects 告警列表
     * @param maxRetries 最大重试次数
     * @param retryDelayMs 每次重试的延迟时间（毫秒）
     */
    private suspend fun addHighlightsWithRetry(
        source: FileEditorManager,
        file: VirtualFile,
        defects: List<Defect>,
        maxRetries: Int = 3,
        retryDelayMs: Long = 200
    ) {
        val filePath = file.path

        for (attempt in 1..maxRetries) {
            // 在 UI 线程中尝试添加高亮
            val success = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { continuation ->
                ApplicationManager.getApplication().invokeLater {
                    val result = tryAddHighlightsAndBlockHints(source, file, defects)
                    continuation.resume(result) {}
                }
            }

            if (success) {
                logger.debug("成功添加高亮: $filePath, 尝试次数: $attempt")
                return
            }

            if (attempt < maxRetries) {
                logger.debug("添加高亮失败，将在 ${retryDelayMs}ms 后重试: $filePath, 尝试次数: $attempt")
                kotlinx.coroutines.delay(retryDelayMs)
            }
        }

        logger.warn("添加高亮失败，已达到最大重试次数: $filePath")
    }

    /**
     * 尝试添加高亮和块状提示
     *
     * @return 如果成功添加返回 true，否则返回 false
     */
    private fun tryAddHighlightsAndBlockHints(
        source: FileEditorManager,
        file: VirtualFile,
        defects: List<Defect>
    ): Boolean {
        val filePath = file.path

        // 检查文件是否仍然有效
        if (!file.isValid) {
            logger.debug("文件已失效，跳过高亮: $filePath")
            return true // 返回 true 表示不需要重试
        }

        // 获取编辑器
        val allEditors = source.getAllEditors(file)
        val textEditor = allEditors.filterIsInstance<TextEditor>().firstOrNull()

        if (textEditor == null) {
            logger.debug("TextEditor 尚未准备好: $filePath, allEditors.size=${allEditors.size}")
            return false // 返回 false 表示需要重试
        }

        // 调用实际的添加高亮方法
        addHighlightsAndBlockHintsInternal(textEditor, file, defects)
        return true
    }

    /**
     * 文件关闭事件处理
     *
     * 当文件关闭时，清理该文件的所有高亮。
     *
     * @param source FileEditorManager 实例
     * @param file 关闭的文件
     */
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val filePath = file.path
        logger.debug("文件关闭: $filePath")

        // 清理该文件的高亮和 Inlay
        clearHighlights(filePath)
        clearInlays(filePath)
    }

    /**
     * 获取文件的告警列表
     *
     * 直接使用文件路径调用 ScanService 获取该文件的告警。
     * Local Server 支持按文件路径精确查询，无需获取全部再过滤。
     *
     * @param filePath 文件的绝对路径
     * @return 当前文件的告警列表
     */
    private suspend fun getFileDefects(filePath: String): List<Defect> {
        val scanService = ScanService.getInstance(project)

        // 直接使用文件路径查询该文件的告警
        val result = scanService.getScanResult(filePath)

        return when (result) {
            is ScanResultQueryResult.Success -> {
                result.response.getDefectList()
            }
            is ScanResultQueryResult.Failure -> {
                logger.warn("获取扫描结果失败: ${result.message}")
                emptyList()
            }
        }
    }

    /**
     * 添加高亮和块状提示到编辑器（内部实现）
     *
     * 为每个告警在对应的代码行上添加：
     * 1. 波浪线高亮
     * 2. 边栏图标
     * 3. 代码行上方的块状告警提示
     *
     * @param textEditor TextEditor 实例
     * @param file 打开的文件
     * @param defects 告警列表
     */
    private fun addHighlightsAndBlockHintsInternal(textEditor: TextEditor, file: VirtualFile, defects: List<Defect>) {
        val filePath = file.path
        val editor = textEditor.editor
        val document = editor.document
        val markupModel = editor.markupModel

        // 清理旧的高亮和 Inlay
        clearHighlights(filePath)
        clearInlays(filePath)

        // 创建新的高亮列表
        val highlighters = mutableListOf<RangeHighlighter>()
        // 创建新的 Inlay 列表
        val inlays = mutableListOf<com.intellij.openapi.editor.Inlay<*>>()

        // 为每个告警添加高亮
        defects.forEach { defect ->
            try {
                // 行号是从 1 开始的，需要转换为从 0 开始的索引
                val lineNumber = if (defect.line > 0) defect.line - 1 else 0

                // 检查行号是否有效
                if (lineNumber >= document.lineCount) {
                    logger.warn("行号超出范围: $filePath:${defect.line}, document.lineCount=${document.lineCount}")
                    return@forEach
                }

                val lineStartOffset = document.getLineStartOffset(lineNumber)
                val lineEndOffset = document.getLineEndOffset(lineNumber)

                // 创建提示文本（HTML 格式）
                val tooltipText = buildString {
                    append("<html><body style='width: 300px;'>")
                    append("<b>PreCI 告警</b><br/><br/>")
                    append("<b>严重程度:</b> ${escapeHtml(defect.getSeverityText())}<br/>")
                    append("<b>工具:</b> ${escapeHtml(defect.toolName)}<br/>")
                    append("<b>规则:</b> ${escapeHtml(defect.checkerName)}<br/>")
                    append("<b>描述:</b> ${escapeHtml(defect.description)}<br/>")
                    append("<b>行号:</b> ${defect.line}")
                    append("</body></html>")
                }

                // 根据严重程度获取颜色和图标
                val textAttributes = DefectColorScheme.getTextAttributes(defect)
                val icon = DefectColorScheme.getIcon(defect)

                // 添加高亮（根据严重程度使用不同颜色的波浪下划线）
                val highlighter = markupModel.addRangeHighlighter(
                    lineStartOffset,
                    lineEndOffset,
                    HighlighterLayer.WARNING,
                    textAttributes,
                    HighlighterTargetArea.EXACT_RANGE
                )

                // 设置滚动条上的提示
                highlighter.errorStripeTooltip = tooltipText

                // 设置 Gutter 图标和提示（在编辑器左侧显示对应严重程度的图标）
                highlighter.gutterIconRenderer = PreCIGutterIconRenderer(defect, tooltipText, icon)

                highlighters.add(highlighter)

                // 添加块状 Inlay（在代码行上方显示告警信息块）
                try {
                    val inlay = addBlockInlay(editor, defect, lineStartOffset)
                    if (inlay != null) {
                        inlays.add(inlay)
                    }
                } catch (e: Exception) {
                    logger.error("添加块状 Inlay 失败: $filePath:${defect.line}", e)
                }

                logger.debug("添加高亮和块状提示: $filePath:${defect.line}, ${defect.checkerName}")

            } catch (e: Exception) {
                logger.error("添加高亮失败: $filePath:${defect.line}", e)
            }
        }

        // 保存高亮器和 Inlay 引用
        if (highlighters.isNotEmpty()) {
            fileHighlighters[filePath] = highlighters
            logger.info("已为文件添加 ${highlighters.size} 个高亮: $filePath")
        }
        if (inlays.isNotEmpty()) {
            fileInlays[filePath] = inlays
            logger.info("已为文件添加 ${inlays.size} 个块状提示: $filePath")
        }

        // 注册鼠标点击监听器，用于响应 Block Inlay 的展开/折叠操作
        registerMouseListenerIfNeeded(editor)
    }

    /**
     * 为编辑器注册鼠标点击监听器（幂等）
     *
     * 监听鼠标点击事件，当用户点击 Block Inlay 区域时，
     * 转发给 [PreCIDefectBlockRenderer.toggleExpand] 实现多行描述的展开/折叠。
     *
     * 使用 [registeredEditors] 保证每个编辑器只注册一次。
     *
     * @param editor 目标编辑器
     */
    private fun registerMouseListenerIfNeeded(editor: Editor) {
        if (editor in registeredEditors) return
        registeredEditors.add(editor)

        editor.addEditorMouseListener(object : EditorMouseListener {
            @Suppress("TooGenericExceptionCaught")
            override fun mouseClicked(event: EditorMouseEvent) {
                try {
                    val point = event.mouseEvent.point
                    val inlays = editor.inlayModel.getBlockElementsInRange(0, editor.document.textLength)

                    for (inlay in inlays) {
                        val bounds = inlay.bounds ?: continue
                        if (!bounds.contains(point)) continue

                        val renderer = inlay.renderer
                        if (renderer is PreCIDefectBlockRenderer && renderer.isClickable()) {
                            renderer.toggleExpand()
                            inlay.update()
                            break
                        }
                    }
                } catch (e: Exception) {
                    logger.error("处理 Inlay 点击事件失败", e)
                }
            }
        })
    }

    /**
     * 清理文件的高亮
     *
     * 移除指定文件的所有高亮标记。
     *
     * @param filePath 文件路径
     */
    private fun clearHighlights(filePath: String) {
        val highlighters = fileHighlighters.remove(filePath) ?: return

        highlighters.forEach { highlighter ->
            try {
                highlighter.dispose()
            } catch (e: Exception) {
                logger.error("清理高亮失败: $filePath", e)
            }
        }

        logger.debug("已清理文件高亮: $filePath, count=${highlighters.size}")
    }

    /**
     * 清理文件的 Inlay
     *
     * 移除指定文件的所有块状提示。
     *
     * @param filePath 文件路径
     */
    private fun clearInlays(filePath: String) {
        val inlays = fileInlays.remove(filePath) ?: return

        inlays.forEach { inlay ->
            try {
                inlay.dispose()
            } catch (e: Exception) {
                logger.error("清理 Inlay 失败: $filePath", e)
            }
        }

        logger.debug("已清理文件 Inlay: $filePath, count=${inlays.size}")
    }

    /**
     * 添加块状 Inlay
     *
     * 在代码行上方添加自定义渲染的告警提示块。
     *
     * @param editor 编辑器实例
     * @param defect 告警对象
     * @param offset 插入位置的偏移量（行首）
     * @return 创建的 Inlay 对象，失败返回 null
     */
    private fun addBlockInlay(
        editor: com.intellij.openapi.editor.Editor,
        defect: Defect,
        offset: Int
    ): com.intellij.openapi.editor.Inlay<*>? {
        try {
            // 创建自定义渲染器
            val renderer = PreCIDefectBlockRenderer(defect)

            // 在代码行上方添加块状 Inlay
            val inlay = editor.inlayModel.addBlockElement(
                offset,
                true,  // relatesToPrecedingText
                true,  // showAbove - 显示在代码行上方
                0,     // priority
                renderer
            )

            logger.debug("添加块状 Inlay: ${defect.filePath}:${defect.line}")
            return inlay

        } catch (e: Exception) {
            logger.error("创建块状 Inlay 失败: ${defect.filePath}:${defect.line}", e)
            return null
        }
    }

    /**
     * 刷新指定文件的高亮和块状提示
     *
     * 重新获取告警并更新显示。可在扫描完成后调用以更新显示。
     *
     * @param filePath 文件路径，如果为 null 则刷新所有打开的文件
     */
    fun refreshHighlights(filePath: String? = null) {
        val fileEditorManager = FileEditorManager.getInstance(project)

        val filesToRefresh = if (filePath != null) {
            fileEditorManager.openFiles.filter { it.path == filePath }
        } else {
            fileEditorManager.openFiles.toList()
        }

        filesToRefresh.forEach { file ->
            // 模拟文件关闭-打开以刷新高亮和块状提示
            clearHighlights(file.path)
            clearInlays(file.path)
            fileOpened(fileEditorManager, file)
        }
    }

    /**
     * 清理所有高亮和 Inlay
     *
     * 在组件销毁时调用，清理所有文件的高亮和块状提示。
     */
    fun dispose() {
        fileHighlighters.keys.toList().forEach { filePath ->
            clearHighlights(filePath)
        }
        fileInlays.keys.toList().forEach { filePath ->
            clearInlays(filePath)
        }
        registeredEditors.clear()
        logger.debug("已清理所有高亮和 Inlay")
    }

    /**
     * HTML 转义
     *
     * 转义 HTML 特殊字符，防止 XSS 和显示问题。
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    companion object {
        private val logger = PreCILogger.getLogger(PreCIDefectHighlightListener::class.java)

        /**
         * 创建并注册监听器
         *
         * 在项目打开时调用，注册文件编辑器监听器。
         *
         * @param project 项目实例
         * @param coroutineScope 协程作用域
         * @return 监听器实例
         */
        @JvmStatic
        fun register(project: Project, coroutineScope: CoroutineScope): PreCIDefectHighlightListener {
            val listener = PreCIDefectHighlightListener(project, coroutineScope)

            // 注册监听器
            project.messageBus.connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                listener
            )

            logger.info("PreCI 告警高亮监听器已注册")

            // 对当前已打开的文件添加高亮
            val fileEditorManager = FileEditorManager.getInstance(project)
            fileEditorManager.openFiles.forEach { file ->
                listener.fileOpened(fileEditorManager, file)
            }

            return listener
        }
    }

    /**
     * PreCI Gutter 图标渲染器
     *
     * 在编辑器左侧的 Gutter 区域显示告警图标，
     * 鼠标悬停时显示告警详情，点击时可触发操作。
     *
     * 根据告警的严重程度显示不同的图标：
     * - 严重告警（severity=1）：红色错误图标
     * - 一般告警（severity=2）：黄色警告图标
     * - 提示告警（severity=4）：蓝色信息图标
     *
     * @param defect 告警信息
     * @param tooltipText 提示文本（HTML 格式）
     * @param icon 要显示的图标
     */
    private class PreCIGutterIconRenderer(
        private val defect: Defect,
        private val tooltipText: String,
        private val icon: Icon
    ) : GutterIconRenderer() {

        /**
         * 获取图标
         *
         * 根据告警严重程度返回对应的图标
         */
        override fun getIcon(): Icon = icon

        /**
         * 获取悬停提示文本
         *
         * 鼠标悬停在图标上时显示的提示
         */
        override fun getTooltipText(): String = tooltipText

        /**
         * 获取对齐方式
         *
         * 图标靠左对齐
         */
        override fun getAlignment(): Alignment = Alignment.LEFT

        /**
         * 判断是否可导航
         *
         * 点击图标时不进行导航
         */
        override fun isNavigateAction(): Boolean = false

        /**
         * 获取点击动作
         *
         * 点击图标时显示告警详情（可选）
         */
        override fun getClickAction(): AnAction? {
            return object : AnAction("查看 PreCI 告警详情") {
                override fun actionPerformed(e: AnActionEvent) {
                    // 点击时可以显示更详细的信息或跳转到问题面板
                    // 目前只是日志记录
                    PreCILogger.getLogger(PreCIGutterIconRenderer::class.java)
                        .info("点击告警: ${defect.checkerName} at ${defect.filePath}:${defect.line}")
                }
            }
        }

        /**
         * 判断两个渲染器是否相等
         *
         * 用于去重和更新判断
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PreCIGutterIconRenderer) return false
            return defect.filePath == other.defect.filePath &&
                defect.line == other.defect.line &&
                defect.checkerName == other.defect.checkerName
        }

        /**
         * 计算哈希码
         */
        override fun hashCode(): Int {
            var result = defect.filePath.hashCode()
            result = 31 * result + defect.line
            result = 31 * result + defect.checkerName.hashCode()
            return result
        }
    }
}
