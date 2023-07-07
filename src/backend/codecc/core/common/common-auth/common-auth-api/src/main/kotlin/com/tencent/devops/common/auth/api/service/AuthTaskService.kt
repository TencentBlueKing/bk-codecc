package com.tencent.devops.common.auth.api.service


interface AuthTaskService {
    /**
     * 获取任务创建来源
     */
    fun getTaskCreateFrom(
        taskId: Long
    ): String


    /**
     * 获取任务所属流水线ID
     */
    fun getTaskPipelineId(
        taskId: Long
    ): String



    /**
     * 获取任务bg id
     */
    fun getTaskBgId(
        taskId: Long
    ): String

    fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String>

    fun queryPipelineListForUser(user: String, projectId: String): Set<String>

    fun queryPipelineListByProjectId(projectId: String): Set<String>

    fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String>

    fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String>

    fun queryTaskListByPipelineIds(pipelineIds: Set<String>): Set<String>

    fun queryPipelineIdsByTaskIds(taskIds: Set<Long>): Set<String>
}