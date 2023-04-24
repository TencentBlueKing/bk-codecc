package com.tencent.bk.codecc.defect.component

import com.tencent.bk.codecc.defect.cluster.ClusterCompareProcess
import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectCommitComponent
import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.service.annotation.cluster_type.BYCOMMITID
import com.tencent.devops.common.service.annotation.cluster_type.BYDEFAULT
import com.tencent.devops.common.service.annotation.cluster_type.BYDESCRIPTION
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@ExperimentalUnsignedTypes
@Component
class DefectClusterComponent @Autowired constructor(
    private val toolMetaCacheService: ToolMetaCacheService,
    private val commitIdDefectCommitComponent: CommitIdDefectCommitComponent,
    private val descriptionDefectCommitComponent: DescriptionDefectCommitComponent,
    private val scmJsonComponent: ScmJsonComponent
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DefectClusterComponent::class.java)
    }

    @ExperimentalUnsignedTypes
    fun executeCluster(aggregateDispatchFileName: AggregateDispatchFileName): Boolean {

        return try {
            logger.info("start to execute cluster! input file: ${aggregateDispatchFileName.inputFileName}, output file ${aggregateDispatchFileName.outputFileName}")
            /*val result = asyncExecuteUnixCommand(
                "./pp-cluster --input ${aggregateDispatchFileName.inputFileName} --output ${aggregateDispatchFileName.outputFileName} --pretty",
                File("/opt"), null
            )
            logger.info("execute cluster finish! result : $result")*/
            scmJsonComponent.index(aggregateDispatchFileName.inputFileName, ScmJsonComponent.AGGREGATE)
            ClusterCompareProcess.clusterMethod(aggregateDispatchFileName)
            val outputFile = File(aggregateDispatchFileName.outputFilePath)
            //排除读延时因素
            var i = 0
            while (!outputFile.exists() && i < 3) {
                logger.info("waiting for generating output file")
                Thread.sleep(2000L)
                i++
            }
            if (outputFile.exists()) {
                scmJsonComponent.upload(
                    aggregateDispatchFileName.outputFilePath,
                    aggregateDispatchFileName.outputFileName, ScmJsonComponent.AGGREGATE
                )
                return true
            }
            return outputFile.exists()
        } catch (t: Throwable) {
            logger.error("execute cluster fail! error : ${t.message}", t)
            false
        }
    }


    @ExperimentalUnsignedTypes
    fun executeClusterNew(defectClusterDTO: DefectClusterDTO): Boolean {
        var commonLog = ""

        return try {
            val taskId = defectClusterDTO.commitDefectVO.taskId
            val toolName = defectClusterDTO.commitDefectVO.toolName
            val buildId = defectClusterDTO.commitDefectVO.buildId
            commonLog = "task id: $taskId, tool name: $toolName, build id: $buildId"

            val clusterType = toolMetaCacheService.getClusterType(
                defectClusterDTO.commitDefectVO.toolName
            ) ?: BYDEFAULT::class.java.name
            logger.info("cluster exec begin, $commonLog, cluster type: $clusterType")

            val beginTime = System.currentTimeMillis()
            this::class.java.declaredMethods.find { method ->
                method.declaredAnnotations.find {
                    it.annotationClass.simpleName == clusterType
                } != null
            }.let {
                if (it == null) {
                    executeDefaultCluster(defectClusterDTO)
                } else {
                    it.invoke(this, defectClusterDTO)
                }
            }
            logger.info("cluster exec end, $commonLog, cost: ${System.currentTimeMillis() - beginTime}")

            true
        } catch (t: Throwable) {
            logger.error(
                "cluster exec fail! $commonLog, input file path: ${defectClusterDTO.inputFilePath}", t
            )

            false
        }
    }

    @BYDEFAULT
    private fun executeDefaultCluster(defectClusterDTO: DefectClusterDTO) {
        val toolPattern = toolMetaCacheService.getToolPattern(
            defectClusterDTO.commitDefectVO.toolName
        )
        val processComponent = SpringContextUtil.getBean(
            AbstractDefectCommitComponent::class.java,
            "${toolPattern}DefectCommitComponent"
        )
        processComponent.processCluster(defectClusterDTO)
    }

    @BYCOMMITID
    private fun executeClusterByCommitId(defectClusterDTO: DefectClusterDTO) {
        commitIdDefectCommitComponent.processCluster(defectClusterDTO)
    }

    @BYDESCRIPTION
    private fun executeClusterByDescription(defectClusterDTO: DefectClusterDTO) {
        descriptionDefectCommitComponent.processCluster(defectClusterDTO)
    }
}
