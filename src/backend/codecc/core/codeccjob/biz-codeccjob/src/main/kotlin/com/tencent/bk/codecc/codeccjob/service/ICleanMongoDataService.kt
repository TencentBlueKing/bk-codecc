package com.tencent.bk.codecc.codeccjob.service

interface ICleanMongoDataService {
    fun clean(projectId: String, taskId: Long, obsoleteBuildIdList: List<String>, taskToolList: List<String>)
}
