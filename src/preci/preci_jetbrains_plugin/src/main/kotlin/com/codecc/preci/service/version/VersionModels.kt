package com.codecc.preci.service.version

/**
 * 版本服务相关数据模型
 *
 * 封装版本服务的各种操作结果，包括版本查询、更新检查和执行更新。
 *
 * @since 2.0
 */

/**
 * 版本查询结果
 *
 * 封装通过 CLI 或 API 获取版本号的操作结果。
 *
 * @since 2.0
 */
sealed class VersionResult {
    /**
     * 查询成功
     *
     * @property version 版本号字符串（如 "1.0.0"）
     */
    data class Success(val version: String) : VersionResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : VersionResult()
}

/**
 * 更新检查结果
 *
 * 封装版本比较后的结果，判断是否有可用更新。
 *
 * @since 2.0
 */
sealed class UpdateCheckResult {
    /**
     * 有可用更新
     *
     * @property currentVersion 当前本地版本号
     * @property latestVersion 线上最新版本号
     */
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String
    ) : UpdateCheckResult()

    /**
     * 已是最新版本
     *
     * @property currentVersion 当前版本号
     */
    data class AlreadyLatest(val currentVersion: String) : UpdateCheckResult()

    /**
     * 检查失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : UpdateCheckResult()
}

/**
 * 执行更新结果
 *
 * 封装通过 `preci update` 命令执行更新的操作结果。
 *
 * @since 2.0
 */
sealed class UpdateResult {
    /**
     * 更新成功
     *
     * @property message 成功信息（包含更新输出）
     */
    data class Success(val message: String) : UpdateResult()

    /**
     * 更新失败
     *
     * @property message 失败原因描述
     * @property exception 异常对象（可选）
     */
    data class Failure(
        val message: String,
        val exception: Exception? = null
    ) : UpdateResult()
}
