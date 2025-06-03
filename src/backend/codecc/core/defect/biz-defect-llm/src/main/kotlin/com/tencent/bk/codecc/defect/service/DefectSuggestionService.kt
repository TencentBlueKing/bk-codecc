package com.tencent.bk.codecc.defect.service

import com.google.common.collect.Lists
import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.dao.defect.DefectSuggestionRecordRepository
import com.tencent.bk.codecc.defect.llm.api.ChatGLM2Api
import com.tencent.bk.codecc.defect.llm.api.QpilotApi
import com.tencent.bk.codecc.defect.llm.api.BkAIDevApi
import com.tencent.bk.codecc.defect.llm.api.ChatGPTApi
import com.tencent.bk.codecc.defect.llm.api.HunYuanApi
import com.tencent.bk.codecc.defect.llm.api.LLMApi
import com.tencent.bk.codecc.defect.llm.api.LLMOpenAIApi
import com.tencent.bk.codecc.defect.llm.handle.BkAIDevResultHandle
import com.tencent.bk.codecc.defect.llm.handle.ChatGLM2ResultHandle
import com.tencent.bk.codecc.defect.llm.handle.ChatGPTResultHandle
import com.tencent.bk.codecc.defect.llm.handle.HunYuanResultHandle
import com.tencent.bk.codecc.defect.llm.handle.LLMResultHandle
import com.tencent.bk.codecc.defect.llm.handle.QpilotResultHandle
import com.tencent.bk.codecc.defect.model.DefectSuggestionRecordEntity
import com.tencent.bk.codecc.defect.utils.FileUtils
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.bk.codecc.defect.vo.CCNDefectDetailQueryRspVO
import com.tencent.bk.codecc.defect.vo.DefectSuggestionEvaluateVO
import com.tencent.bk.codecc.defect.vo.DefectSuggestionRecordVO
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryRspVO
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.BizServiceFactory
import com.tencent.devops.common.util.BeanUtils
import joptsimple.internal.Strings
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.util.Date

