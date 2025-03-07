package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.statistic.SCAStatisticRepository;
import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity;
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.SCALastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service("SCAQueryStatisticBizService")
public class SCAQueryStatisticBizServiceImpl implements IQueryStatisticBizService {

    @Autowired
    private SCAStatisticRepository scaStatisticRepository;

    @Override
    public BaseLastAnalysisResultVO processBiz(ToolLastAnalysisResultVO arg, boolean isLast) {
        long taskId = arg.getTaskId();
        String toolName = arg.getToolName();
        String buildId = arg.getBuildId();

        SCAStatisticEntity statisticEntity;
        if (isLast) {
            statisticEntity = scaStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
        } else {
            statisticEntity = scaStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        }
        SCALastAnalysisResultVO lastAnalysisResultVO = new SCALastAnalysisResultVO();
        if (statisticEntity != null) {
            BeanUtils.copyProperties(statisticEntity, lastAnalysisResultVO);
        }

        lastAnalysisResultVO.setPattern(ComConstants.ToolPattern.SCA.name());

        return lastAnalysisResultVO;
    }
}