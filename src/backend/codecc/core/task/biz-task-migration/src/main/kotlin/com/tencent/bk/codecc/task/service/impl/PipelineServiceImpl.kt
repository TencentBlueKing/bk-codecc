/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.bk.codecc.task.service.MetaService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.utils.PipelineUtils
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.BuildEnvVO
import com.tencent.bk.codecc.task.vo.PipelineBasicInfoVO
import com.tencent.bk.codecc.task.vo.RepoInfoVO
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import feign.RequestInterceptor
import lombok.extern.slf4j.Slf4j
import net.sf.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.set

/**
 * 与蓝盾交互工具类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
@Slf4j
open class PipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val baseDataRepository: BaseDataRepository,
    private val toolMetaCacheService: ToolMetaCacheService,
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
    private val metaService: MetaService,
    private val pipelineUtils: PipelineUtils
) : PipelineService {

    @Value("\${devops.dispatch.imageName:tlinux_ci}")
    private val imageName: String = ""

    @Value("\${devops.dispatch.buildType:DOCKER}")
    private val buildType: String = ""

    @Value("\${devops.dispatch.imageVersion:3.*}")
    private val imageVersion: String = ""

    override fun updateCodeLibrary(userName: String, registerVO: BatchRegisterVO, taskEntity: TaskInfoEntity): Boolean {
        if (taskEntity.createFrom == ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() ||
            taskEntity.createFrom == ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()) {
            logger.info("no codecc task channel, do not update...")
            return false
        }

        val model = with(taskEntity) {
            getPipelineModel(userName, projectId, pipelineId, createFrom, nameEn)
        }

        val stageList = mutableListOf<Stage>()
        model.stages.forEach { stage ->
            val containerList = mutableListOf<Container>()
            stage.containers.forEach { container ->
                val elementList = mutableListOf<Element>()
                container.elements.forEach { element ->
                    // 旧插件替换成新的
                    val newElement = when {
                        pipelineUtils.isNewCodeElement(element) || pipelineUtils.isOldCodeElement(element) -> {
                            pipelineUtils.getNewCodeElement(
                                PipelineUtils.Companion.CodeElementData(
                                    scmType = registerVO.scmType,
                                    repoHashId = registerVO.repoHashId,
                                    branch= registerVO.branch,
                                    relPath = ""
                                )
                            )
                        }
                        else -> {
                            element
                        }
                    }
                    elementList.add(newElement)

                }
                container.elements = elementList

                // 改buildEnv
                val newContainer = if (container is VMBuildContainer) {
                    VMBuildContainer(
                        id = container.id,
                        name = container.name,
                        elements = elementList,
                        status = container.status,
                        startEpoch = container.startEpoch,
                        systemElapsed = container.systemElapsed,
                        elementElapsed = container.elementElapsed,
                        baseOS = container.baseOS,
                        vmNames = container.vmNames,
                        maxQueueMinutes = container.maxQueueMinutes,
                        maxRunningMinutes = container.maxRunningMinutes,
                        buildEnv = registerVO.buildEnv,
                        customBuildEnv = container.customBuildEnv,
                        thirdPartyAgentId = container.thirdPartyAgentId,
                        thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                        thirdPartyWorkspace = container.thirdPartyWorkspace,
                        dockerBuildVersion = container.dockerBuildVersion,
                        dispatchType = container.dispatchType,
                        canRetry = container.canRetry,
                        enableExternal = container.enableExternal,
                        containerId = container.containerId,
                        jobControlOption = container.jobControlOption,
                        mutexGroup = container.mutexGroup,
                        tstackAgentId = container.tstackAgentId
                    )
                } else {
                    container
                }

                containerList.add(newContainer)
            }
            val newStage = Stage(
                containers = containerList,
                id = null
            )
            stageList.add(newStage)
        }

        val newModel = with(model) {
            Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
        }

        // 更新流水线model
        with(taskEntity) {
            val channelCode = pipelineUtils.getDevopsChannelCode(createFrom, taskEntity.nameEn)
            val edit = client.getDevopsService(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, channelCode)
            if (Objects.nonNull(edit) && edit.data != true) {
                throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
            }
            logger.info("update codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")
        }

        return true
    }

    override fun installAtom(userName: String, projectIds: ArrayList<String>, atomCode: String): Boolean {
        val result = client.getDevopsService(ServiceMarketAtomResource::class.java).installAtom(
                userName,
                ChannelCode.GONGFENGSCAN,
                InstallAtomReq(projectIds, atomCode)
        )

        if (result.isNotOk() || result.data == null) {
            logger.info("install atom for open scan project fail, userName: $userName | projects: $projectIds, atomCode: $atomCode")
            return false
        }

        return result.data!!
    }

    override fun createActiveProjDevopsProject(activeProjParseModel: ActiveProjParseModel): String {
        // TODO("not implemented")
        return ""
    }


    override fun assembleCreatePipeline(registerVO: BatchRegisterVO, taskInfoEntity: TaskInfoEntity,
                                        defaultExecuteTime: String, defaultExecuteDate: List<String>,
                                        userName: String, relPath: String, pipelineAction: String): String {
        val finalBuildType = if(!taskInfoEntity.projectId.isNullOrBlank()
            && taskInfoEntity.projectId.startsWith(ComConstants.GRAY_PROJECT_PREFIX)) {
            "GONGFENGSCAN"
        } else {
            buildType
        }
        val modelParam = pipelineUtils.createPipeline(
            registerVO = registerVO,
            taskInfoEntity = taskInfoEntity,
            relPath = relPath,
            imageName = imageName,
            dispatchType = pipelineUtils.getDispatchType(finalBuildType, imageName, imageVersion)
        )
        if("UPDATE" == pipelineAction) {
            val result = client.getDevopsService(ServicePipelineResource::class.java)
                .edit(userName, taskInfoEntity.projectId, taskInfoEntity.pipelineId, modelParam, ChannelCode.CODECC_EE)
            if (result.isNotOk() || null == result.data) {
                logger.error("create pipeline fail! err msg: {}", result.message)
                throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
            }
            return taskInfoEntity.pipelineId
        } else {
            val result = client.getDevopsService(ServicePipelineResource::class.java)
                .create(userName, taskInfoEntity.projectId, modelParam, ChannelCode.CODECC_EE)
            if (result.isNotOk() || null == result.data) {
                logger.error("create pipeline fail! err msg: {}", result.message)
                throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
            }
            return result.data!!.id
        }
    }


    override fun updatePipelineTools(
        userName: String, taskId: Long, toolList: List<String>, taskInfoEntity: TaskInfoEntity?,
        updateType: ComConstants.PipelineToolUpdateType, registerVO: BatchRegisterVO?, relPath: String?
    ): Set<String> {
        return mutableSetOf()
    }


    /**
     * 启动流水线
     *
     * @param taskInfoEntity
     * @param toolName
     * @param userName
     * @return
     */
    override fun startPipeline(
        pipelineId: String,
        projectId: String,
        nameEn: String,
        createFrom: String?,
        toolName: List<String>,
        userName: String
    ): String {
        val valueMap = mapOf(
            "_CODECC_FILTER_TOOLS" to toolName.joinToString(","),
            "scheduledTriggerPipeline" to "false",
            "manualTriggerPipeline" to "true",
            "openSourceCheckerSetType" to ""
        )
        var channelCode = ChannelCode.CODECC_EE
        if (nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            channelCode = ChannelCode.CODECC
        } else if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == createFrom) {
            channelCode = ChannelCode.GONGFENGSCAN
        }

        val interceptor = SpringContextUtil.getBean(
            RequestInterceptor::class.java,
            "devopsRequestInterceptor"
        )
        logger.info("Interceptor: $interceptor")

        logger.info("manualStartup: $userName | $projectId | $pipelineId | $valueMap | $channelCode")
        val buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
            userName, projectId, pipelineId, valueMap, channelCode
        )
        if (buildIdResult.isNotOk() || null == buildIdResult.data) {
            logger.error(
                "start pipeline fail! project id: {}, pipeline id: {}, msg is: {}", projectId,
                pipelineId, buildIdResult.message
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val buildId = buildIdResult.data?.id ?: ""
        logger.info("return build id: {}", buildId)
        return buildId
    }

    @Async("asyncTaskExecutor")
    override fun updateTaskInitStep(
        isFirstTrigger: String?, taskInfoEntity: TaskInfoEntity,
        pipelineBuildId: String, toolName: String, userName: String
    ) {
        val uploadTaskLogStepVO = UploadTaskLogStepVO()
        uploadTaskLogStepVO.stepNum = 1
        uploadTaskLogStepVO.startTime = System.currentTimeMillis()
        uploadTaskLogStepVO.endTime = 0L

        uploadTaskLogStepVO.msg = if (isFirstTrigger?.toBoolean() == true) {
            "添加${toolMetaCacheService.getToolDisplayName(toolName)}后自动触发第一次分析"
        } else {
            "手动触发($userName)"
        }

        uploadTaskLogStepVO.flag = TaskConstants.TASK_FLAG_PROCESSING
        uploadTaskLogStepVO.toolName = toolName
        uploadTaskLogStepVO.streamName = taskInfoEntity.nameEn
        uploadTaskLogStepVO.pipelineBuildId = pipelineBuildId
        uploadTaskLogStepVO.triggerFrom = userName

        val uploadResult = client.get(ServiceReportTaskLogRestResource::class.java).uploadTaskLog(uploadTaskLogStepVO)
        if (uploadResult.isNotOk()) {
            logger.error(
                "upload task analysis log fail! stream name: {}, tool name: {}",
                taskInfoEntity.nameEn, toolName
            )
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        logger.info("update task log step status success! task id: ${taskInfoEntity.taskId}, tool name: $toolName")
    }

    override fun getRepositoryList(projCode: String): List<RepoInfoVO> {
        val repoResult = client.getDevopsService(ServiceRepositoryResource::class.java).list(projCode, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data!!.map { (repositoryHashId, aliasName, url, type, _, _, _, _, authType) ->
            val repoInfoVO = RepoInfoVO()
            repoInfoVO.repoHashId = repositoryHashId
            repoInfoVO.url = url
            repoInfoVO.authType = authType
            repoInfoVO.type = type.name
            repoInfoVO.aliasName = aliasName
            repoInfoVO
        }
    }

    override fun getPipelineBasicInfo(projectId: String, pipelineIds: Set<String>): List<PipelineBasicInfoVO> {
        val result = client.getDevopsService(ServicePipelineResource::class.java)
            .getPipelineByIds(projectId, pipelineIds)
        if (result.isNotOk() || null == result.data) {
            logger.error("get pipeline names fail")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val ret = mutableListOf<PipelineBasicInfoVO>()
        result.data!!.forEach {
            val item = PipelineBasicInfoVO()
            item.pipelineId = it.pipelineId
            item.pipelineName = it.pipelineName
            item.projectId = it.projectId
            ret.add(item)
        }

        return ret
    }

    /**
     * 获取代码库分支
     *
     * @param projCode
     * @param url
     * @param scmType
     * @return
     */
    override fun getRepositoryBranches(projCode: String, url: String, scmType: String): List<String>? {

        val repoResult = client.getDevopsService(ServiceScmResource::class.java)
            .listBranches(projCode, url, ScmType.valueOf(scmType), null, null, null, null, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data
    }

    /**
     * 将codecc平台的项目语言转换为蓝盾平台的codecc原子语言
     */
    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun localConvertDevopsCodeLang(langCode: Long): List<String> {
        val metadataList = metaService.queryMetadatas(ComConstants.METADATA_TYPE_LANG)[ComConstants.METADATA_TYPE_LANG]
        val languageList = metadataList?.filter { metadataVO ->
            (metadataVO.key.toLong() and langCode) != 0L
        }
            ?.map { metadataVO ->
                JSONArray.fromObject(metadataVO.aliasNames)[0].toString()
            }
        return if (languageList.isNullOrEmpty()) listOf(ComConstants.CodeLang.OTHERS.langName()) else languageList
    }

    @Throws(StreamException::class)
    override fun convertDevopsCodeLangToCodeCC(codeLang: String): Long? {
        if (codeLang.isBlank()) {
            return 0L
        }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val reqLangList = objectMapper.readValue<List<String>>(codeLang, object : TypeReference<List<String>>() {

        })
        return metaLangList.filter { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            langArray.any { reqLangList.contains(it as String) }
        }.map { metaLang ->
            metaLang.paramCode.toLong()
        }.ifEmpty { return 0L }.reduce { acc, l -> acc or l }
    }


    @Throws(StreamException::class)
    override fun convertDevopsCodeLangToCodeCCWithOthers(codeLang: String): Long? {
        if (codeLang.isBlank()) {
            return 0L
        }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val reqLangList = objectMapper.readValue<List<String>>(codeLang, object : TypeReference<List<String>>() {

        })
        var filteredReqLangList = reqLangList

        val filteredMetaLangList = metaLangList.filter { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            filteredReqLangList = reqLangList.filterNot { langArray.toList().contains(it) }
            langArray.any { reqLangList.contains(it as String) }
        }
        val otherBaseData = metaLangList.find { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            langArray.toList().contains("OTHERS")
        }
        return if(!filteredReqLangList.isNullOrEmpty() && null != otherBaseData){
            filteredMetaLangList.map { metaLang ->
                metaLang.paramCode.toLong()
            }.ifEmpty { return 0L }.reduce { acc, l -> acc or l } or otherBaseData!!.paramCode.toLong()
        } else {
            filteredMetaLangList.map { metaLang ->
                metaLang.paramCode.toLong()
            }.ifEmpty { return 0L }.reduce { acc, l -> acc or l }
        }
    }

    override fun convertCodeCCLangToString(codeLang: Long): Set<String> {
        val metadataList = metaService.queryMetadatas(ComConstants.METADATA_TYPE_LANG)[ComConstants.METADATA_TYPE_LANG]
        return metadataList?.filter { (it.key.toLong() and codeLang) != 0L }?.map { it.langFullKey }?.toSet()?: setOf()
    }

    override fun modifyCodeCCTiming(
        taskInfoEntity: TaskInfoEntity,
        executeDate: List<String>,
        executeTime: String,
        userName: String
    ) {
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
            ChannelCode.BS
        } else if (taskInfoEntity.nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            ChannelCode.CODECC
        } else {
            ChannelCode.CODECC_EE
        }
        val modelResult = client.getDevopsService(ServicePipelineResource::class.java, projectId)
            .get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data
        val newModel = with(model!!)
        {
            val stageList = kotlin.collections.mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                    if (stageIndex == 0) {
                        val containerList = kotlin.collections.mutableListOf<Container>()
                        stage.containers.forEachIndexed { containerIndex, container ->
                            val newContainer: Container = if (container is TriggerContainer &&
                                containerIndex == 0
                            ) {
                                val newElements = container.elements.toMutableList()
                                if (newElements.size > 1) {
                                    newElements.removeAt(1)
                                }
                                //定时时间或者日期为空则删除定时任务编排
                                if (!executeTime.isBlank() && !executeDate.isNullOrEmpty()) {
                                    val cronTabStr = pipelineUtils.getCrontabTimingStr(executeTime, executeDate)
                                    //无论原来是否有定时任务原子，都新建更新
                                    val timerTriggerElement =
                                        TimerTriggerElement("定时触发", null, null,
                                            cronTabStr,
                                            null, listOf(cronTabStr), null
                                        )
                                    newElements.add(timerTriggerElement)
                                }
                                TriggerContainer(
                                    id = container.id,
                                    name = container.name,
                                    elements = newElements,
                                    status = container.status,
                                    startEpoch = container.startEpoch,
                                    systemElapsed = container.systemElapsed,
                                    elementElapsed = container.elementElapsed,
                                    params = container.params,
                                    templateParams = container.templateParams,
                                    buildNo = container.buildNo,
                                    canRetry = container.canRetry,
                                    containerId = container.containerId,
                                    jobId = null
                                )
                            } else {
                                container
                            }
                            containerList.add(newContainer)
                        }
                        Stage(containerList, stage.id)
                    } else {
                        stage
                    }
                stageList.add(newStage)
            }
            Model(
                name,
                desc,
                stageList,
                labels,
                instanceFromTemplate,
                pipelineCreator
            )
        }

        val modifyResult = client.getDevopsService(ServicePipelineResource::class.java)
            .edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk() || modifyResult.data != true) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        logger.info("modify codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")

        taskInfoEntity.executeDate = executeDate
        taskInfoEntity.executeTime = executeTime
    }

    override fun deleteCodeCCTiming(userName: String, taskEntity: TaskInfoEntity) {
        var createFrom = taskEntity.getCreateFrom()
        var pipelineId = taskEntity.getPipelineId()
        var projectId = taskEntity.getProjectId()
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == createFrom)
            ChannelCode.BS
        else if (taskEntity.nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            ChannelCode.CODECC
        } else
            ChannelCode.CODECC_EE

        val modelResult = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data

        val newModel = with(model!!)
        {
            val stageList = kotlin.collections.mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                    if (stageIndex == 0) {
                        val containerList = kotlin.collections.mutableListOf<Container>()
                        stage.containers.forEachIndexed { containerIndex, container ->
                            val newContainer: Container = if (container is TriggerContainer &&
                                containerIndex == 0
                            ) {
                                val newElements = container.elements.toMutableList()
                                //todo 要把定时trigger去掉
                                if (newElements.size >= 2 && newElements[1] is TimerTriggerElement) {
                                    newElements.removeAt(1)
                                }
                                com.tencent.devops.common.pipeline.container.TriggerContainer(
                                    id = container.id,
                                    name = container.name,
                                    elements = newElements,
                                    status = container.status,
                                    startEpoch = container.startEpoch,
                                    systemElapsed = container.systemElapsed,
                                    elementElapsed = container.elementElapsed,
                                    params = container.params,
                                    templateParams = container.templateParams,
                                    buildNo = container.buildNo,
                                    canRetry = container.canRetry,
                                    containerId = container.containerId,
                                    jobId = null
                                )
                            } else {
                                container
                            }
                            containerList.add(newContainer)
                        }
                        com.tencent.devops.common.pipeline.container.Stage(containerList, stage.id)
                    } else {
                        stage
                    }
                stageList.add(newStage)
            }
            com.tencent.devops.common.pipeline.Model(
                name,
                desc,
                stageList,
                labels,
                instanceFromTemplate,
                pipelineCreator
            )
        }
        val modifyResult = client.getDevopsService(ServicePipelineResource::class.java)
            .edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk()) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        logger.info("delete codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")
    }

    override fun getRepoUrl(projectId: String?, repoHashId: String?): String {
        if (projectId.isNullOrBlank() || repoHashId.isNullOrBlank()) {
            return ""
        }

        val repoResult = try {
            client.getDevopsService(ServiceRepositoryResource::class.java).get(projectId, repoHashId, RepositoryType.ID)
        } catch (illegalArgument: IllegalArgumentException) {
            logger.warn("illegal argument: ${illegalArgument.message}")
            null
        } catch (notFound: NotFoundException) {
            logger.warn("repo not found: ${notFound.message}")
            null
        }

        if (repoResult == null || repoResult.isNotOk() || repoResult.data == null || repoResult.data!!.url.isBlank()) {
            return ""
        }

        return repoResult.data!!.url
    }

    override fun getRepoDetail(
        taskInfoEntity: TaskInfoEntity,
        analyzeConfigInfoVO: AnalyzeConfigInfoVO
    ): AnalyzeConfigInfoVO {
        val projectId = taskInfoEntity.projectId
        val repoHashId = taskInfoEntity.repoHashId
        val repoResult =
            client.getDevopsService(ServiceRepositoryResource::class.java).get(projectId, repoHashId, RepositoryType.ID)
        if (repoResult.isNotOk()) {
            logger.error("get repo detail fail! project id: $projectId, repo hash id: $repoHashId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val repository = repoResult.data
        //设置代码库信息
        if (repository is CodeSvnRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.SVN.name
        } else if (repository is CodeGitRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GIT.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else if (repository is CodeGitlabRepository) {
            //确认gitlab也是git
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GIT.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else if (repository is GithubRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GITHUB.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else {
            if (null != repository) {
                analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GITHUB.name
                analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
            }
        }
        analyzeConfigInfoVO.url = repository!!.url
        return analyzeConfigInfoVO
    }

    override fun getBuildEnv(
        os: String
    ): List<BuildEnvVO> {
        val repoResult = client.getDevopsService(ServiceContainerAppResource::class.java).listApp(os)
        if (repoResult.isNotOk()) {
            logger.error("get build env list fail! os: $os")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val buildEnvVOList = ArrayList<BuildEnvVO>()
        repoResult.data?.forEach {
            val buildEnvVO = BuildEnvVO()
            buildEnvVO.name = it.name
            buildEnvVO.versions = it.versions
            buildEnvVOList.add(buildEnvVO)
        }
        return buildEnvVOList
    }

    /**
     * 根据蓝盾项目ID列表获取代码库信息Map url，vo
     */
    override fun getRepoUrlByBkProjects(projectIds: Set<String>): Map<String, RepoInfoVO> {
        logger.info("query bk project id count: {}", projectIds.size)
        val result = client.getDevopsService(ServiceRepositoryResource::class.java).listByProjects(
            projectIds, 1,
            20000
        )
        if (result.isNotOk() || result.data == null) {
            logger.error("get repo url info fail! bs project ids: {}", projectIds)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val repoUrlMap = HashMap<String, RepoInfoVO>()
        val resultData = result.data!!
        logger.info("fetch repo url count: {}", resultData.count)

        resultData.records.forEach {
            val repo = RepoInfoVO()
            BeanUtils.copyProperties(it, repo)
            repoUrlMap[it.url] = repo
        }

        return repoUrlMap
    }

    override fun updateCheckerSets(
        userName: String,
        projectId: String,
        pipelineId: String,
        taskId: Long,
        checkerSets: List<ToolCheckerSetVO>
    ): Boolean {
       return true
    }

    override fun searchLostRelationPipelineIds(
        pipelineIdSet: Set<String>,
        projectId: String,
        taskId: Long,
        userName: String
    ): Set<String> {
        val result = mutableSetOf<String>()
        pipelineIdSet.forEach p@{ pipelineId ->
            val model = getPipelineModel(userName, projectId, pipelineId, ChannelCode.BS)
            model.stages.forEach s@{ stage ->
                stage.containers.forEach c@{ container ->
                    container.elements.forEach e@{ element ->
                        if (element.getAtomCode() != ComConstants.AtomCode.CODECC_V2.code() &&
                            element.getAtomCode() != ComConstants.AtomCode.CODECC_V3.code()
                        ) {
                            return@e
                        }

                        val asyncTaskId = pipelineUtils.getAsyncTaskIdFromElement(element)
                        if (asyncTaskId != null && asyncTaskId == taskId) {
                            return@p
                        }
                    }
                }
            }

            result.add(pipelineId)
        }

        return result
    }

    private fun getPipelineModel(
        userName: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Model {
        val result = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (result.isNotOk() || null == result.data) {
            logger.error("get incremental info fail! bs project id: {}, bs incremental id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data as Model
    }

    private fun getPipelineModel(
        userName: String,
        projectId: String,
        pipelineId: String,
        createFrom: String,
        nameEn: String
    ): Model {
        val channelCode = pipelineUtils.getDevopsChannelCode(createFrom, nameEn)

        val result = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (result.isNotOk() || null == result.data) {
            logger.error("get incremental info fail! bs project id: {}, bs incremental id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data as Model
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
