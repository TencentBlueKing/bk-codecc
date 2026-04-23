package com.codecc.preci.service.vcs

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * VCS 检查服务接口
 *
 * 提供 Git 钩子（pre-commit / pre-push）触发的代码检查功能。
 * 整合扫描触发、进度轮询、结果获取的完整流程，供 [PreCICheckinHandlerFactory]
 * 和 [PreCIPrePushHandler] 调用。
 *
 * **完整检查流程：**
 * 1. 读取用户设置，确定是否启用以及扫描范围（变更文件 / 全部文件）
 * 2. 调用 `POST /task/scan` 发起扫描
 * 3. 轮询 `GET /task/scan/progress` 等待扫描完成
 * 4. 调用 `POST /task/scan/result` 获取缺陷列表
 * 5. 根据缺陷数量返回 [VcsCheckResult]
 *
 * @since 1.1
 */
@Service(Service.Level.PROJECT)
interface VcsCheckService {

    /**
     * 执行 pre-commit 检查
     *
     * 根据用户配置的扫描范围（变更文件 scanType=102 或全部文件 scanType=0），
     * 触发扫描并等待结果。
     *
     * @return 检查结果，调用方据此决定是否阻止 commit
     */
    suspend fun performPreCommitCheck(): VcsCheckResult

    /**
     * 执行 pre-push 检查
     *
     * 根据用户配置的扫描范围（变更文件 scanType=103 或全部文件 scanType=0），
     * 触发扫描并等待结果。
     *
     * @return 检查结果，调用方据此决定是否阻止 push
     */
    suspend fun performPrePushCheck(): VcsCheckResult

    companion object {
        /**
         * 获取 VcsCheckService 实例
         *
         * @param project 当前项目
         * @return VcsCheckService 实例
         */
        @JvmStatic
        fun getInstance(project: Project): VcsCheckService {
            return project.getService(VcsCheckService::class.java)
        }
    }
}
