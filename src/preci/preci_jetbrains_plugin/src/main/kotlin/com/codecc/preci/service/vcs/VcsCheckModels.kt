package com.codecc.preci.service.vcs

import com.codecc.preci.api.model.response.Defect

/**
 * VCS 检查相关数据模型
 *
 * 封装 Git 钩子检查的配置和结果数据类型。
 *
 * @since 1.1
 */

/**
 * Git 钩子扫描范围
 *
 * 定义 pre-commit 和 pre-push 检查时的扫描范围选项。
 *
 * @property scanType 对应的 PreCI Local Server scanType 值
 */
enum class GitHookScanScope(val scanType: Int) {
    /** 变更文件代码检查（pre-commit，scanType=102） */
    CHANGED_FILES_COMMIT(102),

    /** 变更文件代码检查（pre-push，scanType=103） */
    CHANGED_FILES_PUSH(103),

    /** 全部文件代码检查（scanType=0） */
    ALL_FILES(0)
}

/**
 * VCS 检查结果
 *
 * 封装 pre-commit / pre-push 检查的执行结果。
 * 调用方根据不同的结果子类决定是否阻止 commit/push。
 *
 * @since 1.1
 */
sealed class VcsCheckResult {
    /**
     * 检查未启用，直接放行
     */
    data object Disabled : VcsCheckResult()

    /**
     * 扫描完成且无缺陷，放行
     *
     * @property message 提示信息
     */
    data class NoDefects(val message: String) : VcsCheckResult()

    /**
     * 扫描完成且发现缺陷，需要阻止并提示用户
     *
     * @property defectCount 缺陷总数
     * @property defects 缺陷列表
     */
    data class HasDefects(
        val defectCount: Int,
        val defects: List<Defect>
    ) : VcsCheckResult()

    /**
     * 检查过程中发生错误（网络、超时等），默认放行以不阻塞用户
     *
     * @property message 错误信息
     * @property exception 异常对象
     */
    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : VcsCheckResult()
}
