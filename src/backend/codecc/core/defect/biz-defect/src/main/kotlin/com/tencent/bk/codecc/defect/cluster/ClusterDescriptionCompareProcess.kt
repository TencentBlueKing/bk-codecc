package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

@Component
class ClusterDescriptionCompareProcess: AbstractClusterCompareProcess<LintDefectV2Entity>() {
    /**
     * 聚类方法
     */
    override fun clusterMethod(
        inputDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): List<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        val outputFileList =
            CopyOnWriteArrayList<AggregateDefectOutputModelV2<LintDefectV2Entity>>()
        val defectMap = inputDefectList.groupBy {
            it.filePath to it.checker
        }
        defectMap.forEach { (_, u) ->
            val outputFileMap = u.groupBy { it.pinpointHash }
            outputFileList.addAll(outputFileMap.map {
                AggregateDefectOutputModelV2(defects = it.value)
            })
        }
        return outputFileList
    }

    /**
     * 获取特征值映射方法，每个类型方法不一致
     */
    override fun getPinpointHashMap(aggregateDefectInputList: List<LintDefectV2Entity>): Map<String?, List<LintDefectV2Entity>> {
        return mapOf()
    }

    override fun getLineNumList(aggregateDefectInputList: List<LintDefectV2Entity>): Set<Int>? {
        return null
    }
}
