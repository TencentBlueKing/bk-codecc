/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildInfoDao;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CodeLang;
import com.tencent.devops.common.constant.ComConstants.StepFlag;
import com.tencent.devops.common.service.BaseDataCacheService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * cloc查询代码行数服务
 *
 * @version V1.0
 * @date 2020/3/31
 */
@Slf4j
@Service
public class CLOCQueryCodeLineServiceImpl implements ICLOCQueryCodeLineService {

    @Autowired
    private CLOCDefectDao clocDefectDao;
    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;
    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    @Autowired
    private ToolBuildInfoDao toolBuildInfoDao;
    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Override
    public ToolClocRspVO getCodeLineInfo(Long taskId, String toolName) {
        List<CodeLineModel> codeLineModelList = clocDefectDao.getCodeLineInfo(taskId, toolName);
        ToolClocRspVO toolClocRspVO = new ToolClocRspVO();
        toolClocRspVO.setTaskId(taskId);
        toolClocRspVO.setCodeLineList(codeLineModelList);
        return toolClocRspVO;
    }

    /**
     * 按任务ID统计总代码行数
     *
     * @param taskIds 任务ID集合
     * @return int
     */
    @Override
    public Long queryCodeLineByTaskIds(Collection<Long> taskIds) {
        long codeLine = 0;
        if (CollectionUtils.isEmpty(taskIds)) {
            return codeLine;
        }

//        List<TaskLogOverviewEntity> taskLogOverviewEntityList =
//                taskLogOverviewDao.findBuildIdsByStartTime(taskIds, null, null, null);
        List<ToolBuildInfoEntity> toolBuildInfoEntities = toolBuildInfoDao.findLatestBuildIdByTaskIdSet(taskIds);
        if (CollectionUtils.isEmpty(toolBuildInfoEntities)) {
            return codeLine;
        }
        List<String> buildIds = toolBuildInfoEntities.stream().map(ToolBuildInfoEntity::getDefectBaseBuildId)
                .collect(Collectors.toList());

        List<CLOCStatisticEntity> statisticEntityList =
                clocStatisticsDao.batchStatClocStatisticByTaskId(taskIds, buildIds);

        return statisticEntityList.stream().map(item -> item.getSumCode() + item.getSumBlank() + item.getSumComment())
                .reduce(Long::sum).orElse(0L);
    }

    @Override
    public CLOCDefectQueryRspInfoVO generateSpecificLanguage(long taskId, String toolName, String language) {
        CLOCStatisticEntity clocStatisticEntity = clocStatisticRepository
                .findFirstByTaskIdAndToolNameAndLanguageOrderByUpdatedDateDesc(taskId, toolName, language);
        CLOCDefectQueryRspInfoVO clocDefectQueryRspInfoVO = new CLOCDefectQueryRspInfoVO();
        if (null != clocStatisticEntity) {
            clocDefectQueryRspInfoVO.setLanguage(language);
            clocDefectQueryRspInfoVO.setSumCode(clocStatisticEntity.getSumCode());
            clocDefectQueryRspInfoVO.setSumBlank(clocStatisticEntity.getSumBlank());
            clocDefectQueryRspInfoVO.setSumComment(clocStatisticEntity.getSumComment());
        }
        return clocDefectQueryRspInfoVO;
    }

    @Override
    public List<CodeLineModel> queryCodeLineByTaskId(Long taskId) {
        log.info("start to query task code lang:{}", taskId);
        // 获取最近扫描成功的BuildId
        TaskLogEntity taskLog = taskLogRepository
                .findFirstByTaskIdAndToolNameAndFlagOrderByStartTimeDesc(taskId, ComConstants.Tool.SCC.name(),
                        StepFlag.SUCC.value());
        if (taskLog == null || StringUtils.isBlank(taskLog.getBuildId())) {
            log.info("end of query task code lang:{}. task log empty", taskId);
            return Collections.emptyList();
        }
        //
        String buildId = taskLog.getBuildId();
        List<CLOCStatisticEntity> clocStatistics = clocStatisticRepository
                .findByTaskIdAndToolNameAndBuildId(taskId, ComConstants.Tool.SCC.name(), buildId);
        if (CollectionUtils.isEmpty(clocStatistics)) {
            log.info("end of query task code lang:{}. clocStatistics empty", taskId);
            return Collections.emptyList();
        }
        List<CodeLineModel> vos = new ArrayList<>();
        Map<String, Long> langToValueMap = baseDataCacheService.getLangToValueMap();
        for (CLOCStatisticEntity clocStatistic : clocStatistics) {
            long langValue = langToValueMap.getOrDefault(clocStatistic.getLanguage(), CodeLang.OTHERS.langValue());
            vos.add(new CodeLineModel(clocStatistic.getLanguage(), clocStatistic.getSumCode(),
                    clocStatistic.getSumComment(), clocStatistic.getSumEfficientComment(), clocStatistic.getSumBlank(),
                    langValue, clocStatistic.getFileNum()));
        }
        log.info("end of query task code lang:{}.vos size:{} ", taskId, vos.size());
        return vos;
    }
}
