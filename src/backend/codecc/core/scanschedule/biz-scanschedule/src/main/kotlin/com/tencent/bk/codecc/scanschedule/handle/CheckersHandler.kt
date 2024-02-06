package com.tencent.bk.codecc.scanschedule.handle

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource
import com.tencent.bk.codecc.scanschedule.constants.ScanConstants
import com.tencent.bk.codecc.scanschedule.pojo.input.CheckerOptions
import com.tencent.bk.codecc.scanschedule.pojo.input.OpenCheckers
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord
import com.tencent.bk.codecc.scanschedule.vo.SimpleCheckerSetVO
import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CheckersHandler @Autowired constructor(
    private val client: Client
) {

    /**
     * 根据规则集名称获取规则集列表信息
     * @param toolName
     * @param checkerSetVOList
     * @return
     */
    fun getCheckersForToolName(toolName: String, checkerSetVOList: List<CheckerSetVO>): List<OpenCheckers> {
        val toolOpenCheckers: MutableSet<OpenCheckers> = mutableSetOf()

        for (checkerSetVO in checkerSetVOList) {
            // 1.规则集中是否包含该工具？
            if (checkerSetVO.toolList.contains(toolName)) {
                // 2.筛选对应工具的规则
                for (checkerPropVO in checkerSetVO.checkerProps.filter { it.toolName == toolName }) {
                    val openCheckers = OpenCheckers()
                    // 3.获取规则名称
                    openCheckers.checkerName = checkerPropVO.checkerKey
                    // 4.获取规则属性
                    if (StringUtils.isNotBlank(checkerPropVO.props)) {
                        val props = JsonUtil.fromJson(checkerPropVO.props, ArrayList<HashMap<*, *>>()::class.java)
                        val checkerOptionsList = props?.map { propMap ->
                            CheckerOptions().apply {
                                checkerOptionName = propMap["propName"] as String?
                                checkerOptionValue = propMap["propValue"] as String?
                            }
                        } ?: emptyList()

                        if (checkerOptionsList.isNotEmpty()) {
                            openCheckers.checkerOptions = checkerOptionsList
                        }
                    }
                    // 5.添加到列表中
                    toolOpenCheckers.add(openCheckers)
                }
            }
        }
        return toolOpenCheckers.toList()
    }

    /**
     * 根据规则集名称获取规则集列表信息
     * @param scanRecord
     * @return
     */
    fun queryCheckerDetailForSet(scanRecord: ScanRecord): List<CheckerSetVO?>? {
        // 1.获取规则集
        val checkerSets: MutableList<SimpleCheckerSetVO> = scanRecord.checkerSets?.toMutableList() ?: mutableListOf()
        if (checkerSets.isEmpty()) {
            val defaultCheckerSet = SimpleCheckerSetVO().apply {
                checkerSet = ScanConstants.SCAN_SCHEDULE_DEFAULT_CHECKER_SET
                checkerSetVersion = ScanConstants.DEFAULT_TOOL_VERSION
            }
            checkerSets.add(defaultCheckerSet)
        }

        // 2.调用规则接口获取对应规则列表
        val result = client.get(ServiceCheckerSetRestResource::class)
            .getCheckerSetsForContent(checkerSets.map { it.checkerSet })
        if (result.isNotOk() || CollectionUtils.isEmpty(result.data)) {
            throw CodeCCException("select checker set $checkerSets data is null")
        }

        return result.data ?: emptyList()
    }
}