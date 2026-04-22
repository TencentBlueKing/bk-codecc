package com.codecc.preci.core.http

/**
 * PreCI API 异常基类
 *
 * 所有 PreCI API 调用相关的异常都继承自此类，便于统一捕获和处理
 *
 * @property message 异常信息描述
 * @property cause 导致此异常的原始异常（如果有）
 *
 * @since 1.0
 */
sealed class PreCIApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 网络异常
 *
 * 当发生网络连接失败、超时、DNS 解析失败等底层网络问题时抛出此异常
 *
 * **使用场景：**
 * - 无法连接到 Local Server
 * - 请求超时
 * - 网络中断
 *
 * **处理建议：**
 * - 提示用户检查 Local Server 是否启动
 * - 提示用户检查网络连接
 * - 实现重试机制
 *
 * @property message 异常信息描述
 * @property cause 导致此异常的原始网络异常
 *
 * @since 1.0
 */
open class NetworkException(message: String, cause: Throwable? = null) : PreCIApiException(message, cause)

/**
 * 服务繁忙/响应超时异常
 *
 * 当 TCP 连接成功但服务端未在超时时间内返回响应时抛出（Read timed out）。
 * 与 [NetworkException] 的区别：服务是可达的，只是正忙或卡住了。
 *
 * **处理建议：**
 * - 不要盲目重试（服务已收到请求，重复发送可能加重负担）
 * - 不要触发服务自动启动（服务正在运行）
 * - 提示用户服务繁忙，建议稍后重试
 *
 * @since 1.0
 */
class ServerBusyException(message: String, cause: Throwable? = null) : NetworkException(message, cause)

/**
 * 业务异常
 *
 * 当 Local Server 返回 HTTP 4xx 或 5xx 状态码时抛出此异常，表示业务逻辑错误
 *
 * **使用场景：**
 * - 400 Bad Request：请求参数错误
 * - 401 Unauthorized：未登录或认证过期
 * - 404 Not Found：接口不存在
 * - 500 Internal Server Error：服务端内部错误
 *
 * **处理建议：**
 * - 根据 httpCode 和 message 提示用户具体错误信息
 * - 401 错误时引导用户重新登录
 * - 500 错误时建议用户联系技术支持
 *
 * @property httpCode HTTP 状态码（400-599）
 * @property errorCode PreCI 业务错误码（如 100005、100009）
 * @property message 服务端返回的错误信息
 *
 * @since 1.0
 */
class BusinessException(
    val httpCode: Int,
    message: String,
    val errorCode: Int? = null
) : PreCIApiException(message) {
    /**
     * 判断是否是认证错误（需要重新登录）
     */
    fun isAuthError(): Boolean = errorCode == PreCIErrorCode.CODE_INVALID_ACCESS_TOKEN

    /**
     * 判断是否是项目 ID 错误（需要绑定项目）
     */
    fun isProjectIdError(): Boolean = errorCode == PreCIErrorCode.CODE_INVALID_PROJECT_ID
}

/**
 * Server 未启动异常
 *
 * 当检测到 PreCI Local Server 未运行时抛出此异常
 *
 * **使用场景：**
 * - 执行 `preci port` 命令失败
 * - 端口文件不存在或无效
 * - 无法连接到检测到的端口
 *
 * **处理建议：**
 * - 提示用户启动 Local Server
 * - 提供一键启动服务的选项
 * - 显示服务启动指引
 *
 * @property message 异常信息描述
 *
 * @since 1.0
 */
class ServerNotRunningException(message: String) : PreCIApiException(message)

/**
 * 序列化/反序列化异常
 *
 * 当 JSON 序列化或反序列化失败时抛出此异常
 *
 * **使用场景：**
 * - JSON 格式不正确
 * - 数据类型不匹配
 * - 必填字段缺失
 *
 * **处理建议：**
 * - 记录详细日志用于问题排查
 * - 提示用户可能是版本不兼容
 * - 建议用户更新插件或 Local Server
 *
 * @property message 异常信息描述
 * @property cause 导致此异常的原始序列化异常
 *
 * @since 1.0
 */
class SerializationException(message: String, cause: Throwable? = null) : PreCIApiException(message, cause)

/**
 * PreCI 未安装异常
 *
 * 当检测到 PreCI CLI 未安装时抛出此异常
 *
 * **使用场景：**
 * - 执行 `preci version` 命令失败
 * - PreCI 命令不在系统 PATH 中
 * - API 调用失败且尝试启动服务时发现未安装
 *
 * **处理建议：**
 * - 提示用户安装 PreCI CLI
 * - 提供安装指南链接
 * - 提供一键下载安装选项（后续实现）
 *
 * @property message 异常信息描述
 * @property cause 导致此异常的原始异常（如果有）
 *
 * @since 1.0
 */
class ServerNotInstalledException(message: String, cause: Throwable? = null) : PreCIApiException(message, cause)

/**
 * Server 启动失败异常
 *
 * 当 PreCI Local Server 启动失败时抛出此异常
 *
 * **使用场景：**
 * - `preci server start` 命令执行失败
 * - 服务启动超时
 * - 服务启动后无法正常响应
 *
 * **处理建议：**
 * - 提示用户查看 PreCI 日志
 * - 检查端口是否被占用
 * - 建议用户重启 PreCI 或系统
 * - 提供问题排查指南链接
 *
 * @property message 异常信息描述
 * @property cause 导致此异常的原始异常（如果有）
 *
 * @since 1.0
 */
class ServerStartFailedException(message: String, cause: Throwable? = null) : PreCIApiException(message, cause)

