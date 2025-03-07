package com.tencent.bk.codecc.defect.resources

import com.tencent.bk.codecc.defect.api.DefectSuggestionsRestResource
import com.tencent.bk.codecc.defect.service.DefectSuggestionService
import com.tencent.bk.codecc.defect.vo.DefectSuggestionEvaluateVO
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.web.RestResource
import lombok.AllArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Controller

@RestResource
@Controller
@AllArgsConstructor
class DefectSuggestionsResourceImpl @Autowired constructor(
    val defectSuggestionService: DefectSuggestionService
) : DefectSuggestionsRestResource {

    override fun defectSuggestion(
        @Header(AUTH_HEADER_DEVOPS_BK_TICKET)
        bkTicket: String?,
        @Header(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String?,
        @Header(AUTH_HEADER_DEVOPS_TASK_ID)
        taskId: String?,
        @Header(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String?,
        request: CommonDefectDetailQueryReqVO
    ): Result<String?> {
        logger.info(
            "enter defect suggestion projectId:$projectId  taskId:$taskId userId:$userId " +
                    "entityId:${request.entityId}"
        )
        val content = defectSuggestionService.defectSuggestionHandle(
            bkTicket, projectId, taskId, userId, request
        )
        return Result(content)
    }

    override fun defectSuggestionEvaluateHandle(
        request: DefectSuggestionEvaluateVO
    ): Result<Boolean> {
        val result = defectSuggestionService.defectSuggestionEvaluate(request)
        return Result(result)
    }

    override fun defectSuggestionEvaluateList(
        defectId: String
    ): Result<DefectSuggestionEvaluateVO> {
        val result = defectSuggestionService.getDefectSuggestionEvaluate(defectId)
        return Result(result)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefectSuggestionsResourceImpl::class.java)
    }
}
