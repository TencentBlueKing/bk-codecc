package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.task.dao.mongorepository.AnalyzeCountStatRepository;
import com.tencent.bk.codecc.task.model.AnalyzeCountStatEntity;
import com.tencent.bk.codecc.task.service.AnalyzeCountStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        analyzeCountStatEntities.forEach(analyzeCountStat ->
                taskIds.computeIfAbsent(analyzeCountStat.getStatus(), k -> new ArrayList<>())
                        .addAll(analyzeCountStat.getTaskIdList()));
        return taskIds;
    }
}
