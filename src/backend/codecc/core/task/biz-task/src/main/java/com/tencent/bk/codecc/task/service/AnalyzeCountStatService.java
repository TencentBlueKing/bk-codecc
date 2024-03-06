package com.tencent.bk.codecc.task.service;


import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具失败统计数据接口
 */
public interface AnalyzeCountStatService {


    /**
     * 根据时间、创建来源、工具名获取失败id
     * @return (工具名, [任务id])
     */
    Map<Integer, List<Long>> getToolFailedTaskId(String toolName, Set<String> createFrom, String detailTime);
}
