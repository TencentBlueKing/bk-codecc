package com.codecc.preci.core.http

/**
 * PreCI Server 错误码定义
 *
 * 对应 preci_server 的错误码常量，用于识别和处理特定的业务错误
 *
 * **参考：** `preci_server_v2/internal/util/perror/code.go`
 *
 * @since 1.0
 */
object PreCIErrorCode {
    /**
     * 项目根目录不合法
     */
    const val CODE_INVALID_ROOT_DIR = 100000

    /**
     * 不合法的路径
     */
    const val CODE_INVALID_PATHS = 100001

    /**
     * 其他扫描任务正在运行
     */
    const val CODE_OTHER_SCAN_RUNNING = 100002

    /**
     * 当前没有扫描任务
     */
    const val CODE_NO_SCAN_TASK = 100003

    /**
     * 快速登录失败
     */
    const val CODE_QUICK_LOGIN_ERROR = 100004

    /**
     * Access Token 无效，需要重新登录
     *
     * **处理策略：** 自动调用快速登录
     */
    const val CODE_INVALID_ACCESS_TOKEN = 100005

    /**
     * 无法获取最新版本信息
     */
    const val CODE_NO_LATEST_VERSION = 100006

    /**
     * 当前已是最新版本
     */
    const val CODE_IS_LATEST_VERSION = 100007

    /**
     * 下载失败
     */
    const val CODE_DOWNLOAD_FAILED = 100008

    /**
     * 蓝盾项目 ID 无效或缺失
     *
     * **处理策略：** 提示用户去 Settings/Tools/PreCI 绑定项目
     */
    const val CODE_INVALID_PROJECT_ID = 100009

    /**
     * 扫描规则集无效或缺失
     */
    const val CODE_INVALID_CHECKER_SET = 100010

    /**
     * 判断错误码是否需要重新登录
     *
     * @param errorCode 错误码
     * @return 如果需要重新登录返回 true，否则返回 false
     */
    fun isAuthError(errorCode: Int): Boolean {
        return errorCode == CODE_INVALID_ACCESS_TOKEN
    }

    /**
     * 判断错误码是否需要绑定项目
     *
     * @param errorCode 错误码
     * @return 如果需要绑定项目返回 true，否则返回 false
     */
    fun isProjectIdError(errorCode: Int): Boolean {
        return errorCode == CODE_INVALID_PROJECT_ID
    }
}

