package com.tencent.bk.codecc.defect.model.sca

/**
 * SCA 扫描上下文
 */
class SCAScanContext(
    /**
     * 是否全量扫描
     */
    var fullScan: Boolean = false,
    /**
     * 增量文件绝对文件列表
     */
    var incrementPaths: List<String>? = null,
    /**
     * 根路径
     */
    var rootPaths: Set<String>? = null,
    /**
     * 删除文件列表
     */
    var deletedPaths: List<String>? = null,
    /**
     * 是否重新分配处理人
     */
    var reallocate: Boolean = false,

) {
    /**
     * 检查文件路径是否在扫描范围内
     *
     * @param filePath 文件的绝对路径
     * @param relPath 文件的相对路径(相对于代码库根目录)
     * @return true-文件在扫描范围内; false-文件不在扫描范围内
     *
     * 判断逻辑:
     * 1. 如果是全量扫描,则所有文件都在扫描范围内
     * 2. 如果文件路径为空,则认为在扫描范围内
     * 3. 增量扫描时:
     *   a. 检查文件的绝对路径是否在扫描文件列表中(incrementPaths和deletedPaths)
     *   b. 检查文件的相对路径是否匹配任意根路径(rootPaths)转换后的相对路径
     */
    fun isFileScanned(filePath: String?, relPath: String?): Boolean {
        if (fullScan) {
            return true
        }
        if (filePath.isNullOrEmpty() && relPath.isNullOrEmpty()) {
            return true
        }
        // 合并incrementPaths与deletedPaths
        val scanFilePath = (incrementPaths ?: emptyList()) + (deletedPaths ?: emptyList())
        if (!filePath.isNullOrEmpty() && scanFilePath.contains(filePath)) {
            return true
        }
        if (!relPath.isNullOrEmpty()) {
            if (!rootPaths.isNullOrEmpty()) {
                return scanFilePath.any { path ->
                    // 遍历所有rootPath,找出所有匹配的
                    rootPaths!!.mapNotNull { root ->
                        if (path.startsWith(root)) {
                            // 移除前缀,确保以/开头
                            val relativePath = path.substring(root.length)
                            if (relativePath.startsWith("/")) relativePath else "/$relativePath"
                        } else {
                            null
                        }
                    }.contains(relPath)
                }
            }
        }
        return false
    }
}
