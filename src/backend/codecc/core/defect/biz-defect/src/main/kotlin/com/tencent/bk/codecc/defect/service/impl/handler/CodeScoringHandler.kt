package com.tencent.bk.codecc.defect.service.impl.handler

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity
import com.tencent.bk.codecc.defect.pojo.HandlerDTO
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService
import com.tencent.bk.codecc.defect.service.IHandler
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.BaseDataVO
import com.tencent.devops.common.api.OpenSourceCheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.BaseDataCacheService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.stream.Collectors

/**
 * 代码算分
 */
@Service
class CodeScoringHandler @Autowired constructor(
    private val client: Client,
    private val handler: ClusterHandler,
    private val applicationContext: ApplicationContext,
    private val baseDataCacheService: BaseDataCacheService,
    private val checkerSetTaskRelationshipRepository: CheckerSetTaskRelationshipRepository
) : IHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(CodeScoringHandler::class.java)
    }

    override fun handler(handlerDTO: HandlerDTO) {
        try {
            if (handlerDTO.scanStatus == ComConstants.ScanStatus.SUCCESS) {
                val taskDetailVO = getTaskDetail(handlerDTO.taskId)
                val codeScoringService: AbstractCodeScoringService = applicationContext.getBean(
                    getScoringServiceName(taskDetailVO),
                    AbstractCodeScoringService::class.java
                )
                codeScoringService.scoring(
                    taskDetailVO,
                    buildId = handlerDTO.buildId,
                    toolName = handlerDTO.toolName
                )
            }
        } catch (e: Throwable) {
            logger.error(
                "code scoring fail! ${handlerDTO.taskId}, ${handlerDTO.toolName}, ${handlerDTO.buildId}", e
            )
        } finally {
            handler.handler(handlerDTO)
        }
    }

    private fun getScoringServiceName(taskDetailVO: TaskDetailVO): String {
        val isOpenScan = isOpenScan(taskDetailVO.taskId, taskDetailVO.codeLang)
        return if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
                        .equals(taskDetailVO.createFrom, ignoreCase = true) || isOpenScan
        ) {
            "TStandard"
        } else {
            "Custom"
        }
    }

    /**
     * 判断当前度量计算的环境是否符合开源扫描的场景
     * 规则集是否符合开源扫描规则集要求
     * 从缓存中根据当前项目语言获取相应的全量规则集信息与当前 Task 的规则集比对
     *
     * @param taskId
     * @param codeLang
     */
    private fun isOpenScan(taskId: Long, codeLang: Long): Boolean {
        val checkerSetTaskRelationshipEntityList: List<CheckerSetTaskRelationshipEntity> =
            checkerSetTaskRelationshipRepository.findByTaskId(taskId)
        val baseDataVOList: List<BaseDataVO> = baseDataCacheService.getLanguageBaseDataFromCache(codeLang)
        // 过滤 OTHERS 的开源规则集
        val openSourceCheckerSet = baseDataVOList.stream()
                .filter { baseDataVO: BaseDataVO ->
                    ComConstants.CodeLang.OTHERS.langName() != baseDataVO.langFullKey
                }.flatMap { baseDataVO: BaseDataVO ->
                    baseDataVO.openSourceCheckerListVO.stream()
                            .filter { openSourceCheckerSetVO: OpenSourceCheckerSetVO ->
                                (openSourceCheckerSetVO.checkerSetType == null ||
                                        "FULL" == openSourceCheckerSetVO.checkerSetType)
                            }.map { obj: OpenSourceCheckerSetVO -> obj.checkerSetId }
                }.collect(Collectors.toSet())
        val checkerSetIdSet = checkerSetTaskRelationshipEntityList.stream()
                .map { obj: CheckerSetTaskRelationshipEntity -> obj.checkerSetId }
                .collect(Collectors.toSet())
        return checkerSetIdSet.containsAll(openSourceCheckerSet)
    }

    private fun getTaskDetail(taskId: Long): TaskDetailVO {
        val result: Result<TaskDetailVO?> = client.get(ServiceTaskRestResource::class.java)
                .getTaskInfoById(taskId)

        if (result.isNotOk() || result.data == null) {
            throw CodeCCException(TaskMessageCode.TASK_NOT_FOUND)
        }

        return result.data!!
    }
}
