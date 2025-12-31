package com.tencent.bk.codecc.defect.cluster

import com.tencent.bk.codecc.defect.component.AggregateDebugService
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity
import com.tencent.bk.codecc.defect.pojo.AggregateDefectOutputModelV2
import com.tencent.devops.common.web.aop.annotation.LogTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Component("clusterLintCompareProcess")
class ClusterLintCompareProcess @Autowired constructor(
    private val aggregateDebugService: AggregateDebugService
) : AbstractClusterCompareProcess<LintDefectV2Entity>() {

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterCompareProcess::class.java)
    }

    override fun getPinpointHashMap(aggregateDefectInputList: List<LintDefectV2Entity>): Map<String?, List<LintDefectV2Entity>> {
        return aggregateDefectInputList.groupBy { it.pinpointHash }
    }

    override fun getLineNumList(aggregateDefectInputList: List<LintDefectV2Entity>): Set<Int>? {
        val lineNumSet = mutableSetOf<Int>()
        aggregateDefectInputList.forEach {
            lineNumSet.add(it.lineNum)
        }
        return lineNumSet
    }

    /**
     * 聚类主方法
     * @param inputDefectList 输入告警清单
     * @param md5SameMap md5相同映射
     */
    @LogTime(threshold = 15, unit = TimeUnit.MINUTES)
    @ExperimentalUnsignedTypes
    override fun clusterMethod(
        inputDefectList: List<LintDefectV2Entity>,
        md5SameMap: MutableMap<String, Boolean>
    ): CopyOnWriteArrayList<AggregateDefectOutputModelV2<LintDefectV2Entity>> {
        logger.info("input defect size: ${inputDefectList.size}")
        val outputFileList = CopyOnWriteArrayList<AggregateDefectOutputModelV2<LintDefectV2Entity>>()
        //1.根据文件名和规则名分组，不同的文件名和规则名不聚类在一起
        logger.info("start to distinct input defect list!")
        val inputDefectMap = inputDefectList.groupBy { it.filePath to it.checker }
        val executor = getThreadPool(inputDefectMap.size)

        try {
            val lock = CountDownLatch(inputDefectMap.size)
            //2. 根据分组分别进行配置属性和并且利用线程池并行比较（**优化点）
            logger.info("start to config properties and compare!")
            inputDefectMap.forEach { (t, u) ->
                executor.execute {
                    try {
                        // 判断该文件的新老告警的md5是否一致，如果一致的话，则不需要进行pinpointhash比较，只要进行行号的比较
                        val filePath = t.first
                        val md5Same = md5SameMap[filePath]
                        val outputFileMap = if (null != md5Same && md5Same) {
                            u.groupBy { it.lineNum }
                        } else {
                            configAndCompare(u)
                        }
                        //
                        var defectOutputList = outputFileMap.map {
                            AggregateDefectOutputModelV2(
                                defects = it.value
                            )
                        }

                        // 如果符合新的聚合方式，就是用新的聚合进一步分组
                        val defect = u.firstOrNull { !it.newDefect }
                        if (defect != null) {
                            val taskId = defect.taskId
                            val toolName = defect.toolName.orEmpty()
                            val checker = defect.checker.orEmpty()
                            if (aggregateDebugService.isMatchNewAggregate(taskId, toolName, checker)) {
                                defectOutputList =
                                    aggregateDebugService.regroupDefectsByMessage(
                                        taskId,
                                        toolName,
                                        checker,
                                        defectOutputList
                                    )
                            }
                        }

                        // 使用同步方法块保证，LinkedList在多线程addAll过程中的安全性
                        synchronized(outputFileList) {
                            outputFileList.addAll(defectOutputList)
                        }
                    } catch (e: Exception) {
                        logger.info("config and compare elements fail! index is $t, msg is${e.message}")
                    } finally {
                        lock.countDown()
                    }
                }
            }
            // 定时,超时2个小时自动超时
            lock.await(2, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
            logger.info(
                "execute cluster process fail!CountDownLatch Await More Than 2 Hours!InputDefectList " +
                        "Size:${inputDefectList.size}，Exception Msg is ${e.message}"
            )
        } catch (e: Exception) {
            logger.info("execute cluster process fail!")
        } finally {
            executor.shutdownNow()
        }
        return outputFileList
    }

}
