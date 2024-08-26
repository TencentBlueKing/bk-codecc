package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.task.dao.mongorepository.AnalyzeCountStatRepository;
import com.tencent.bk.codecc.task.model.AnalyzeCountStatEntity;
import com.tencent.bk.codecc.task.service.AnalyzeCountStatService;
import com.tencent.devops.common.constant.ComConstants;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalyzeCountStatServiceImpl implements AnalyzeCountStatService {

    @Autowired
    private AnalyzeCountStatRepository analyzeCountStatRepository;

    @Override
    public Map<Integer, List<Long>> getToolFailedTaskId(String toolName, Set<String> createFrom, String detailTime) {
        Map<Integer, List<Long>> taskIds = Maps.newHashMap();
        List<AnalyzeCountStatEntity> analyzeCountStatEntities =
                analyzeCountStatRepository.findByDateAndDataFromInAndToolName(detailTime, createFrom, toolName);
        if (CollectionUtils.isNotEmpty(analyzeCountStatEntities)) {
            for (AnalyzeCountStatEntity analyzeCountStat: analyzeCountStatEntities) {
                // 如果key第一次存key
                if (taskIds.containsKey(analyzeCountStat.getStatus())) {
                    taskIds.get(analyzeCountStat.getStatus()).addAll(analyzeCountStat.getTaskIdList());
                } else {
                    taskIds.put(analyzeCountStat.getStatus(), analyzeCountStat.getTaskIdList());
                }
            }
        }
        return taskIds;
    }
}
