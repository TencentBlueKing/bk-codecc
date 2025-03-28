package com.tencent.bk.codecc.defect.component

import com.alibaba.fastjson.JSONObject
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.cluster.ClusterLintCompareProcess
import com.tencent.bk.codecc.defect.component.abstract.AbstractDefectCommitComponent
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectNewInputModel
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.bk.codecc.defect.pojo.DefectClusterDTO
import com.tencent.bk.codecc.defect.service.DefectFilePathClusterService
import com.tencent.bk.codecc.defect.vo.CommitDefectVO
import com.tencent.devops.common.api.codecc.util.JsonUtil
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.ComConstants.DefectStatus
import com.tencent.devops.common.constant.ComConstants.Tool
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.constant.RedisKeyConstants
import org.apache.commons.lang.BooleanUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.util.Pair
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.io.File

@Component("LINTDefectCommitComponent")
class LintDefectCommitComponent @Autowired constructor(
    protected val lintDefectV2Dao: LintDefectV2Dao,
    private val clusterLintCompareProcess: ClusterLintCompareProcess,
    protected val newLintDefectTracingComponent: NewLintDefectTracingComponent,
    private val redisTemplate: RedisTemplate<String, String>,
    private val defectFilePathClusterService: DefectFilePathClusterService,
    private val scmJsonComponent: ScmJsonComponent
) : AbstractDefectCommitComponent<LintDefectV2Entity>(scmJsonComponent) {
    companion object {
        private val logger = LoggerFactory.getLogger(LintDefectCommitComponent::class.java)
    }

    @ExperimentalUnsignedTypes
    override fun processCluster(defectClusterDTO: DefectClusterDTO) {
        with(defectClusterDTO) {
            val taskId = commitDefectVO.taskId
            val startTime = System.currentTimeMillis()
            var queryListCost = 0L
            var saveListCost = 0L
            val toolName = commitDefectVO.toolName
            val buildId = commitDefectVO.buildId
            val commonlog = "task id: $taskId, tool name: $toolName, build id: $buildId"
            logger.info(
                "[lint cluster process] cluster process begin! $commonlog, defect cluster info: $defectClusterDTO"
            )

            // 1. 从nfs中读取本次上报告警清单
            val aggregateDefectNewInputModel = getCurrentDefectList(inputFilePath)
            val inputDefects = aggregateDefectNewInputModel.defectList
            val relPathSet = aggregateDefectNewInputModel.relPathSet
            val filePathSet = aggregateDefectNewInputModel.filePathSet
            logger.info(
                "[lint cluster process] current json file, $commonlog, current defect size: ${inputDefects.size}, " +
                        "rel path set size: ${relPathSet?.size}, file path set size: ${filePathSet?.size}"
            )

            // 2. 获取原有告警清单
            var queryOrSaveBeginTime = System.currentTimeMillis()
            val preDefectList = getPreDefectList(this, relPathSet, filePathSet)
            queryListCost = System.currentTimeMillis() - queryOrSaveBeginTime
            logger.info(
                "[lint cluster process] get pre defect list, $commonlog, size: ${preDefectList.size}"
            )

            // 3.声明判断md5是否一致的映射，用于比较时简化流程
            val md5SameMap = mutableMapOf<String, Boolean>()
            // 4.组装聚类入参
            val inputDefectList = preHandleDefectList(
                streamName = commitDefectVO.streamName,
                toolName = commitDefectVO.toolName,
                buildId = commitDefectVO.buildId,
                currentDefectList = inputDefects,
                preDefectList = preDefectList,
                md5SameMap = md5SameMap
            )
            logger.info(
                "[lint cluster process] pre handle result, $commonlog, input defect size: ${inputDefectList.size}"
            )

            // 5. 聚类得到结果
            val outputDefectList = clusterMethod(inputDefectList, md5SameMap)
            logger.info(
                "[lint cluster process] cluster result, $commonlog, output group size: ${outputDefectList.size}"
            )

            // 6. 对于聚类结果进行后处理
            val upsertDefectList = postHandleDefectList(
                outputDefectList, buildEntity, transferAuthorList, commitDefectVO.isReallocate
            )
            logger.info(
                "[lint cluster process] post handle result, $commonlog, result defect size: ${upsertDefectList.size}"
            )

            // 7. 保存文件路径聚合 需要开启缓存
            saveDefectPathCluster(taskId, toolName, buildEntity, upsertDefectList)

            queryOrSaveBeginTime = System.currentTimeMillis()
            // 8. 分批保存告警
            saveDefectFile(
                commitDefectVO,
                aggregateDefectNewInputModel,
                buildEntity,
                upsertDefectList
            )
            saveListCost = System.currentTimeMillis() - queryOrSaveBeginTime

            logger.info(
                "[lint cluster process] save defect file success! $commonlog, " +
                        "time cost total: ${System.currentTimeMillis() - startTime}, " +
                        "query list cost: $queryListCost, save list cost: $saveListCost"
            )
        }
    }

    override fun preHandleDefectList(
        streamName: String,
        toolName: String,
        buildId: String,
        currentDefectList: List<LintDefectV2Entity>,
        preDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<LintDefectV2Entity> {
        val md5Map = getMd5Map(streamName, toolName, buildId)
        val relPathMap = mutableMapOf<String, String>()
        val finalDefectList = mutableListOf<LintDefectV2Entity>()
        //1.先处理本次上报的告警
        currentDefectList.forEach {
            if (!it.relPath.isNullOrBlank()) {
                relPathMap[it.relPath] = it.filePath
            }
            it.fileMd5 = md5Map[it.filePath]
            // ObjectId用作判断update还是insert逻辑，不再预分配
            // it.entityId = ObjectId.get().toString()
            it.newDefect = true
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
        }
        finalDefectList.addAll(currentDefectList)
        // 2.再处理原有的告警
        val processedPreDefectList = preDefectList.filter { !it.pinpointHash.isNullOrBlank() ||
                toolName in Tool.SCAN_COMMIT_TOOLS }
        processedPreDefectList.forEach {
            it.newDefect = false
            if (it.status == 0) {
                it.status = ComConstants.DefectStatus.NEW.value()
            }
            val filePath = relPathMap[it.relPath]
            if (!filePath.isNullOrBlank()) {
                it.filePath = filePath
            }
            val fileMd5 = md5Map[it.filePath]
            //判断文件的md5是否相同的映射，如果相同的话则不需要比较pinpoint哈希值了（默认为false）
            md5SameMap.compute(it.filePath) { _, u ->
                //只有当没有值或者值为真的情况下才会更新，因为默认值是false
                if (null == u || u) {
                    !fileMd5.isNullOrBlank() && fileMd5 == it.fileMd5
                } else {
                    u
                }
            }
        }
        finalDefectList.addAll(processedPreDefectList)
        logger.info("pre defect size: ${preDefectList.size}, current defect size: ${currentDefectList.size}")
        return finalDefectList
    }

    override fun getCurrentDefectList(
        inputFilePath: String
    ): AggregateDefectNewInputModel<LintDefectV2Entity> {
        val inputFileName = inputFilePath.substring(inputFilePath.lastIndexOf("/") + 1)
        val inputFile = File(inputFilePath)
        var i = 0
        while (!inputFile.exists() && i < 3) {
            scmJsonComponent.indexNoThrow(inputFileName, ScmJsonComponent.AGGREGATE)
            Thread.sleep(10000L)
            i++
        }
        if (!inputFile.exists()) {
            logger.info("input file not exists! will return null")
            throw CodeCCException(CommonMessageCode.SYSTEM_ERROR)
        }
        return JsonUtil.to(
            inputFile.readText(),
            object : TypeReference<AggregateDefectNewInputModel<LintDefectV2Entity>>() {})
    }

    override fun getPreDefectList(
        defectClusterDTO: DefectClusterDTO,
        relPathSet: Set<String>?,
        filePathSet: Set<String>?
    ): List<LintDefectV2Entity> {
        val taskId = defectClusterDTO.commitDefectVO.taskId
        val toolName = defectClusterDTO.commitDefectVO.toolName
        val finalDefects = mutableListOf<LintDefectV2Entity>()

        finalDefects.addAll(lintDefectV2Dao.findByTaskIdAndToolNameAndPath(taskId, toolName, relPathSet, filePathSet))

        return finalDefects
    }

    private fun postHandleDefectListDebugEnabled(buildEntity: BuildEntity?): Boolean {
        if (buildEntity == null || buildEntity.buildId.isNullOrEmpty()) {
            return false
        }
        val check: Boolean = try {
            redisTemplate.opsForSet()
                    .isMember(RedisKeyConstants.DEBUG_BUILD_ID_SET_KEY, buildEntity.buildId) ?: false
        } catch (e: Exception) {
            logger.error("postHandleDefectListDebugEnabled cause error. ${buildEntity.buildId}", e)
            false
        }
        return check
    }

    override fun postHandleDefectList(
        outputDefectList: List<AggregateDefectOutputModelV2<LintDefectV2Entity>>,
        buildEntity: BuildEntity?,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?,
        isReallocate: Boolean?
    ): List<LintDefectV2Entity> {
        if (outputDefectList.isNullOrEmpty()) {
            logger.info("output defect list is empty! build id: ${buildEntity?.buildId}")
            return emptyList()
        }

        // 是否开启日志调试
        if (postHandleDefectListDebugEnabled(buildEntity)) {
            logger.info("postHandleDefectList outputDefectList:{}", JSONObject.toJSONString(outputDefectList))
        }


        val upsertDefectList = mutableListOf<LintDefectV2Entity>()
        outputDefectList.forEach {
            //将每一组的告警分为新告警和老告警
            val partitionedDefects = it.defects.partition { defect -> defect.newDefect }
            val newDefects = partitionedDefects.first

            // 判断是否走优化逻辑
            val deduplicate = canDeduplicate(partitionedDefects.first)
            val oldDefects = if (deduplicate) {
                // 折叠相同hash与行号的告警，去重
                partitionedDefects.second.groupBy { it.lineNum to it.pinpointHash }
                        .mapValues { (_, group) ->
                            // 优先取已忽略的告警
                            group.sortedByDescending {
                                it.status and ComConstants.DefectStatus.IGNORE.value() > 0
                            }.first()
                        }.values.toList()
            } else {
                partitionedDefects.second
            }

            val sameOldDefectMap = if (deduplicate) {
                // 将被去重的告警取出，用于后续统一处理
                oldDefects.associateBy(keySelector = { oldDefect -> oldDefect.entityId },
                    valueTransform = { oldDefect ->
                        partitionedDefects.second.filter { originalOldDefect ->
                            oldDefect.pinpointHash == originalOldDefect.pinpointHash
                                    && oldDefect.lineNum == originalOldDefect.lineNum
                                    && oldDefect != originalOldDefect
                        }
                    }).filterValues { sameDefects -> sameDefects.isNotEmpty() }
            } else {
                emptyMap()
            }

            val groupId = UUIDUtil.generate()
            /**
             * 如果聚类分组中只有老告警
             * 1. 将状态是NEW的老告警变成已修复
             * 2. 老告警是其他状态，则不变更直接上报
             */
            if (newDefects.isNullOrEmpty()) {
                //logger.info("newDefects: ${newDefects.size}˚")
                oldDefects.forEach { oldDefect ->
                    if (oldDefect.status == ComConstants.DefectStatus.NEW.value()) {
                        fixDefect(oldDefect, buildEntity)
                        oldDefect.pinpointHashGroup = groupId
                        upsertDefectList.add(oldDefect)
                    }
                    processSameOldDefect(oldDefect, sameOldDefectMap, upsertDefectList, buildEntity)
                    // 判断是否配置了处理人重新分配
                    if (true == isReallocate) {
                        transferAuthor(oldDefect, transferAuthorList)
                    }
                }
            } else {

                val priorityIgnoreDefectMap = if (deduplicate) {
                    /**
                     * 已忽略告警需要优先匹配，如果行号与Hash相同，应该直接匹配上, 优先处理
                     */
                    newDefects.filter { newDefect ->
                        oldDefects.any { oldDefect ->
                            oldDefect.pinpointHash == newDefect.pinpointHash &&
                                    oldDefect.lineNum == newDefect.lineNum &&
                                    oldDefect.status and ComConstants.DefectStatus.IGNORE.value() > 0 &&
                                    (oldDefect.ignoreCommentDefect == null || !oldDefect.ignoreCommentDefect)
                        }
                    }.associateWith { newDefect ->
                        oldDefects.first { oldDefect ->
                            oldDefect.pinpointHash == newDefect.pinpointHash &&
                                    oldDefect.lineNum == newDefect.lineNum
                        }
                    }
                } else {
                    emptyMap()
                }

                if (priorityIgnoreDefectMap.isNotEmpty()) {
                    priorityIgnoreDefectMap.forEach { entry ->
                        updateOldDefectInfo(entry.value, entry.key, transferAuthorList, buildEntity, isReallocate)
                        entry.value.pinpointHashGroup = groupId
                        upsertDefectList.add(entry.value)
                        // 查看是否有需要处理的相同旧告警
                        processSameOldDefect(entry.value, sameOldDefectMap, upsertDefectList, buildEntity)
                    }
                }

                /**
                 * 如果聚类分组中新老告警都有
                 * 1.有对应老告警：
                 *   1.1 如果对应的新告警有忽略flag，且老告警为待修复状态，则状态转为忽略，更新忽略原因
                 *   1.2 老告警是已修复，则变为重新打开
                 *   1.3 老告警是忽略状态，且注释忽略为flag为true，且新告警注释忽略flag为false，则变为重新打开，老告警注释忽略flag配置为false
                 *   1.3 老告警是其他状态，则不变更状态直接上报
                 * 2.无对应老告警
                 *   2.1 如果对应的新告警有忽略flag，则状态转为忽略，更新忽略原因
                 *   2.2 告警是首次创建的告警
                 * 3.老告警列表比新告警多，部分老告警没有对应的新告警
                 *   3.1 将状态是NEW的老告警变成已修复
                 *   3.2 老告警是其他状态，则不变更直接上报
                 */
                val sortedNewDefects = newDefects.filter { newDefect ->
                    priorityIgnoreDefectMap.keys.none { ignoreNewDefect ->
                        newDefect.lineNum == ignoreNewDefect.lineNum
                                && newDefect.pinpointHash == ignoreNewDefect.pinpointHash
                    }
                }.sortedBy { newDefect -> newDefect.lineNum }
                val sortedOldDefects = oldDefects.filter { oldDefect ->
                    priorityIgnoreDefectMap.values.none { ignoreOldDefect ->
                        oldDefect.entityId == ignoreOldDefect.entityId
                    }
                }.sortedBy { oldDefect -> oldDefect.lineNum }
                sortedNewDefects.forEachIndexed { index, newDefect ->
                    var selectedOldDefect = if (!sortedOldDefects.isNullOrEmpty() && sortedOldDefects.size > index) {
                        sortedOldDefects[index]
                    } else {
                        null
                    }
                    if (null != selectedOldDefect) {
                        //用新告警信息更新老告警信息
                        updateOldDefectInfo(selectedOldDefect, newDefect, transferAuthorList, buildEntity, isReallocate)
                        // 设置是否处理的状态
                        if (null != selectedOldDefect.mark
                                && selectedOldDefect.mark.equals(ComConstants.MarkStatus.MARKED.value())
                                && selectedOldDefect.status.equals(ComConstants.DefectStatus.NEW.value())
                        ) {
                            selectedOldDefect.markButNoFixed = true
                        }
                        val ignoreFlag = null != newDefect.ignoreCommentDefect && newDefect.ignoreCommentDefect
                        if (ignoreFlag) {
                            if (selectedOldDefect.status == ComConstants.DefectStatus.NEW.value()) {
                                ignoreDefect(
                                    selectedOldDefect, newDefect.author.first() ?: "", newDefect.ignoreCommentReason,
                                    buildEntity?.buildId
                                )
                            }
                        } else {
                            if (selectedOldDefect.status and ComConstants.DefectStatus.FIXED.value() > 0) {
                                reopenDefect(selectedOldDefect)
                            }
                            if ((selectedOldDefect.status and ComConstants.DefectStatus.IGNORE.value() > 0) &&
                                    null != selectedOldDefect.ignoreCommentDefect && selectedOldDefect.ignoreCommentDefect &&
                                    (null == newDefect.ignoreCommentDefect || !newDefect.ignoreCommentDefect)
                            ) {
                                selectedOldDefect.ignoreCommentDefect = false
                                reopenDefect(selectedOldDefect)
                            }
                            // 重新上报，打开规则屏蔽的告警
                            if (selectedOldDefect.status and DefectStatus.CHECKER_MASK.value() > 0) {
                                reopenDefect(selectedOldDefect)
                            }
                        }
                        if (selectedOldDefect.author.isNullOrEmpty()) {
                            selectedOldDefect.author = newDefect.author
                        }
                    } else {
                        selectedOldDefect = newDefect
                        selectedOldDefect.createTime = System.currentTimeMillis()
                        selectedOldDefect.status = ComConstants.DefectStatus.NEW.value()
                        if (null != buildEntity) {
                            selectedOldDefect.createBuildNumber = buildEntity.buildNo
                        }
                        if (null != selectedOldDefect.ignoreCommentDefect && selectedOldDefect.ignoreCommentDefect) {
                            ignoreDefect(
                                selectedOldDefect,
                                selectedOldDefect.author?.firstOrNull() ?: "",
                                selectedOldDefect.ignoreCommentReason,
                                buildEntity?.buildId
                            )
                        }
                        //作者转换
                        transferAuthor(selectedOldDefect, transferAuthorList)
                    }
                    selectedOldDefect.pinpointHashGroup = groupId
                    upsertDefectList.add(selectedOldDefect)
                    // 查看是否有需要处理的相同旧告警
                    processSameOldDefect(selectedOldDefect, sameOldDefectMap, upsertDefectList, buildEntity)
                }
                //老告警比新告警多出来的那部分告警变成已修复
                if (!sortedOldDefects.isNullOrEmpty() && sortedOldDefects.size > sortedNewDefects.size) {
                    logger.info("old more than new: ${sortedOldDefects.size} ${sortedNewDefects.size}")
                    val needToCloseDefects = sortedOldDefects.subList(sortedNewDefects.size, sortedOldDefects.size)
                    needToCloseDefects.forEach { closeDefect ->
                        if (closeDefect.status == ComConstants.DefectStatus.NEW.value()) {
                            fixDefect(closeDefect, buildEntity)
                            closeDefect.pinpointHashGroup = groupId
                            upsertDefectList.add(closeDefect)
                        }
                        // 查看是否有需要处理的相同旧告警
                        processSameOldDefect(closeDefect, sameOldDefectMap, upsertDefectList, buildEntity)
                    }
                }
            }
        }
        logger.info("upsert defect: ${upsertDefectList.size}")
        return upsertDefectList
    }

    /**
     * 是否可以去重处理
     */
    private fun canDeduplicate(newDefects: List<LintDefectV2Entity>): Boolean {
        if (newDefects.isEmpty()) {
            return true
        }
        // 判断是否有重复的项
        val duplicateKeys = newDefects.map { it.lineNum.toString() + ":" + it.pinpointHash }.toSet()
        return duplicateKeys.size == newDefects.size
    }

    /**
     * 处理与oldDefect相同的告警（pinpoint hash 与 行号都相同）
     */
    private fun processSameOldDefect(
        oldDefect: LintDefectV2Entity,
        sameOldDefectMap: Map<String, List<LintDefectV2Entity>>,
        upsertDefectList: MutableList<LintDefectV2Entity>,
        buildEntity: BuildEntity?
    ) {
        if (!sameOldDefectMap.containsKey(oldDefect.entityId)
                || sameOldDefectMap[oldDefect.entityId] == null
                || sameOldDefectMap[oldDefect.entityId]!!.isEmpty()
        ) {
            return
        }
        // 直接置为修复, 同步pinpoint hash 与 line num
        val sameOldDefects = sameOldDefectMap[oldDefect.entityId]
        sameOldDefects!!.forEach {
            it.pinpointHash = oldDefect.pinpointHash
            it.pinpointHashGroup = oldDefect.pinpointHashGroup
            it.lineNum = oldDefect.lineNum
            if (it.status == ComConstants.DefectStatus.NEW.value()) {
                it.status = it.status or ComConstants.DefectStatus.FIXED.value()
                it.fixedTime = System.currentTimeMillis()
                if (null != buildEntity) {
                    it.fixedBuildNumber = buildEntity.buildNo
                }
            }
            upsertDefectList.add(it)
        }
    }

    @ExperimentalUnsignedTypes
    fun clusterMethod(
        inputDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        return clusterLintCompareProcess.clusterMethod(inputDefectList, md5SameMap)
    }

    /**
     * 保存告警待修复与已修复的文件路径列表
     */
    fun saveDefectPathCluster(
        taskId: Long, toolName: String, buildEntity: BuildEntity?, upsertDefectList: List<LintDefectV2Entity>
    ) {
        if (!StringUtils.hasLength(toolName) || buildEntity == null || !StringUtils.hasLength(buildEntity.buildId) ||
                CollectionUtils.isEmpty(upsertDefectList)
        ) {
            return
        }
        try {
            if (!isFileCacheEnable(taskId)) {
                return
            }
            val startTime = System.currentTimeMillis()
            logger.info("saveDefectPathCluster start. $taskId, $toolName, ${buildEntity.buildId}")
            // 取出待修复的项
            val newDefectPaths = upsertDefectList.filter { it.status == DefectStatus.NEW.value() }
                    .distinctBy { it.filePath }.map { Pair.of(it.filePath, it.relPath ?: "") }.toList()
            if (!CollectionUtils.isEmpty(newDefectPaths)) {
                defectFilePathClusterService.saveBuildDefectFilePath(
                    taskId,
                    toolName,
                    buildEntity.buildId,
                    DefectStatus.NEW,
                    newDefectPaths
                )
            }

            // 取出已修复的项
            val fixedDefectPaths = upsertDefectList.filter { it.status and DefectStatus.FIXED.value() > 0 }
                    .distinctBy { it.filePath }.map { Pair.of(it.filePath, it.relPath ?: "") }.toList()
            if (!CollectionUtils.isEmpty(fixedDefectPaths)) {
                defectFilePathClusterService.saveBuildDefectFilePath(
                    taskId,
                    toolName,
                    buildEntity.buildId,
                    DefectStatus.FIXED,
                    fixedDefectPaths
                )
            }
            logger.info(
                "saveDefectPathCluster end. $taskId, $toolName, ${buildEntity.buildId} cost: " +
                        "${System.currentTimeMillis() - startTime} ms"
            )
        } catch (e: Exception) {
            logger.error("saveDefectPathCluster cause error. $taskId, $toolName, ${buildEntity.buildId}", e)
        }
    }

    fun saveDefectFile(
        commitDefectVO: CommitDefectVO,
        aggregateDefectNewInputModel: AggregateDefectNewInputModel<LintDefectV2Entity>,
        buildEntity: BuildEntity?,
        upsertDefectList: List<LintDefectV2Entity>
    ) {
        newLintDefectTracingComponent.saveDefectFile(
            commitDefectVO.taskId,
            commitDefectVO.toolName,
            aggregateDefectNewInputModel.filterPaths,
            aggregateDefectNewInputModel.whitePaths,
            buildEntity,
            upsertDefectList
        )
    }

    /**
     * 将告警设置为已修复
     *
     * @param defect
     * @param buildEntity
     */
    fun fixDefect(
        defect: LintDefectV2Entity,
        buildEntity: BuildEntity?
    ) {
        defect.status = defect.status or ComConstants.DefectStatus.FIXED.value()
        defect.fixedTime = System.currentTimeMillis()
        if (null != buildEntity) {
            defect.fixedBuildNumber = buildEntity.buildNo
        }
    }

    /**
     * 更新老告警信息
     */
    fun updateOldDefectInfo(
        selectOldDefect: LintDefectV2Entity, newDefect: LintDefectV2Entity,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?,
        buildEntity: BuildEntity?,
        isReallocate: Boolean?
    ) {
        with(selectOldDefect) {
            checker = newDefect.checker
            lineNum = newDefect.lineNum
            message = newDefect.message
            pinpointHash = newDefect.pinpointHash
            lineUpdateTime = newDefect.lineUpdateTime
            filePath = newDefect.filePath
            relPath = newDefect.relPath
            url = newDefect.url
            repoId = newDefect.repoId
            revision = newDefect.revision
            branch = newDefect.branch
            subModule = newDefect.subModule
            fileUpdateTime = newDefect.fileUpdateTime
            langValue = newDefect.langValue
            language = newDefect.language
            fileMd5 = newDefect.fileMd5
            if (author.isNullOrEmpty() || isReallocate == true || BooleanUtils.isTrue(buildEntity?.reallocate)) {
                author = newDefect.author
                transferAuthor(this, transferAuthorList)
            }
            defectInstances = newDefect.defectInstances
            if (status and ComConstants.DefectStatus.IGNORE.value() > 0 &&
                    status and ComConstants.DefectStatus.FIXED.value() == 0
            ) {
                ignoreBuildId = buildEntity?.buildId
            }
        }
    }

    /**
     * 作者转换
     */
    private fun transferAuthor(
        selectOldDefect: LintDefectV2Entity?,
        transferAuthorList: List<TransferAuthorEntity.TransferAuthorPair>?
    ) {
        if (selectOldDefect != null
                && !selectOldDefect.author.isNullOrEmpty()
                && !transferAuthorList.isNullOrEmpty()
        ) {
            transferAuthorList.forEach {
                if (selectOldDefect.author.contains(it.sourceAuthor)) {
                    selectOldDefect.author = listOf(it.targetAuthor)
                }
            }
        }
    }

    /**
     * 重新打开告警
     */
    protected fun reopenDefect(oldDefect: LintDefectV2Entity) {
        oldDefect.status = ComConstants.DefectStatus.NEW.value()
        oldDefect.fixedTime = null
        oldDefect.fixedBuildNumber = null
    }

    /**
     * 忽略告警
     */
    private fun ignoreDefect(oldDefect: LintDefectV2Entity, author: String, ignoreReason: String, buildId: String?) {
        oldDefect.status = oldDefect.status or ComConstants.DefectStatus.IGNORE.value()
        oldDefect.ignoreCommentDefect = true
        oldDefect.ignoreAuthor = author
        oldDefect.ignoreBuildId = buildId
        val ignoreReasonRegex = Regex("^\\s*(工具误报|其他|设计如此)\\s*([:：])")
        val groupValues = ignoreReasonRegex.find(ignoreReason)?.groupValues
        when (groupValues?.get(1)) {
            "工具误报" -> {
                oldDefect.ignoreReasonType = ComConstants.IgnoreReasonType.ERROR_DETECT.value()
                oldDefect.ignoreReason = ignoreReason.substringAfter(groupValues[2])
            }

            "其他" -> {
                oldDefect.ignoreReasonType = ComConstants.IgnoreReasonType.OTHER.value()
                oldDefect.ignoreReason = ignoreReason.substringAfter(groupValues[2])
            }

            "设计如此" -> {
                oldDefect.ignoreReasonType = ComConstants.IgnoreReasonType.SPECIAL_PURPOSE.value()
                oldDefect.ignoreReason = ignoreReason.substringAfter(groupValues[2])
            }

            else -> {
                oldDefect.ignoreReasonType = ComConstants.IgnoreReasonType.OTHER.value()
                oldDefect.ignoreReason = ignoreReason
            }
        }
        oldDefect.ignoreTime = System.currentTimeMillis()
    }
}
