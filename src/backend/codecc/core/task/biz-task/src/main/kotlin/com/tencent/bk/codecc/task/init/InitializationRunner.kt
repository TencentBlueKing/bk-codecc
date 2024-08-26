/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.codecc.task.init

import com.tencent.bk.codecc.task.dao.CommonDao
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.service.AdminAuthorizeService
import com.tencent.bk.codecc.task.service.code.InitResponseCode
import com.tencent.devops.common.auth.api.pojo.external.KEY_ADMIN_MEMBER
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PROJECT_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.constant.RedisKeyConstants.STANDARD_LANG
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.util.ThreadPoolUtil
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class InitializationRunner @Autowired constructor(
    private val initResponseCode: InitResponseCode,
    private val taskRepository: TaskRepository,
    private val baseDataRepository: BaseDataRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val toolMetaCacheService: ToolMetaCacheService,
    private val adminAuthorizeService: AdminAuthorizeService,
    private val commonDao: CommonDao
) : CommandLineRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(InitializationRunner::class.java)
    }

    override fun run(vararg arg: String?) {
        val currentVal = redisTemplate.opsForValue().get(RedisKeyConstants.CODECC_TASK_ID)
        if (null == currentVal || currentVal.toLong() < ComConstants.COMMON_NUM_10000L) {
            logger.info("start to initialize redis key!")
            val taskInfoEntity = taskRepository.findFirstByTaskIdExistsOrderByTaskIdDesc(true)
            if (null == taskInfoEntity) {
                redisTemplate.opsForValue()
                        .set(RedisKeyConstants.CODECC_TASK_ID, ComConstants.COMMON_NUM_10000L.toString())
            } else {
                redisTemplate.opsForValue()
                        .set(RedisKeyConstants.CODECC_TASK_ID, (taskInfoEntity.taskId + 1).toString())
            }
        }

        val jedisConnectionFactory: JedisConnectionFactory = redisTemplate.connectionFactory as JedisConnectionFactory
        logger.info(
            "start to init data with redis: {}, {}, {}",
            jedisConnectionFactory.hostName,
            jedisConnectionFactory.port,
            jedisConnectionFactory.database
        )

        // 国际化操作[ 响应码、操作记录、规则包、规则名称、报表日期、工具参数、工具描述、操作类型 ]
        globalMessage(redisTemplate)

        // 管理员列表
        adminMember(redisTemplate)

        // 所有项目的创建来源
        ThreadPoolUtil.addRunnableTask { taskCreateFrom(redisTemplate) }

        // 初始化工具缓存
        toolMetaCacheService.loadToolDetailCache()

        // 将工具顺序设置在缓存中
        setToolOrder()

        // 缓存 语言-工具 映射关系
        setLangToolMapping()

        // 初始化开源配置规则集
//        updateOpenCheckerSet()
    }

    /**
     * 国际化处理
     */
    fun globalMessage(redisTemplate: RedisTemplate<String, String>) {
        // 响应码、操作记录国际化
        val responseCodeMap = initResponseCode.getGlobalMessageMap()
        for (key in responseCodeMap.keys) {
            redisTemplate.opsForValue().set(key, responseCodeMap[key] ?: "")
        }

        // 规则包国际化
        val checkerPackageMap = initResponseCode.getCheckerPackage()
        redisTemplate.opsForHash<String, String>()
                .putAll(RedisKeyConstants.GLOBAL_CHECKER_PACKAGE_MSG, checkerPackageMap)

        // 数据报表日期国际化
        val dataReportDate = initResponseCode.getDataReportDate()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_DATA_REPORT_DATE, dataReportDate)

        // 工具描述国际化
        val toolDescription = initResponseCode.getToolDescription()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_DESCRIPTION, toolDescription)

        // 工具参数标签[ labelName ]国际化
        val labelName = initResponseCode.getToolParams()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_LABEL_NAME, labelName)
        logger.info("init global params GLOBAL_TOOL_PARAMS_LABEL_NAME: {}", labelName)

        // 工具参数提示[ tips ]国际化
        val tips = initResponseCode.getToolParamsTips()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_TIPS, tips)
        logger.info("init global params GLOBAL_TOOL_PARAMS_TIPS: {}", tips)

        // 操作类型国际化
        val operTypeMap = initResponseCode.getOperTypeMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_PREFIX_OPERATION_TYPE, operTypeMap)

        val checkDescMap = initResponseCode.getCheckerDescMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_CHECKER_DESC, checkDescMap)
    }

    /**
     * 管理员列表
     */
    private fun adminMember(redisTemplate: RedisTemplate<String, String>) {
        val baseDataEntities = baseDataRepository.findAllByParamTypeAndParamCode("ADMIN_MEMBER", "ADMIN_MEMBER")
        if (!baseDataEntities.isNullOrEmpty()) {
            val baseDataEntity = baseDataEntities[0]
            if (!baseDataEntity.paramValue.isNullOrEmpty()) {
                redisTemplate.opsForValue().set(KEY_ADMIN_MEMBER, baseDataEntity.paramValue)
            }
        }
        // 添加bg管理员清单
        /**
         * 953: CDG
         * 29294: CSIG
         * 956: IEG
         * 29292: PCG
         * 14129: WXG
         * 958: TEG
         * 78: S1
         * 2233: S2
         * 234: S3
         * 955: 其他
         */
//        val baseDataEntityList = baseDataRepository.findAllByParamType("BG_ADMIN_MEMBER")
//        if (!baseDataEntityList.isNullOrEmpty()) {
//            baseDataEntityList.forEach {
//                redisTemplate.opsForHash<String, String>()
//                    .put(RedisKeyConstants.KEY_USER_BG_ADMIN, it.paramCode, it.paramValue)
//            }
//        }
        // 新BG管理员清单
        val initializationBgAdminMember = adminAuthorizeService.initializationBgAdminMember()
        if (initializationBgAdminMember) {
            logger.info("initialization BgAdminMember success.")
        }
    }

    /**
     * 所有项目的创建来源
     */
    private fun taskCreateFrom(redisTemplate: RedisTemplate<String, String>) {
        // 判断是否需要缓存createFrom
        val newestTaskId = redisTemplate.opsForValue().get(RedisKeyConstants.CODECC_TASK_ID)
        val newestTaskCreateFrom = redisTemplate.opsForHash<String, String>()
                .get(PREFIX_TASK_INFO + (newestTaskId!!.toLong() - 1), KEY_CREATE_FROM)
        // 判断是否需要缓存bg映射
        val maxTaskInfoEntity =
            taskRepository.findFirstByCreateFromOrderByTaskIdDesc(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())
        val latestBgMapping = redisTemplate.opsForHash<String, String>()
                .get(RedisKeyConstants.KEY_TASK_BG_MAPPING, maxTaskInfoEntity.taskId.toString())
        if (StringUtils.isNotEmpty(newestTaskCreateFrom) && !latestBgMapping.isNullOrBlank()) {
            return
        }
        var pageable: Pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "task_id"))
        var pageTasks: List<TaskInfoEntity>
        do {
            val taskInfoEntityPage = taskRepository.findTasksByPage(pageable)
            if (!taskInfoEntityPage.hasContent()) {
                break
            }
            pageTasks = taskInfoEntityPage.content
            var needCache = false
            var needProjectIdCache = false
            val lastTask = pageTasks.lastOrNull()
            lastTask?.let {
                needCache =
                    redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + it.taskId, KEY_CREATE_FROM)
                        .isNullOrEmpty()
                needProjectIdCache =
                    redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + it.taskId, KEY_PROJECT_ID)
                        .isNullOrEmpty()
            }

            if (needCache || needProjectIdCache) {
                redisTemplate.execute { connection ->
                    pageTasks.forEach { task ->
                        if (needCache && !task.createFrom.isNullOrEmpty()) {
                            connection.hSet(
                                (PREFIX_TASK_INFO + task.taskId).toByteArray(),
                                KEY_CREATE_FROM.toByteArray(),
                                task.createFrom.toByteArray()
                            )
                        }
                        if (needProjectIdCache && !task.projectId.isNullOrEmpty()) {
                            connection.hSet(
                                (PREFIX_TASK_INFO + task.taskId).toByteArray(),
                                KEY_PROJECT_ID.toByteArray(),
                                task.projectId.toByteArray()
                            )
                        }
                    }
                }
            }

            redisTemplate.execute { connection ->
                pageTasks.forEach { task ->
                    connection.hSet(
                        RedisKeyConstants.KEY_TASK_BG_MAPPING.toByteArray(),
                        task.taskId.toString().toByteArray(),
                        task.bgId.toString().toByteArray()
                    )
                }
            }

            pageable = pageable.next()
        } while (taskInfoEntityPage.hasNext())
    }

    private fun setToolOrder() {
        val toolOrder = commonDao.toolOrder
        val langOrder = commonDao.langOrder

        logger.info("start to set tool order: {}", commonDao.toolOrder)
        logger.info("start to set lang order: {}", commonDao.langOrder)

        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_TOOL_ORDER, toolOrder)
        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_LANG_ORDER, langOrder)
    }

    fun setLangToolMapping() {
        val langtoolMapping = baseDataRepository.findAllByParamType(STANDARD_LANG)
        langtoolMapping.forEach {
            redisTemplate.opsForHash<String, String>().put(RedisKeyConstants.STANDARD_LANG, it.paramCode, it.paramValue)
        }
    }
}
