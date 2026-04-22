package com.codecc.preci.util

import java.io.File

/**
 * 路径格式转换工具。
 *
 * IntelliJ 的 `VirtualFile.path` / `Project.basePath` 始终使用正斜杠（`D:/foo`），
 * 而 Windows 上的 PreCI Local Server 使用反斜杠（`D:\foo`）存储和匹配路径。
 * 本工具在 Windows 环境下将路径转换为系统原生格式，macOS/Linux 不做任何处理。
 */
object PathHelper {

    private val isWindows = System.getProperty("os.name").lowercase().startsWith("win")

    /**
     * 将路径转换为系统原生格式，供发送给 PreCI Local Server API 使用。
     * 仅在 Windows 上执行转换，其他平台直接返回原始路径。
     */
    fun toNativePath(path: String): String =
        if (isWindows) File(path).absolutePath else path

    /**
     * 将路径转换为 IntelliJ 格式（正斜杠），供 `LocalFileSystem.findFileByPath` 等 API 使用。
     * 仅在 Windows 上执行转换，其他平台直接返回原始路径。
     */
    fun toIntelliJPath(path: String): String =
        if (isWindows) path.replace('\\', '/') else path
}
