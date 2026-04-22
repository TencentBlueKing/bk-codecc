package com.codecc.preci.service.checker

import com.codecc.preci.api.model.response.CheckerSet
import com.codecc.preci.api.model.response.CheckerSetListResponse
import com.codecc.preci.api.model.response.CheckerSetSelectResponse

/**
 * 规则集信息
 *
 * 用于在服务层传递规则集信息的数据类，从 API 响应中的 [CheckerSet] 转换而来。
 *
 * @property id 规则集唯一标识 ID
 * @property name 规则集显示名称
 * @property toolName 规则集对应的检查工具（如 golangci-lint）
 *
 * @since 1.0
 */
data class CheckerSetInfo(
    val id: String,
    val name: String,
    val toolName: String
) {
    companion object {
        /**
         * 从 API 响应的 [CheckerSet] 转换为 [CheckerSetInfo]
         *
         * @param checkerSet API 响应中的规则集数据
         * @return 服务层的规则集信息
         */
        fun fromApiResponse(checkerSet: CheckerSet): CheckerSetInfo {
            return CheckerSetInfo(
                id = checkerSet.checkerSetId,
                name = checkerSet.checkerSetName,
                toolName = checkerSet.toolName
            )
        }
    }
}

/**
 * 规则集列表查询结果密封类
 *
 * 表示获取规则集列表操作的结果，使用密封类保证类型安全。
 * 调用 [CheckerService.getCheckerSetList] 方法后返回此类型。
 *
 * **使用示例：**
 * ```kotlin
 * when (val result = checkerService.getCheckerSetList()) {
 *     is CheckerSetListResult.Success -> {
 *         result.checkerSets.forEach { println("${it.name} (${it.toolName})") }
 *     }
 *     is CheckerSetListResult.Failure -> {
 *         println("获取失败: ${result.message}")
 *     }
 * }
 * ```
 *
 * @since 1.0
 */
sealed class CheckerSetListResult {
    /**
     * 查询成功
     *
     * @property checkerSets 规则集列表
     * @property response 原始 API 响应
     *
     * @since 1.0
     */
    data class Success(
        val checkerSets: List<CheckerSetInfo>,
        val response: CheckerSetListResponse
    ) : CheckerSetListResult()

    /**
     * 查询失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : CheckerSetListResult()
}

/**
 * 规则集选择结果密封类
 *
 * 表示选择规则集操作的结果，使用密封类保证类型安全。
 * 调用 [CheckerService.selectCheckerSets] 方法后返回此类型。
 *
 * **使用示例：**
 * ```kotlin
 * when (val result = checkerService.selectCheckerSets(listOf("set1", "set2"))) {
 *     is CheckerSetSelectResult.Success -> {
 *         println("成功选择 ${result.selectedSets.size} 个规则集")
 *     }
 *     is CheckerSetSelectResult.Failure -> {
 *         println("选择失败: ${result.message}")
 *     }
 * }
 * ```
 *
 * @since 1.0
 */
sealed class CheckerSetSelectResult {
    /**
     * 选择成功
     *
     * @property projectRoot 项目根目录
     * @property selectedSets 成功选择的规则集 ID 列表
     * @property response 原始 API 响应
     *
     * @since 1.0
     */
    data class Success(
        val projectRoot: String,
        val selectedSets: List<String>,
        val response: CheckerSetSelectResponse? = null
    ) : CheckerSetSelectResult()

    /**
     * 选择失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : CheckerSetSelectResult()
}

/**
 * 规则集配置持久化结果密封类
 *
 * 表示规则集配置持久化操作的结果。
 *
 * @since 1.0
 */
sealed class CheckerSetPersistResult {
    /**
     * 持久化成功
     *
     * @property savedSets 成功保存的规则集 ID 列表
     *
     * @since 1.0
     */
    data class Success(
        val savedSets: List<String>
    ) : CheckerSetPersistResult()

    /**
     * 持久化失败
     *
     * @property message 失败原因描述
     * @property cause 原始异常（如果有）
     *
     * @since 1.0
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : CheckerSetPersistResult()
}
