package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.openapi.v2.ApigwToolScanResourceV2
import com.tencent.bk.codecc.scanschedule.api.ServiceScanResource
import com.tencent.bk.codecc.scanschedule.vo.ContentVO
import com.tencent.bk.codecc.scanschedule.vo.ScanResultVO
import com.tencent.devops.common.api.pojo.codecc.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwToolScanResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwToolScanResourceV2 {

    override fun scan(appCode: String?, scanContentVO: ContentVO?): Result<ScanResultVO> {
        return client.getWithoutRetry(ServiceScanResource::class).scan(appCode, scanContentVO)
    }
}