@Controller
class DefectSuggestionService @Autowired constructor(
    private val fileAndDefectQueryFactory: BizServiceFactory<IQueryWarningBizService>,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val defectSuggestionRecordRepository: DefectSuggestionRecordRepository
) {

    @Value("\${codecc.llm.appcode:#{null}}")
    val appCode: String? = null

    @Value("\${codecc.llm.appsecret:#{null}}")
    val appSecret: String? = null

    @Value("\${codecc.llm.bkaidev:#{null}}")
    val bkaidevHost: String? = null

    @Value("\${codecc.llm.model:'hunyuan-turbo'}")
    val model: String = ""

    fun getDefectSuggestionEvaluate(
        defectId: String
    ): DefectSuggestionEvaluateVO {
        val defectEvaluate = DefectSuggestionEvaluateVO(
            defectId = defectId,
            goodEvaluates = mutableListOf(),
            badEvaluates = mutableListOf()
        ).apply {
            defectSuggestionRecordRepository.findFirstByDefectId(defectId)?.let { entity ->
                entity.goodEvaluates?.let { goodEvaluates.addAll(it) }
                entity.badEvaluates?.let { badEvaluates.addAll(it) }
            }
        }
        return defectEvaluate
    }

    fun defectSuggestionEvaluate(
        request: DefectSuggestionEvaluateVO
    ): Boolean {
        val entity = defectSuggestionRecordRepository.findFirstByDefectId(request.defectId)
        entity?.let {
            it.goodEvaluates = request.goodEvaluates
            it.badEvaluates = request.badEvaluates
            defectSuggestionRecordRepository.save(it)
            return true
        }
        return false
    }

    fun defectSuggestionHandle(
        bkTicket: String?,
        projectId: String?,
        taskId: String?,
        userId: String?,
        request: CommonDefectDetailQueryReqVO
    ): String? {

        // 根据传入的缺陷id来查询缺陷建议
        val findResult = findDefectSuggestionsByDefectId(request)
        if (!Strings.isNullOrEmpty(findResult)) {
            logger.info(
                "get defect suggestion from cache projectId:$projectId  taskId:$taskId userId:$userId " +
                        "entityId:${request.entityId}"
            )
            return findResult
        }

        // 生成问答内容
        val apiChatVO = getChatInfo(userId, projectId, taskId, request)

        // 开始问答并获取结果
        val ticketFromUser = if (bkTicket.isNullOrEmpty() &&
                !LLMConstants.BK_TICKET_FOR_USER.get(userId).isNullOrEmpty()
        ) {
            LLMConstants.BK_TICKET_FOR_USER.get(userId)
        } else {
            bkTicket
        }

        // 新版OpenAI客户端接口
        val content = getDefectSuggestionContent(ticketFromUser, apiChatVO, request.entityId)

        // 缓存记录
        val defectSuggestionRecordVO = DefectSuggestionRecordVO(
            createdAfterDate = Date().time,
            defectId = request.entityId,
            llmName = model,
            projectId = projectId,
            taskId = taskId?.toLong(),
            userId = userId,
            stream = request.stream,
            content = content
        )
        val defectSuggestionRecordEntity = DefectSuggestionRecordEntity()
        BeanUtils.copyProperties(defectSuggestionRecordVO, defectSuggestionRecordEntity)
        defectSuggestionRecordRepository.save(defectSuggestionRecordEntity)

        // 缓存问答结果
        return content
    }

    fun findDefectSuggestionsByDefectId(request: CommonDefectDetailQueryReqVO): String? {
        val defectsSuggestionRecordEntityList =
            defectSuggestionRecordRepository.findByDefectId(request.entityId)
        logger.info(
            "get defect suggestion from cache. entityId:${request.entityId} " +
                    "cache size:${defectsSuggestionRecordEntityList?.size ?: 0}"
        )
        // 如果选择不刷新缓存，则从结果中查找对应stream类型的修复建议直接返回
        if (!request.flushCache && !defectsSuggestionRecordEntityList.isNullOrEmpty()) {
            val result = defectsSuggestionRecordEntityList.find {
                request.stream == it?.stream
            }
            result?.content?.let {
                simpMessagingTemplate.convertAndSend(
                    "${LLMConstants.WEBSOCKET_TOPIC_DEFECT_SUGGESTION}/${request.entityId}", it
                )
            }
            return result?.content
        }

        // 如果选刷新缓存，则需要清除缓存再重新调用AI接口
        if (!defectsSuggestionRecordEntityList.isNullOrEmpty()) {
            defectsSuggestionRecordEntityList.forEach {
                it?.let { it1 -> defectSuggestionRecordRepository.delete(it1) }
            }
        }

        return ComConstants.EMPTY_STRING
    }

    fun getChatInfo(
        userId: String?,
        projectId: String?,
        taskId: String?,
        request: CommonDefectDetailQueryReqVO
    ): ApiChatVO {
        logger.info("start to create chat info entityId:${request.entityId}")
        // 调用defect服务，获取告警信息（包括代码内容，告警行，告警描述）
        val service: IQueryWarningBizService = fileAndDefectQueryFactory.createBizService(
            Lists.newArrayList(request.toolName),
            Lists.newArrayList(request.dimension),
            ComConstants.BusinessType.QUERY_WARNING.value(),
            IQueryWarningBizService::class.java
        )
        val defectDetailQueryRspVO =
            service.processQueryWarningDetailRequest(
                projectId, taskId?.toLong(), userId, request, "", Sort.Direction.ASC
            )

        // 获取告警行代码和告警描述
        var chatContent = ""
        var message = ""
        var codeLanguage = ""
        // 当告警类型为lint类型
        (defectDetailQueryRspVO as? LintDefectDetailQueryRspVO)?.lintDefectDetailVO?.let { defect ->
            // 获取文件语言
            codeLanguage = getLanguageByExtension(getFileExtension(defect.filePath))

            // 获取文件内容
            val fileContent = defect.fileInfoMap[defect.fileMd5]?.contents
            if (defect.defectInstances == null) {
                // 单行告警
                message = defect.message
                // 通过告警行，获取行内容
                chatContent = fileContent?.let {
                    FileUtils.getLinesContent(it, defect.lineNum, defect.lineNum)
                }.toString()
            } else {
                // 跨行告警
                val traceList = mutableListOf<Triple<Int, String, Int>>()
                for (instance in defect.defectInstances) {
                    val sortTraces = instance.traces.sortedBy { it.traceNum }
                    sortTraces.forEach {
                        traceList.add(Triple(it.traceNum, it.message, it.lineNum))
                    }
                }
                // 获取跨行告警描述
                message = buildString {
                    traceList.forEach { trace ->
                        val lineContent = fileContent?.let {
                            FileUtils.getLinesContent(it, trace.third, trace.third)
                        }
                        appendln("$lineContent ${trace.first} 存在告警：${trace.second}")
                    }
                }
                // 通过跨行获取内容
                chatContent = fileContent?.let {
                    FileUtils.getLinesContent(it, traceList.first().third, traceList.last().third)
                }.toString()
            }
        }

        // 多行圈复杂度告警
        (defectDetailQueryRspVO as? CCNDefectDetailQueryRspVO)?.defectVO?.let { ccn ->
            // 获取文件语言
            codeLanguage = getLanguageByExtension(getFileExtension(ccn.filePath))

            // 获取文件内容
            val fileContent = defectDetailQueryRspVO.fileContent
            // 通过告警行，获取方法内容
            chatContent = fileContent?.let {
                FileUtils.getLinesContent(it, ccn.startLines, ccn.endLines)
            }.toString()
            // 获取告警描述
            message = String.format("圈复杂度为%s，请进行函数功能拆分以降低代码复杂度。", ccn.ccn)
        }

        // 问答内容：
        var apiChatVO = ApiChatVO(
            userChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_USER_CHAT_PROMPT,
            assistantChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_ASSISTANT_CHAT_PROMPT,
            content = "这是一段使用${codeLanguage}语言编写的代码," +
                    "${LLMConstants.DEFECT_SUGGESTIONS_USER_CHAT_ASK_PROMPT} \n " +
                    "描述：$message \n " +
                    "代码内容：$chatContent",
            stream = request.stream
        )
        logger.info(
            "create chat info end message length: ${message.length} content length:${chatContent.length} " +
                    "entityId:${request.entityId} code language is $codeLanguage"
        )
        return apiChatVO
    }

    /**
     * 获取文件后缀名
     */
    fun getFileExtension(filePath: String): String {
        val lastDotIndex = filePath.lastIndexOf('.')

        if (lastDotIndex == -1 || lastDotIndex == filePath.length - 1) {
            return ""
        }
        return filePath.substring(lastDotIndex)
    }

    /**
     * 根据后缀名获取对应的语言信息
     */
    fun getLanguageByExtension(extension: String): String {
        for ((language, suffixes) in LLMConstants.LANG_SUFFIX_MAP) {
            if (suffixes.contains(extension)) {
                return language
            }
        }
        return "未知"
    }

    /**
     * 获取大模型生成建议，采用OpenAI客户端格式
     */
    fun getDefectSuggestionContent(bkTicket: String?, apiChatVO: ApiChatVO, defectId: String): String {
        logger.info("use openAI client to get defect suggestion: $defectId")
        var content = ""
        // 生成调用LLM接口鉴权对象
        val apiAuthVO = ApiAuthVO(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            host = bkaidevHost,
            bkTicket = bkTicket,
            apiKey = LLMConstants.LLM_API_KEY
        )

        return if (apiChatVO.stream == true) {
            runBlocking {
                LLMOpenAIApi.getChatCompletions(apiAuthVO, apiChatVO, model).collect { result ->
                    content += result
                    // 实时推送部分结果
                    simpMessagingTemplate.convertAndSend(
                        "${LLMConstants.WEBSOCKET_TOPIC_DEFECT_SUGGESTION}/$defectId",
                        result
                    )
                }
                // 返回完整内容
                content
            }
        } else {
            // 非流式调用
            val result = LLMOpenAIApi.getChatCompletion(apiAuthVO, apiChatVO, model)
            // 推送完整结果
            simpMessagingTemplate.convertAndSend(
                "${LLMConstants.WEBSOCKET_TOPIC_DEFECT_SUGGESTION}/$defectId",
                result
            )
            result
        }
    }

    /**
     * 获取大模型生成建议，该接口已过时，新接口采用OpenAI客户端格式进行请求
     */
    fun defectSuggestionContent(bkTicket: String?, apiChatVO: ApiChatVO, defectId: String): String {
        logger.info("start to get defect suggestion: $defectId")
        var content = ""
        // 生成调用LLM接口鉴权对象
        val apiAuthVO = ApiAuthVO(
            bkAppCode = appCode,
            bkAppSecret = appSecret,
            host = bkaidevHost,
            bkTicket = bkTicket,
            apiKey = LLMConstants.LLM_API_KEY
        )
        // 选择大模型接口及结果处理对象
        val (llmApi: LLMApi, llmResultHandle: LLMResultHandle) = when (model) {
            LLMConstants.LLMType.HUNYUAN.llmName.lowercase() -> Pair(HunYuanApi, HunYuanResultHandle)
            LLMConstants.LLMType.CHATGPT.llmName.lowercase() -> Pair(ChatGPTApi, ChatGPTResultHandle)
            LLMConstants.LLMType.CHATGLM2.llmName.lowercase() -> Pair(ChatGLM2Api, ChatGLM2ResultHandle)
            LLMConstants.LLMType.QPILOT.llmName.lowercase() -> Pair(QpilotApi, QpilotResultHandle)
            LLMConstants.LLMType.BKAIDEV.llmName.lowercase() -> Pair(BkAIDevApi, BkAIDevResultHandle)
            else -> Pair(HunYuanApi, HunYuanResultHandle)
        }

        if (apiChatVO.stream == true) {
            runBlocking {
                llmApi.defectSuggestionApiStream(apiAuthVO, apiChatVO).collect { result ->
                    content += result
                    simpMessagingTemplate.convertAndSend(
                        "${LLMConstants.WEBSOCKET_TOPIC_DEFECT_SUGGESTION}/$defectId", result
                    )
                }
            }
        } else {
            content = llmApi.defectSuggestionApi(apiAuthVO, apiChatVO)
            simpMessagingTemplate.convertAndSend(
                "${LLMConstants.WEBSOCKET_TOPIC_DEFECT_SUGGESTION}/$defectId", content
            )
        }
        logger.info("get defect suggestion end: $defectId content length:${content.length}")
        return content
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefectSuggestionService::class.java)
    }
}
