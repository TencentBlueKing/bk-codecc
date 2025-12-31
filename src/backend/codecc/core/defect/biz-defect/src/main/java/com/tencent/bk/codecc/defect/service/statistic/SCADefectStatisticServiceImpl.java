/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.statistic;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.statistic.SCAStatisticRepository;
import com.tencent.bk.codecc.defect.model.sca.SCAVulnerabilityEntity;
import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.SCADefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.SCADefectStatisticModelBuilder;
import com.tencent.bk.codecc.defect.service.AbstractDefectStatisticService;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SCA告警统计
 *
 * @author ruitaoyuan
 */
@Component
@Slf4j
public class SCADefectStatisticServiceImpl extends AbstractDefectStatisticService<SCAVulnerabilityEntity,
        SCADefectStatisticModel> {

    @Autowired
    private SCAStatisticRepository scaStatisticRepository;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    @Override
    public SCADefectStatisticModel buildStatisticModel(
            DefectStatisticModel<SCAVulnerabilityEntity> defectStatisticModel) {
        return new SCADefectStatisticModelBuilder()
                .sbomAggregateModel(defectStatisticModel.getSbomAggregateModel())
                .fastIncrementFlag(defectStatisticModel.getFastIncrementFlag())
                .taskId(defectStatisticModel.getTaskDetailVO().getTaskId())
                .toolName(defectStatisticModel.getToolName())
                .createFrom(defectStatisticModel.getTaskDetailVO().getCreateFrom())
                .buildId(defectStatisticModel.getBuildId())
                .allDefectList(defectStatisticModel.getDefectList())
                .build();
    }

    /**
     * 判断告警是否是 "待修复" 状态，statisticService 只统计待修复告警
     *
     * @param defectEntity 抽象告警实体类
     * @return 返回 true 时代表当前告警状态是 "待修复"
     */
    @Override
    public boolean isStatusNew(SCAVulnerabilityEntity defectEntity) {
        return defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value();
    }

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefect(SCAVulnerabilityEntity defectEntity, SCADefectStatisticModel statisticModel) {
    }

    /**
     * 统计新增告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticNewDefect(SCAVulnerabilityEntity defectEntity, SCADefectStatisticModel statisticModel) {
        statisticModel.incNewVulCount();
        if (defectEntity.getSeverity() == ComConstants.SERIOUS) {
            statisticModel.incNewHighVulCount();
        } else if (defectEntity.getSeverity() == ComConstants.NORMAL) {
            statisticModel.incNewMediumVulCount();
        } else if (defectEntity.getSeverity() == ComConstants.PROMPT
                || defectEntity.getSeverity() == ComConstants.PROMPT_IN_DB) {
            statisticModel.incNewLowVulCount();
        } else {
            statisticModel.incNewUnknownVulCount();
        }
    }

    @Override
    public void statisticOldDefect(SCAVulnerabilityEntity defectEntity, SCADefectStatisticModel statisticModel) {

    }

    @Override
    public void statisticDefectChange(SCADefectStatisticModel statisticModel) {

    }

    @Override
    public void statisticChecker(SCADefectStatisticModel statisticModel) {

    }

    @Override
    public void statisticChart(SCADefectStatisticModel statisticModel) {

    }

    @Override
    public void buildAndSaveStatisticResult(SCADefectStatisticModel statisticModel) {
        SCAStatisticEntity scaStatistic = scaStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
                statisticModel.getTaskId(), statisticModel.getToolName(), statisticModel.getBuildId()
        );
        SCAStatisticEntity saveSCAStatistic = statisticModel.getBuilder().convert(scaStatistic);
        statisticModel.setScaStatisticEntity(saveSCAStatistic);
        scaStatisticRepository.save(saveSCAStatistic);
    }

    @Override
    public void asyncStatisticDefect(SCADefectStatisticModel statisticModel) {

    }

    @Override
    public void pushDataKafka(SCADefectStatisticModel statisticModel) {

    }
}
