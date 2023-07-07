package com.tencent.bk.codecc.codeccjob.service.impl

import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.bk.codecc.codeccjob.api.UserTaskLogRestResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.security.AuthMethod
import com.tencent.devops.log.api.ServiceLogResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTaskLogRestResourceImpl @Autowired constructor(
        private val client: Client
) : UserTaskLogRestResource {
    @AuthMethod
    override fun getInitLogs(projectId: String, user: String, pipelineId: String, buildId: String?,
                             search: String?): Result<QueryLogs> {
        val result = client.getDevopsService(ServiceLogResource::class.java).getInitLogs(user, projectId, pipelineId,
            buildId!!, false, null, null, null, 1)
        return Result(result.status,result.message,result.data)
    }

    @AuthMethod
    override fun getAfterLogs(projectId: String?, user: String?, pipelineId: String?, buildId: String?, start: Int?,
                              search: String?): Result<QueryLogs> {
        val result = client.getDevopsService(ServiceLogResource::class.java).getAfterLogs(user!!, projectId!!,
            pipelineId!!, buildId!!, start!!.toLong(), false,null,
            null, null,  1)
        return Result(result.status,result.message,result.data)
    }

}