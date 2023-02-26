package com.tencent.bk.codecc.quartz.mq

import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.JobInternalDto
import com.tencent.bk.codecc.quartz.pojo.OperationType
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.devops.common.web.mq.EXCHANGE_INTERNAL_JOB
import com.tencent.devops.common.web.mq.ROUTE_INTERNAL_JOB
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class JobChangeListener @Autowired constructor(
    private val customSchedulerManager: CustomSchedulerManager,
    private val jobManageService: JobManageService,
    private val rabbitTemplate: RabbitTemplate
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobChangeListener::class.java)
    }

    fun externalJobMsg(jobExternalDto: JobExternalDto) {
        try {
            //如果是第一个分片的第一个节点，则执行保存动作，并发消息
            val shardingResult = CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult()
                ?: return
            if (shardingResult.currentShard.shardNum == 1 &&
                shardingResult.currentNode.nodeNum == 1
            ) {
                var jobInstance: JobInstanceEntity? = null
                when (jobExternalDto.operType) {
                    OperationType.ADD -> {
                        jobInstance = jobManageService.saveJob(jobExternalDto)
                    }
                    OperationType.RESCHEDULE -> {
                        if (jobExternalDto.jobName.isNullOrBlank()) {
                            return
                        }

                        val jobInstanceEntity = jobManageService.findJobByName(jobExternalDto.jobName!!)
                        if (jobInstanceEntity != null) {
                            jobInstanceEntity.cronExpression = jobExternalDto.cronExpression
                            jobInstanceEntity.updatedBy = "sysadmin"
                            jobInstanceEntity.updatedDate = System.currentTimeMillis()
                            jobInstance = jobManageService.saveJob(jobInstanceEntity)
                        } else {
                            logger.info("reschedule job fail, entity is null, job name: ${jobExternalDto.jobName}")
                        }
                    }
                    OperationType.REMOVE -> {
                        jobManageService.deleteJob(jobExternalDto.jobName)
                        jobInstance = JobInstanceEntity()
                        jobInstance.jobName = jobExternalDto.jobName
                    }
                    OperationType.PARSE -> {
                        if(jobExternalDto.jobName.isNullOrBlank()){
                            return
                        }

                        val jobInstanceEntity = jobManageService.findJobByName(jobExternalDto.jobName!!)
                        if (jobInstanceEntity != null) {
                            jobInstanceEntity.updatedBy = "sysadmin"
                            jobInstanceEntity.updatedDate = System.currentTimeMillis()
                            jobInstanceEntity.status = 1
                            jobInstance = jobManageService.saveJob(jobInstanceEntity)
                        }
                    }
                    OperationType.RESUME -> {
                        if(jobExternalDto.jobName.isNullOrBlank()){
                            return
                        }

                        val jobInstanceEntity = jobManageService.findJobByName(jobExternalDto.jobName!!)
                        if (jobInstanceEntity != null) {
                            jobInstanceEntity.updatedBy = "sysadmin"
                            jobInstanceEntity.updatedDate = System.currentTimeMillis()
                            jobInstanceEntity.status = 0
                            jobInstance = jobManageService.saveJob(jobInstanceEntity)
                        }
                    }
                }

                // 由internal逻辑可知，RESUME/PARSE均需要triggerName和jobName，REMOVE只需jobName
                // RESCHEDULE额外还需要cronExpression，ADD的话jobInstance不可能null
                jobInstance = jobInstance ?: JobInstanceEntity().apply {
                    jobName = jobExternalDto.jobName
                    triggerName = jobExternalDto.jobName
                    cronExpression = jobExternalDto.cronExpression
                }

                rabbitTemplate.convertAndSend(
                    EXCHANGE_INTERNAL_JOB,
                    ROUTE_INTERNAL_JOB,
                    JobInternalDto(jobExternalDto.operType, jobInstance)
                )
            }
        } catch (e: Exception) {
            logger.error("handle external job fail!", e)
        }
    }

    fun internalJobMsg(jobInternalDto: JobInternalDto) {
        try {
            customSchedulerManager.newJobComeOrRemove(jobInternalDto.jobInstance, jobInternalDto.operType)
        } catch (e: Exception) {
            logger.error("handle internal job fail! e: ${e.message}")
        }
    }

    fun deleteJobMsg(message : String){
        try{
            customSchedulerManager.deleteScheduledJobs()
            logger.info("delete all job successfully!")
        } catch (e : Exception) {
            logger.error("delete all jobs fail!")
        }
    }

    fun initJobMsg(message : String){
        try{
            customSchedulerManager.manualInitialize()
            logger.info("manual initialize all job successfully!")
        } catch (e : Exception){
            logger.error("manual initialize all job fail!")
        }
    }
}