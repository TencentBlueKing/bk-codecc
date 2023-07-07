package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource
import com.tencent.bk.codecc.defect.api.ServicePkgDefectRestResource
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqOldVO
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO
import com.tencent.bk.codecc.openapi.v2.ApigwDefectResourceV2
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
open class ApigwDefectResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwDefectResourceV2 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDefectResourceV2Impl::class.java)
    }

    override fun getTasksByAuthor(
        reqVO: QueryMyTasksReqVO
    ): Result<Page<TaskInfoVO>> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getTasksByAuthor(
            reqVO
        )
    }

    override fun queryCodeLineInfo(
        taskId: Long,
        toolName: String
    ): Result<ToolClocRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryCodeLine(taskId, toolName)
    }

    override fun getPipelineTask(pipelineId: String, multiPipelineMark: String?, user: String?): Result<PipelineTaskVO> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getPipelineTask(pipelineId, multiPipelineMark, user)
    }

    override fun getPipelineAllTask(pipelineId: String, user: String?): Result<List<PipelineTaskVO>> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getPipelineAllTask(pipelineId, user)
    }

    override fun authorTransfer(
        apigw: String,
        taskId: Long,
        projectId: String,
        appCode: String,
        batchDefectProcessReqOldVO: BatchDefectProcessReqOldVO,
        userId: String
    ): Result<Boolean> {
        logger.info("start to author transfer!! task id: $taskId, project id: $projectId")
        val batchDefectProcessReqVO = batchDefectProcessReqOldVO.toBatchDefectProcessReqVO()
        batchDefectProcessReqVO.bizType = ComConstants.BusinessType.ASSIGN_DEFECT.value()
        batchDefectProcessReqVO.projectId = projectId
        return client.getWithoutRetry(ServiceDefectRestResource::class)
                .batchDefectProcess(taskId, userId, batchDefectProcessReqVO)
    }
}
