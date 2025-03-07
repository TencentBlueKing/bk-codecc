package com.tencent.bk.codecc.defect.utils

object FileUtils {

    /**
     * 获取代码指定行内容
     */
    fun getLinesContent(fileContent: String, startLine: Int, endLine: Int): String {
        val content = mutableListOf<String>()
        val lines = fileContent.split("\n")
        if (startLine in 1..lines.size && endLine in 1..lines.size && startLine <= endLine) {
            for (lineNumber in startLine..endLine) {
                content.add(lines[lineNumber - 1])
            }
        } else {
            return "Line number out of range or start line is greater than end line."
        }
        return content.joinToString("/n")
    }
}
