package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.dao.ToolMetaCacheServiceImpl
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CommonStatisticDao
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintStatisticDao
import com.tencent.bk.codecc.defect.model.statistic.StatisticEntity
import com.tencent.bk.codecc.defect.service.ClusterDefectService
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService
import com.tencent.bk.codecc.defect.service.TaskLogService
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.clusterresult.BaseClusterResultVO
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants.ToolType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component


@Component
class ClusterDefectServiceImpl @Autowired constructor(
    private var applicationContext: ApplicationContext,
    private val client: Client,
    private val taskLogService: TaskLogService,
    private val toolMetaCacheServiceImpl: ToolMetaCacheServiceImpl,
    private val checkerSetQueryBizService: ICheckerSetQueryBizService,
    private val checkerRepository: CheckerRepository,
    private val lintStatisticDao: LintStatisticDao,
    private val commonStatisticDao: CommonStatisticDao,
    private val commonDefectMigrationService: CommonDefectMigrationService
) : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * 以工具类型为维度，聚类告警
     * 聚类逻辑执行需要满足几个前提条件：
     * 1. 所有工具执行完成并成功，通过 getTaskStatus 方法判断工具状态，非成功状态不执行聚类
     * 2. 聚类逻辑必须只执行一次，现有逻辑是由最后一个完成的工具去执行，由 getTaskStatus 方法实现
     * 如果聚类逻辑执行多次，会造成聚类数据不准确
     *
     * @param taskId
     * @param buildId
     * @param toolName
     */
    fun cluster(taskId: Long, buildId: String, toolName: String) {
        logger.info("trigger cluster begin, $taskId, $buildId, $toolName")

        val taskLogVOList = taskLogService.getCurrBuildInfo(taskId, buildId)
        val toolNameList = taskLogVOList.map { x -> x.toolName }
        var toolNameToDimensionStatisticMap: Map<String, StatisticEntity> = mapOf()
        val dimensionToToolMap: Map<String, List<String>>
        val isMigrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId)

        if (isMigrationSuccessful) {
            dimensionToToolMap = getDimensionToToolMap(taskId, toolNameList)
            if (dimensionToToolMap.isEmpty()) {
                logger.info("trigger cluster dimensionToToolMap is empty, task id: $taskId, build id: $buildId")
                return
            }

            val statisticList = mutableListOf<StatisticEntity>()
            statisticList.addAll(lintStatisticDao.getLatestStatisticForCluster(taskId, toolNameList, buildId))
            statisticList.addAll(commonStatisticDao.getLatestStatisticForCluster(taskId, toolNameList, buildId))
            toolNameToDimensionStatisticMap = statisticList.associateBy { it.toolName }
        } else {
            dimensionToToolMap = toolNameList.groupBy { y -> toolMetaCacheServiceImpl.getToolBaseMetaCache(y).type }
        }

        logger.info("trigger cluster dimensionToToolMap: $dimensionToToolMap")

        val clusterBeans = applicationContext.getBeansOfType(ClusterDefectService::class.java)
        dimensionToToolMap.entries.forEach { (dimension, toolNames) ->
            clusterBeans[dimension]?.cluster(
                taskId = taskId,
                buildId = buildId,
                toolList = toolNames,
                isMigrationSuccessful = isMigrationSuccessful,
                toolNameToDimensionStatisticMap = toolNameToDimensionStatisticMap
            )
        }
    }

    /**
     * 获取维度与工具映射
     */
    private fun getDimensionToToolMap(taskId: Long, toolNameList: List<String>): Map<String, List<String>> {
        val projectId = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)?.data?.projectId
        if (projectId.isNullOrBlank()) {
            logger.info("trigger cluster, project id is null, task id: $taskId")
            return mapOf()
        }

        val checkerKeyList = checkerSetQueryBizService.getTaskCheckerSets(projectId, taskId, toolNameList)
                ?.flatMap { x -> (x.checkerProps ?: listOf()).map { y -> y.checkerKey } }

        return checkerRepository.findClusterFieldByToolNameInAndCheckerKeyIn(toolNameList, checkerKeyList)
                .distinctBy { "${it.checkerCategory}_${it.toolName}" }
                .groupBy({ convertToDimension(it.checkerCategory) }, { it.toolName })
    }

    /**
     * 获取指定构建的聚类信息，用于前端概览页面展示
     *
     * @param taskId
     * @param buildId
     */
    fun getClusterStatistic(taskId: Long, buildId: String): List<BaseClusterResultVO> {
        logger.info("get cluster statistic: $taskId $buildId")

        // 通过接口实现类遍历拿到所有工具的聚类信息
        val clusterBeans = applicationContext.getBeansOfType(ClusterDefectService::class.java)
        val clusterResultVOList = mutableListOf<BaseClusterResultVO>()
        clusterBeans.forEach { (_, clusterService) ->
            clusterResultVOList.add(clusterService.getClusterStatistic(taskId, buildId))
        }

        return clusterResultVOList
    }

    /**
     * 规则标签转换为通用维度标签
     */
    fun convertToDimension(checkerCategory: String): String {
        return when (checkerCategory) {
            CheckerCategory.CODE_DEFECT.name -> ToolType.DEFECT.name
            CheckerCategory.CODE_FORMAT.name -> ToolType.STANDARD.name
            CheckerCategory.SECURITY_RISK.name -> ToolType.SECURITY.name
            CheckerCategory.DUPLICATE.name -> ToolType.DUPC.name
            CheckerCategory.COMPLEXITY.name -> ToolType.CCN.name
            // mock: no match bean name
            else -> "OTHERS"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterDefectServiceImpl::class.java)
    }
}
