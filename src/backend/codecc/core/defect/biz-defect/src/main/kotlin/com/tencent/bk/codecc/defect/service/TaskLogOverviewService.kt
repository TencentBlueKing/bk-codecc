package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import org.springframework.data.domain.PageImpl

interface TaskLogOverviewService {
    /**
     * 保存本次构建实际应该执行的有哪些工具
     * 通过插件在过滤完开源扫描的编译型工具后上报到后台保存
     *
     * @param taskLogOverviewVO
     */
    fun saveActualExeTools(taskLogOverviewVO: TaskLogOverviewVO): Boolean

    /**
     * 获取当前扫描实际需要执行的工具
     * 用户开源扫描过滤编译型工具时的度量分数计算
     *
     * @param taskId
     * @param buildId
     */
    fun getActualExeTools(taskId: Long, buildId: String): List<String>?

    /**
     * 获取上一次成功扫描实际执行的工具
     *
     * @param taskId
     */
    fun getLastAnalyzeTool(pipelineId: String, multiPipelineMark: String?): List<String>

    /**
     * 获取上一次成功扫描实际执行的工具
     *
     * @param taskId
     */
    fun getLastAnalyzeTool(taskId: Long): List<String>

    /**
     * 计算任务状态，根据taskLog中的工具状态进行判断
     * 每次执行都更新 taskLog list
     * 任务最后成功后 task log list中的工具与 toolList 对齐
     *
     * @param uploadTaskLogStepVO 上报请求体
     */
    fun calTaskStatus(uploadTaskLogStepVO: UploadTaskLogStepVO)

    /**
     * 获取任务维度分析记录，只返回构建号，用于概览页面查找本次任务最后一次成功的构建
     *
     * @param taskId
     * @param buildId
     * @param status
     */
    fun getTaskLogOverview(taskId: Long, buildId: String?, status: Int?): TaskLogOverviewVO?

    /**
     * 获取任务维度分析记录
     *
     * @param taskId
     * @param buildId
     * @param status
     */
    fun getTaskLogOverview(taskId: Long, buildId: String): TaskLogOverviewVO?

    /**
     * 批量获取当前任务构建记录
     * 用于任务分析就页面，需要带上工具分析记录一起展示
     *
     * @param taskId
     * @param page
     * @param pageSize
     */
    fun getTaskLogOverviewList(taskId: Long, page: Int?, pageSize: Int?): PageImpl<TaskLogOverviewVO>

    /**
     * 获取分析记录以及对应的分析结果
     * 根据 QueryData 的积类型表示查询条件，预留四种查询场景，有需要可以在 companion 中添加
     *
     * @param taskId
     * @param buildId
     * @param buildNum
     * @param status
     */
    fun getAnalyzeResult(taskId: Long, buildId: String?, buildNum: String?, status: Int?): TaskLogOverviewVO?

    /**
     * 统计任务分析次数
     *
     * @param taskIds
     * @param status
     * @param startTime
     * @param endTime
     */
    fun statTaskAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?, endTime: Long?): Int

    /**
     * 取指定工具集中每个工具的最后一次执行成功时间点
     *
     * @param taskId
     * @param toolNameSet
     */
    fun getLatestTime(taskId: Long, toolNameSet: MutableList<String>): MutableMap<String, Long>

    /**
     * 通过tasklog信息拿代码仓库信息
     *
     * @param taskIdList
     */
    fun batchGetRepoInfo(taskIdList: List<Long>): Map<Long, Map<String, TaskLogRepoInfoVO>>

    /**
     * 通过任务ID获取最后的构建id
     */
    fun getLastAnalyzeBuildIdMap(taskIdToBuildIds: Map<Long, Set<String>>): Map<Long, String>

    /**
     * 上报插件错误信息
     */
    fun reportPluginErrorInfo(taskId: Long, buildId: String, errorCode: Int?, errorType: Int?)
}
