package com.tencent.bk.codecc.scanschedule.handle

import com.tencent.bk.codecc.task.api.ServiceToolMetaRestResource
import com.tencent.devops.common.api.ToolMetaDetailVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import org.apache.commons.collections.CollectionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ToolMetaHandler @Autowired constructor(
    private val client: Client
) {

    /**
     * 筛选存在二进制的工具列表
     * @param toolMetaDetailVOList
     * @return
     */
    fun filterToolMetaByEnableBinary(toolMetaDetailVOList: List<ToolMetaDetailVO>): List<ToolMetaDetailVO> {
        return toolMetaDetailVOList.filter { it.binary != null }
    }

    /**
     * 根据规则集获取工具列表
     * @param checkerSetVOList
     * @return
     */
    fun queryToolDetailForCheckerSet(checkerSetVOList: List<CheckerSetVO>): List<ToolMetaDetailVO?>? {
        val toolNameList = checkerSetVOList.flatMap { it.toolList }.toList()
        val result = client.get(ServiceToolMetaRestResource::class.java)
            .queryToolMetaDataByToolName(toolNameList)

        if (result.isNotOk() || CollectionUtils.isEmpty(result.data)) {
            throw CodeCCException("select tool meta $toolNameList data is null")
        }

        return result.data ?: emptyList()
    }
}