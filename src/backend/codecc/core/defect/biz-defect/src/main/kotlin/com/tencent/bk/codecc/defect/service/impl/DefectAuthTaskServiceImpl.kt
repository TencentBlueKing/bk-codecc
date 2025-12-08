package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.devops.common.api.QueryTaskListReqVO
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.KEY_PIPELINE_ID
import com.tencent.devops.common.auth.api.pojo.external.KEY_PROJECT_ID
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.auth.api.pojo.external.TOOLS
import com.tencent.devops.common.auth.api.service.AuthTaskService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants.TIMEOUT_FIVE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import kotlin.text.isNullOrEmpty

@Component
class DefectAuthTaskServiceImpl @Autowired constructor(
    private val client: Client,
    private val redisTemplate: RedisTemplate<String, String>
) : AuthTaskService {

    /**
     * 查询任务创建来源
     */
    override fun getTaskCreateFrom(
            taskId: Long
    ): String {
        var createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
        if (createFrom.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo != null && taskInfo.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.createFrom.isNullOrEmpty()) {
                    createFrom = taskInfoEntity.createFrom
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM, createFrom)
                }
            }

        }
        return createFrom ?: ""
    }

    /**
     * 获取工具列表
     */
    override fun getTaskToolNameList(
        taskId: Long
    ): Set<String> {
        val redisKey = "$PREFIX_TASK_INFO$TOOLS:$taskId"
        // 尝试从 Redis 获取工具列表
        val tools = redisTemplate.opsForSet().members(redisKey)

        if (!tools.isNullOrEmpty()) {
            return tools
        }
        // 查询工具列表
        val res = client.get(ServiceTaskRestResource::class.java).getTaskToolNameList(taskId)
        val taskToolNameList = res?.data ?: emptyList()

        if (taskToolNameList.isNotEmpty()) {
            redisTemplate.opsForSet().add(redisKey, *taskToolNameList.toTypedArray())
            redisTemplate.expire(redisKey, TIMEOUT_FIVE, TimeUnit.MINUTES) // 设置五分钟过期
        }
        return taskToolNameList.toSet()
    }

    /**
     * 获取任务所属流水线ID
     */
    override fun getTaskPipelineId(
            taskId: Long
    ): String {
        var pipelineId = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_PIPELINE_ID)
        if (pipelineId.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo != null && taskInfo.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.pipelineId.isNullOrEmpty()) {
                    pipelineId = taskInfoEntity.pipelineId
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_PIPELINE_ID, pipelineId)
                }
            }
        }
        return pipelineId ?: ""
    }

    /**
     * 获取任务所属bg id
     */
    override fun getTaskBgId(taskId: Long): String {
        return "-1"
    }

    override fun getTaskProjectId(taskId: Long): String {
        var projectId = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_PROJECT_ID)
        if (projectId.isNullOrEmpty()) {
            val taskInfo = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
            if (taskInfo != null && taskInfo.data != null) {
                val taskInfoEntity = taskInfo.data
                if (taskInfoEntity != null && !taskInfoEntity.projectId.isNullOrEmpty()) {
                    projectId = taskInfoEntity.projectId
                    redisTemplate.opsForHash<String, String>().put(PREFIX_TASK_INFO + taskId, KEY_PROJECT_ID, projectId)
                }
            }
        }
        return projectId ?: ""
    }

    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java)
                .batchGetTaskList(request).data?.map { it.pipelineId }?.toSet()
            ?: setOf()
    }

    override fun queryPipelineListForUser(user: String, projectId: String): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        request.userId = user
        return client.get(ServiceTaskRestResource::class.java)
                .batchGetTaskList(request).data?.map { it.pipelineId }?.toSet()
            ?: setOf()
    }

    override fun queryPipelineListByProjectId(projectId: String): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java)
                .batchGetTaskList(request).data?.map { it.pipelineId }?.toSet() ?: setOf()
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        return client.get(ServiceTaskRestResource::class.java)
                .batchGetTaskList(request).data?.map { it.taskId.toString() }?.toSet()
            ?: setOf()
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        val result = mutableListOf<String>()
        val request = QueryTaskListReqVO()
        request.projectId = projectId
        client.get(ServiceTaskRestResource::class.java)
                .batchGetTaskList(request).data?.forEach { result.addAll(it.taskOwner) }
        return result
    }

    override fun queryTaskListByPipelineIds(pipelineIds: Set<String>): Set<String> {
        return client.get(ServiceTaskRestResource::class.java)
                .queryTaskListByPipelineIds(pipelineIds).data ?: setOf()
    }

    override fun queryPipelineIdsByTaskIds(taskIds: Set<Long>): Set<String> {
        return client.get(ServiceTaskRestResource::class.java).getTaskInfosByIds(taskIds.toList()).data
                ?.filter { it != null && !it.pipelineId.isNullOrEmpty() }?.map { it.pipelineId }?.toSet() ?: emptySet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefectAuthTaskServiceImpl::class.java)
    }

}
