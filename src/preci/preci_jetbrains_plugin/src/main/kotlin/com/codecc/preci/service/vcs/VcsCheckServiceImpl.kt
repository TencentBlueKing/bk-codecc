package com.codecc.preci.service.vcs

import com.codecc.preci.core.config.PreCISettings
import com.codecc.preci.core.log.PreCILogger
import com.codecc.preci.service.scan.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay

/**
 * VCS 检查服务实现
 *
 * 实现 [VcsCheckService] 接口，封装「扫描 → 轮询进度 → 获取结果」的完整流程。
 *
 * **设计要点：**
 * - 任何异常均不阻塞用户操作，返回 [VcsCheckResult.Error] 后由调用方放行
 * - 扫描进度轮询设有超时保护（默认 5 分钟），超时后自动放行
 * - 日志完整记录每个步骤，便于问题排查
 *
 * @property project 当前项目
 * @since 1.1
 */
class VcsCheckServiceImpl(private val project: Project) : VcsCheckService {

    private val logger = PreCILogger.getLogger(VcsCheckServiceImpl::class.java)

    companion object {
        /** 进度轮询间隔（毫秒） */
        private const val POLL_INTERVAL_MS = 2000L

        /** 扫描超时时间（毫秒），5 分钟 */
        private const val SCAN_TIMEOUT_MS = 5 * 60 * 1000L

        /** 扫描完成状态 */
        private const val STATUS_DONE = "done"
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun performPreCommitCheck(): VcsCheckResult {
        val settings = PreCISettings.getInstance()

        if (!settings.preCommitCheckEnabled) {
            logger.info("Pre-Commit 检查未启用，跳过")
            return VcsCheckResult.Disabled
        }

        val scanType = settings.preCommitScanScope
        logger.info("开始 Pre-Commit 检查，scanType=$scanType")

        return try {
            performCheck(scanType, "Pre-Commit")
        } catch (e: Exception) {
            logger.error("Pre-Commit 检查异常: ${e.message}", e)
            VcsCheckResult.Error("Pre-Commit 检查异常: ${e.message}", e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun performPrePushCheck(): VcsCheckResult {
        val settings = PreCISettings.getInstance()

        if (!settings.prePushCheckEnabled) {
            logger.info("Pre-Push 检查未启用，跳过")
            return VcsCheckResult.Disabled
        }

        val scanType = settings.prePushScanScope
        logger.info("开始 Pre-Push 检查，scanType=$scanType")

        return try {
            performCheck(scanType, "Pre-Push")
        } catch (e: Exception) {
            logger.error("Pre-Push 检查异常: ${e.message}", e)
            VcsCheckResult.Error("Pre-Push 检查异常: ${e.message}", e)
        }
    }

    /**
     * 执行检查的核心流程：触发扫描 → 轮询进度 → 获取结果
     *
     * @param scanType 扫描类型（0=全量, 102=pre-commit变更, 103=pre-push变更）
     * @param label 日志标签（Pre-Commit / Pre-Push）
     * @return 检查结果
     */
    private suspend fun performCheck(scanType: Int, label: String): VcsCheckResult {
        val scanService = ScanService.getInstance(project)

        // 1. 触发扫描
        val scanResult = when (scanType) {
            GitHookScanScope.ALL_FILES.scanType -> scanService.fullScan()
            GitHookScanScope.CHANGED_FILES_COMMIT.scanType -> scanService.preCommitScan()
            GitHookScanScope.CHANGED_FILES_PUSH.scanType -> scanService.prePushScan()
            else -> {
                logger.error("未知的 scanType: $scanType")
                return VcsCheckResult.Error("未知的扫描类型: $scanType")
            }
        }

        if (scanResult is ScanResult.Failure) {
            logger.error("$label 扫描启动失败: ${scanResult.message}")
            return VcsCheckResult.Error("扫描启动失败: ${scanResult.message}", scanResult.exception)
        }

        logger.info("$label 扫描已启动，开始轮询进度")

        // 2. 轮询进度
        val startTime = System.currentTimeMillis()
        while (true) {
            if (System.currentTimeMillis() - startTime > SCAN_TIMEOUT_MS) {
                logger.warn("$label 扫描超时（${SCAN_TIMEOUT_MS / 1000}秒），放行")
                return VcsCheckResult.Error("扫描超时，已自动放行")
            }

            delay(POLL_INTERVAL_MS)

            val progressResult = scanService.getScanProgress()
            if (progressResult is ScanProgressResult.Failure) {
                logger.error("$label 进度查询失败: ${progressResult.message}")
                return VcsCheckResult.Error("进度查询失败: ${progressResult.message}", progressResult.exception)
            }

            val progress = (progressResult as ScanProgressResult.Success).response
            if (progress.status == STATUS_DONE) {
                logger.info("$label 扫描完成")
                break
            }

            logger.info("$label 扫描进行中: status=${progress.status}, tools=${progress.toolStatuses}")
        }

        // 3. 获取结果
        val resultQuery = scanService.getScanResult()
        if (resultQuery is ScanResultQueryResult.Failure) {
            logger.error("$label 结果查询失败: ${resultQuery.message}")
            return VcsCheckResult.Error("结果查询失败: ${resultQuery.message}", resultQuery.exception)
        }

        val defects = (resultQuery as ScanResultQueryResult.Success).response.getDefectList()
        return if (defects.isEmpty()) {
            logger.info("$label 检查通过，无缺陷")
            VcsCheckResult.NoDefects("$label 检查通过，未发现代码缺陷")
        } else {
            logger.info("$label 检查发现 ${defects.size} 个缺陷")
            VcsCheckResult.HasDefects(defectCount = defects.size, defects = defects)
        }
    }
}
