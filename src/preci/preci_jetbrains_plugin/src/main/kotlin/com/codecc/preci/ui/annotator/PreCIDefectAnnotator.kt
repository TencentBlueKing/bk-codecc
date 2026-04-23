package com.codecc.preci.ui.annotator

import com.codecc.preci.api.model.response.Defect
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.ScanResultQueryResult
import com.codecc.preci.service.scan.ScanService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 文件信息
 *
 * 用于在 collectInformation 和 doAnnotate 之间传递文件路径信息。
 *
 * @property filePath 文件的绝对路径
 * @property project 项目实例
 *
 * @since 1.0
 */
data class FileInfo(
    val filePath: String,
    val project: Project
)

/**
 * 注解结果
 *
 * 包含当前文件的所有告警信息。
 *
 * @property defects 当前文件的告警列表
 * @property filePath 文件路径
 *
 * @since 1.0
 */
data class AnnotationResult(
    val defects: List<Defect>,
    val filePath: String
)

/**
 * PreCI 缺陷注解器
 *
 * **注意：此实现已被 [PreCIDefectHighlightListener] 替代。**
 *
 * 原因：IntelliJ Platform 的 `ExternalAnnotator` 需要绑定到具体语言才能生效，
 * 使用 `language="ANY"` 是无效的。新实现使用 `FileEditorManagerListener` + `MarkupModel`
 * 的方式，可以对所有文件类型生效。
 *
 * 保留此文件的原因：
 * - 可作为特定语言的注解器实现参考
 * - 可能在未来需要针对特定语言实现不同的高亮逻辑
 *
 * ---
 *
 * 当用户打开文件时，自动从 PreCI Local Server 获取该文件的告警信息，
 * 并在对应的代码行上显示告警标记和提示信息。
 *
 * **功能说明：**
 * - 监听文件打开事件
 * - 异步调用 ScanService.getScanResult() 获取扫描结果
 * - 过滤出当前文件的告警
 * - 根据严重程度在代码行上显示不同级别的告警标记：
 *   - 严重告警（severity=1）：ERROR 级别（红色）
 *   - 一般告警（severity=2）：WARNING 级别（黄色）
 *   - 提示告警（severity=4）：WEAK_WARNING 级别（灰色/蓝绿色）
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
 * 1. collectInformation: 收集文件路径信息（同步）
 * 2. doAnnotate: 异步调用 ScanService 获取告警（后台线程）
 * 3. apply: 在代码行上显示告警标记（UI 线程）
 *
 * @since 1.0
 * @see PreCIDefectHighlightListener 推荐使用的新实现
 */
class PreCIDefectAnnotator : ExternalAnnotator<FileInfo, AnnotationResult>() {

    private val logger = PreCILogger.getLogger(PreCIDefectAnnotator::class.java)

    /**
     * 收集文件信息
     *
     * 在文件打开时同步调用，收集文件路径等信息。
     * 这个方法在 UI 线程中执行，应该快速返回。
     *
     * @param file 打开的 PsiFile
     * @return 文件信息，如果无法获取文件路径则返回 null
     */
    override fun collectInformation(file: PsiFile): FileInfo? {
        val project = file.project
        val virtualFile = file.virtualFile ?: return null
        val filePath = virtualFile.path

        logger.debug("收集文件信息: $filePath")

        return FileInfo(
            filePath = filePath,
            project = project
        )
    }

