/**
 * 规则集服务模块
 *
 * 负责代码检查规则集的管理，是 PreCI Local Server 规则集相关 API 的客户端封装。
 *
 * **核心功能：**
 * - 规则集列表查询（`GET /checker/set/list`）
 * - 规则集选择（`POST /checker/set/select`）
 * - 配置持久化（保存/加载用户的规则集选择偏好）
 *
 * **主要类：**
 * - [CheckerService]：规则集服务接口，定义所有规则集管理方法
 * - [CheckerServiceImpl]：规则集服务实现，处理 API 调用和配置持久化
 * - [CheckerSetInfo]：规则集信息数据类
 * - [CheckerSetListResult]：规则集列表查询结果密封类
 * - [CheckerSetSelectResult]：规则集选择结果密封类
 * - [CheckerSetPersistResult]：规则集配置持久化结果密封类
 *
 * **使用示例：**
 * ```kotlin
 * val checkerService = CheckerService.getInstance(project)
 *
 * // 获取规则集列表
 * when (val result = checkerService.getCheckerSetList()) {
 *     is CheckerSetListResult.Success -> {
 *         result.checkerSets.forEach { set ->
 *             println("规则集: ${set.name} (${set.toolName})")
 *         }
 *     }
 *     is CheckerSetListResult.Failure -> {
 *         println("获取失败: ${result.message}")
 *     }
 * }
 *
 * // 选择规则集
 * checkerService.selectCheckerSets(listOf("golang_standard", "security_basic"))
 *
 * // 保存配置到本地
 * checkerService.saveSelectedCheckerSets(listOf("golang_standard"))
 *
 * // 加载本地配置
 * val savedSets = checkerService.loadSelectedCheckerSets()
 * ```
 *
 * @since 1.0
 */
package com.codecc.preci.service.checker


