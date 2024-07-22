package com.tencent.bk.codecc.scanschedule.constants

object ScanConstants {
    /**
     * nfs根路径
     */
    const val NFS_ROOT_PATH = "/data/bkee/codecc/nfs"

    /**
     * 二进制工具安装路径
     */
    const val BINARY_TOOL_INSTALL_PATH = "$NFS_ROOT_PATH/tools"

    /**
     * 文本保存路径
     */
    const val CONTENT_WORKSPACE_PATH = "$NFS_ROOT_PATH/workspace"

    /**
     * 代码片段存放文件命名
     */
    const val SCAN_FILE_NAME = "content"

    /**
     * codecc扫描中间目录
     */
    const val SCAN_RESULT_PATH = ".codecc"

    /**
     * 工具扫描输入文件后缀
     */
    const val SCAN_INPUT_SUFFIX = "_input.json"

    /**
     * 工具扫描输出文件后缀
     */
    const val SCAN_OUTPUT_SUFFIX = "_output.json"

    /**
     * 临时存放工具信息，后续通过OP自动获取相关工具信息
     */
    const val SCAN_SCHEDULE_DEFAULT_CHECKER_SET = "pecker_sensitive"

    /**
     * 默认工具版本定义
     */
    const val DEFAULT_TOOL_VERSION = "latest"
}