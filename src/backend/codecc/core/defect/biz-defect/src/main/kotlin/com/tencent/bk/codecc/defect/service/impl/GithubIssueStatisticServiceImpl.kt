package com.tencent.bk.codecc.defect.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.defect.dao.mongotemplate.GithubStatDefectDao
import com.tencent.bk.codecc.defect.model.defect.GithubIssueDefectEntity
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.codecc.util.JsonUtil.to
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.ParseException
import java.text.SimpleDateFormat

@Service
class GithubIssueStatisticServiceImpl @Autowired constructor(
    private val githubStatDefectDao: GithubStatDefectDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubIssueStatisticServiceImpl::class.java)
    }

    fun statisticGithubIssue(taskId: Long, defects: List<Map<String, String>>): List<GithubIssueDefectEntity> {
        val filterResult = defects.filter {
            "issue" == it["msg_id"] && !it["msg_body"].isNullOrBlank()
        }.map {
            to<Map<String, Any?>>(it["msg_body"]!!)
        }

        val githubStatDefectList = to(
            JsonUtil.toJson(filterResult),
            object : TypeReference<List<GithubIssueDefectEntity>>() {})
        githubStatDefectList.forEach {
            it.taskId = taskId
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'")
            try {
                if (!it.createdAt.isNullOrBlank()) {
                    it.createdTimestamp =
                        simpleDateFormat.parse(it.createdAt).time
                }
                if (!it.closedAt.isNullOrBlank()) {
                    it.closedTimestamp =
                        simpleDateFormat.parse(it.closedAt).time
                }
                if (!it.updatedAt.isNullOrBlank()) {
                    it.updatedTimestamp =
                        simpleDateFormat.parse(it.updatedAt).time
                }
            } catch (e: ParseException) {
                logger.error("github statistic fail to parse timestamp: {}", ExceptionUtils.getStackTrace(e))
            }
        }

        githubStatDefectDao.upsertDefectList(githubStatDefectList)
        return githubStatDefectList
    }
}
