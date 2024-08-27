package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.cluster.ClusterCommitIdCompareProcess
import com.tencent.bk.codecc.defect.cluster.ClusterLintCompareProcess
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildStackRepository
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectNewInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.bk.codecc.defect.service.DefectFilePathClusterService
import com.tencent.bk.codecc.defect.service.impl.LintFilterPathBizServiceImpl
import com.tencent.bk.codecc.defect.vo.CommitDefectVO
import com.tencent.bk.codecc.task.vo.FilterPathInputVO
import com.tencent.devops.common.constant.ComConstants
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

@Component
class CommitIdDefectCommitComponent @Autowired constructor(
    lintDefectV2Dao: LintDefectV2Dao,
    clusterLintCompareProcess: ClusterLintCompareProcess,
    private val clusterCommitIdCompareProcess: ClusterCommitIdCompareProcess,
    newLintDefectTracingComponent: NewLintDefectTracingComponent,
    scmJsonComponent: ScmJsonComponent,
    defectFilePathClusterService: DefectFilePathClusterService,
    private val defectIdGenerator: DefectIdGenerator,
    private val toolBuildStackRepository: ToolBuildStackRepository,
    private val lintFilterPathBizServiceImpl: LintFilterPathBizServiceImpl,
    private val redisTemplate: RedisTemplate<String,String>
) : LintDefectCommitComponent(
    lintDefectV2Dao,
    clusterLintCompareProcess,
    newLintDefectTracingComponent,
    redisTemplate,
    defectFilePathClusterService,
    scmJsonComponent
) {
    override fun preHandleDefectList(
        streamName: String,
        toolName: String,
        buildId: String,
        currentDefectList: List<LintDefectV2Entity>,
        preDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<LintDefectV2Entity> {
        val finalDefectList = mutableListOf<LintDefectV2Entity>()
        //1.先处理本次上报的告警
        currentDefectList.forEach {
            it.entityId = ObjectId.get().toString()
            it.newDefect = true
            it.lineUpdateTime = 1000 * it.lineUpdateTime
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }
        finalDefectList.addAll(currentDefectList)
        //2.再处理原有的告警
        preDefectList.forEach {
            it.newDefect = false
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }
        finalDefectList.addAll(preDefectList)
        return finalDefectList
    }

    override fun clusterMethod(
        inputDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): CopyOnWriteArrayList<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        return clusterCommitIdCompareProcess.clusterMethod(inputDefectList, mutableMapOf())
    }

    override fun saveDefectFile(
        commitDefectVO: CommitDefectVO,
        aggregateDefectNewInputModel: AggregateDefectNewInputModel<LintDefectV2Entity>,
        buildEntity: BuildEntity?,
        upsertDefectList: List<LintDefectV2Entity>
    ) {
        with(commitDefectVO) {
            val beginTime = System.currentTimeMillis()
            logger.info("begin saveDefectFile: taskId:{}, toolName:{}, buildId:{}",
                taskId, toolName, buildEntity!!.buildId)

            if (CollectionUtils.isNotEmpty(upsertDefectList)) {
                val needGenerateDefectIdList: MutableList<LintDefectV2Entity?> = ArrayList()
                val curTime = System.currentTimeMillis()
                upsertDefectList.forEach(Consumer { defect: LintDefectV2Entity ->
                    defect.taskId = taskId
                    defect.toolName = toolName
                    defect.updatedDate = curTime
                    lintFilterPathBizServiceImpl.processBiz(
                        FilterPathInputVO(
                            aggregateDefectNewInputModel.whitePaths,
                            aggregateDefectNewInputModel.filterPaths,
                            defect,
                            curTime
                        ))
                    if (StringUtils.isEmpty(defect.id)) {
                        needGenerateDefectIdList.add(defect)
                    }
                })

                // 初始化告警ID
                if (CollectionUtils.isNotEmpty(needGenerateDefectIdList)) {
                    val increment = needGenerateDefectIdList.size
                    val currMaxId: Long = defectIdGenerator.generateDefectId(
                        taskId,
                        toolName,
                        increment)
                    val currMinIdAtom = AtomicLong(currMaxId - increment + 1)
                    needGenerateDefectIdList.forEach(Consumer { defect: LintDefectV2Entity? ->
                        defect!!.id = currMinIdAtom.getAndIncrement().toString() })
                }
                logger.info("save defect trace result: taskId:{}, toolName:{}, buildId:{}," +
                    " defectCount:{}", taskId, toolName, buildEntity.buildId, upsertDefectList.size)
                lintDefectV2Dao.saveAll(upsertDefectList)
            }
            logger.info("end saveDefectFile, cost:{}, taskId:{}, toolName:{}, buildId:{}",
                System.currentTimeMillis() - beginTime, taskId, toolName, buildEntity.buildId)
        }
    }

    override fun getPreDefectList(
        defectClusterDTO: DefectClusterDTO,
        relPathSet: Set<String>?,
        filePathSet: Set<String>?
    ): List<LintDefectV2Entity> {
        val taskId = defectClusterDTO.commitDefectVO.taskId
        val toolName = defectClusterDTO.commitDefectVO.toolName
        val buildId = defectClusterDTO.commitDefectVO.buildId
        val finalDefects = mutableListOf<LintDefectV2Entity>()
        val toolBuildStack = toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(
            taskId, toolName, buildId
        )

        logger.info("commit defect component getPreDefectList: $taskId $toolName $buildId $toolBuildStack")

        // 获取文件集合中增量时间之后的告警
        finalDefects.addAll(
            lintDefectV2Dao.findByTaskIdAndToolNameAndPathAndLineUpdateTime(
                taskId, toolName, relPathSet, filePathSet, toolBuildStack?.commitSince ?: 0L
            )
        )

        return finalDefects
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommitIdDefectCommitComponent::class.java)
    }
}
