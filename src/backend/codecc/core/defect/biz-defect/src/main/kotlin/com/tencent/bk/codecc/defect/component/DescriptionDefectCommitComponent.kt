package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.cluster.ClusterDescriptionCompareProcess
import com.tencent.bk.codecc.defect.cluster.ClusterLintCompareProcess
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.devops.common.constant.ComConstants
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DescriptionDefectCommitComponent(
    lintDefectV2Repository: LintDefectV2Repository,
    clusterLintCompareProcess: ClusterLintCompareProcess,
    newLintDefectTracingComponent: NewLintDefectTracingComponent,
    scmJsonComponent: ScmJsonComponent,
    private val clusterDescriptionCompareProcess: ClusterDescriptionCompareProcess
) : LintDefectCommitComponent(
    lintDefectV2Repository,
    clusterLintCompareProcess,
    newLintDefectTracingComponent,
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

    override fun postHandleDefectList(
        outputDefectList: List<AggregateDefectOutputModelV2<LintDefectV2Entity>>,
        buildEntity: BuildEntity?,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ): List<LintDefectV2Entity> {
        if (outputDefectList.isEmpty()) {
            logger.info("description output defect list is empty! build id: ${buildEntity?.buildId}")
            return emptyList()
        }
        val upsertDefectList = mutableListOf<LintDefectV2Entity>()
        outputDefectList.forEach {
            val partitionedDefects = it.defects.partition { defect -> defect.newDefect }
            val newDefects = partitionedDefects.first
            val oldDefects = partitionedDefects.second
            if (newDefects.isEmpty()) {
                logger.info("description cluster newDefects: ${buildEntity?.buildId} ${newDefects.size}˚")
                oldDefects.forEach { oldDefect ->
                    if (oldDefect.status == ComConstants.DefectStatus.NEW.value()) {
                        fixDefect(oldDefect, buildEntity)
                        upsertDefectList.add(oldDefect)
                    }
                }
            } else {
                val oldDefectsMap = oldDefects.groupBy (LintDefectV2Entity::getPinpointHash).toMutableMap()
                newDefects.forEach loop@{ newDefect ->
                    val oldDefect = oldDefectsMap[newDefect.pinpointHash]
                    if (oldDefect.isNullOrEmpty()) {
                        newDefect.status = ComConstants.DefectStatus.NEW.value()
                        upsertDefectList.add(newDefect)
                        return@loop
                    }
                    updateOldDefectInfo(newDefect, oldDefect.first(), transferAuthorList, buildEntity)
                    if (oldDefect.first().status and ComConstants.DefectStatus.FIXED.value() > 0) {
                        reopenDefect(oldDefect.first())
                    }
                    if (oldDefect.first().author.isNullOrEmpty()) {
                        oldDefect.first().author = newDefect.author
                    }
                    upsertDefectList.add(oldDefect.first())
                    oldDefectsMap.remove(newDefect.pinpointHash)
                }
                oldDefectsMap.values
                    .flatMap { list -> list.asIterable() }
                    .forEach { fixDefect ->
                        fixDefect(fixDefect, buildEntity)
                        upsertDefectList.add(fixDefect)
                    }
            }
        }
        return upsertDefectList
    }

    @ExperimentalUnsignedTypes
    override fun clusterMethod(
        inputDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        return clusterDescriptionCompareProcess.clusterMethod(inputDefectList, md5SameMap)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DescriptionDefectCommitComponent::class.java)
    }
}