    /**
     * 异步执行注解
     *
     * 在后台线程中调用 ScanService 获取扫描结果，并过滤出当前文件的告警。
     * 这个方法在后台线程中执行，可以执行耗时的网络请求。
     *
     * @param fileInfo 文件信息
     * @return 注解结果，包含当前文件的所有告警
     */
    override fun doAnnotate(fileInfo: FileInfo?): AnnotationResult? {
        if (fileInfo == null) {
            return null
        }

        val filePath = fileInfo.filePath
        val project = fileInfo.project

        logger.info("开始获取文件告警: $filePath")

        // 使用 runBlocking 在后台线程中执行协程
        return runBlocking(Dispatchers.IO) {
            try {
                val scanService = ScanService.getInstance(project)

                // 直接使用文件路径查询该文件的告警
                // Local Server 支持按文件路径精确查询，无需获取全部再过滤
                val result = scanService.getScanResult(filePath)

                when (result) {
                    is ScanResultQueryResult.Success -> {
                        val fileDefects = result.response.getDefectList()

                        logger.info("文件告警获取成功: $filePath, defects.size=${fileDefects.size}")

                        AnnotationResult(
                            defects = fileDefects,
                            filePath = filePath
                        )
                    }
                    is ScanResultQueryResult.Failure -> {
                        logger.warn("文件告警获取失败: $filePath, error=${result.message}")
                        // 返回空结果，不显示告警
                        null
                    }
                }
            } catch (e: Exception) {
                logger.error("文件告警获取异常: $filePath", e)
                // 返回空结果，不显示告警
                null
            }
        }
    }

    /**
     * 应用注解到编辑器
     *
     * 在 UI 线程中调用，将告警信息显示在代码行上。
     * 使用黄色波浪线标记问题行，并在鼠标悬停时显示详细信息。
     *
     * @param file 文件
     * @param annotationResult 注解结果
     * @param holder 注解持有者，用于创建注解
     */
    override fun apply(
        file: PsiFile,
        annotationResult: AnnotationResult?,
        holder: AnnotationHolder
    ) {
        if (annotationResult == null) {
            return
        }

        val filePath = annotationResult.filePath
        val defects = annotationResult.defects

        logger.debug("应用注解到文件: $filePath, defects.size=${defects.size}")

        // 为每个告警创建注解
        defects.forEach { defect ->
            try {
                // 行号是从 1 开始的，需要转换为从 0 开始的索引
                val lineNumber = if (defect.line > 0) defect.line - 1 else 0

                // 获取对应的代码行
                val document = file.viewProvider.document ?: return@forEach
                if (lineNumber >= document.lineCount) {
                    logger.warn("行号超出范围: $filePath:${defect.line}, document.lineCount=${document.lineCount}")
                    return@forEach
                }

                val lineStartOffset = document.getLineStartOffset(lineNumber)
                val lineEndOffset = document.getLineEndOffset(lineNumber)

                // 创建告警注解的消息
                val message = "[${defect.toolName}/${defect.checkerName}] ${defect.description}"

                // 创建工具提示文本
                val tooltipText = buildString {
                    append("<html><body>")
                    append("<b>PreCI 告警</b><br/>")
                    append("严重程度: ${defect.getSeverityText()}<br/>")
                    append("工具: ${defect.toolName}<br/>")
                    append("规则: ${defect.checkerName}<br/>")
                    append("描述: ${defect.description}<br/>")
                    append("文件: ${defect.filePath}<br/>")
                    append("行号: ${defect.line}")
                    append("</body></html>")
                }

                // 根据严重程度确定高亮级别
                val highlightSeverity = when (defect.severity) {
                    1L -> HighlightSeverity.ERROR           // 严重：错误级别（红色）
                    2L -> HighlightSeverity.WARNING         // 一般：警告级别（黄色）
                    4L -> HighlightSeverity.WEAK_WARNING    // 提示：弱警告级别（灰色/蓝绿色）
                    else -> HighlightSeverity.WEAK_WARNING  // 默认：提示：弱警告级别（灰色/蓝绿色）
                }

                // 使用新的 API 创建告警注解
                // 根据严重程度使用不同的高亮级别
                holder.newAnnotation(highlightSeverity, message)
                    .range(TextRange(lineStartOffset, lineEndOffset))
                    .tooltip(tooltipText)
                    .create()

                logger.debug("创建告警注解: $filePath:${defect.line}, ${defect.checkerName}")

            } catch (e: Exception) {
                logger.error("创建告警注解失败: $filePath:${defect.line}", e)
            }
        }
    }
}

