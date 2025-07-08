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

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.model.CCNNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.ChartAverageEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.CcnDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.CcnDefectStatisticModelBuilder;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.AbstractDefectStatisticService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDataReportRspVO;
import com.tencent.bk.codecc.defect.vo.ChartAverageVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 圈复杂度工具告警统计类
 * 遍历逻辑由抽象类实现
 *
 * @author warmli
 */
@Component
@Slf4j
public class CCNDefectStatisticServiceImpl
        extends AbstractDefectStatisticService<CCNDefectEntity, CcnDefectStatisticModel> {
    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    public ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private CheckerService checkerService;
    @Autowired
    private CommonKafkaClient commonKafkaClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    @Override
    public CcnDefectStatisticModel buildStatisticModel(DefectStatisticModel<CCNDefectEntity> defectStatisticModel) {

        // 获取各严重级别定义
        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        if (riskConfigMap == null) {
            log.error("Has not init risk factor config! {} {}",
                    defectStatisticModel.getTaskDetailVO().getTaskId(),
                    defectStatisticModel.getBuildId());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        // 获取超标圈复杂度阈值，优先从规则里面取，取不到从个性化参数里面取，再取不到就是用默认值
        ToolConfigInfoVO toolConfigInfoVO = defectStatisticModel.getTaskDetailVO()
                .getToolConfigInfoList()
                .stream()
                .filter(toolConfig -> toolConfig.getToolName().equalsIgnoreCase(ComConstants.Tool.CCN.name()))
                .findAny()
                .orElseGet(ToolConfigInfoVO::new);
        int ccnThreshold = checkerService.getCcnThreshold(toolConfigInfoVO);

        return new CcnDefectStatisticModelBuilder()
                .sh(sh)
                .h(h)
                .m(m)
                .ccnThreshold(ccnThreshold)
                .averageCcn(defectStatisticModel.getAverageCcn())
                .taskId(defectStatisticModel.getTaskDetailVO().getTaskId())
                .toolName(defectStatisticModel.getToolName())
                .createFrom(defectStatisticModel.getTaskDetailVO().getCreateFrom())
                .buildId(defectStatisticModel.getBuildId())
                .allDefectList(defectStatisticModel.getDefectList())
                .fastIncrementFlag(defectStatisticModel.getFastIncrementFlag())
                .build();
    }

    /**
     * 判断告警是否是 "待修复" 状态，statisticService 只统计待修复告警
     *
     * @param defectEntity 抽象告警实体类
     * @return 返回 true 时代表当前告警状态是 "待修复"
     */
    @Override
    public boolean isStatusNew(CCNDefectEntity defectEntity) {
        return defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value();
    }

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefect(CCNDefectEntity defectEntity, CcnDefectStatisticModel statisticModel) {
        int diff = defectEntity.getCcn() - statisticModel.getCcnThreshold();
        if (diff > 0) {
            statisticModel.setCcnBeyondThresholdSum(statisticModel.getCcnBeyondThresholdSum() + diff);
        }
        int riskValue = getRiskFactorVal(defectEntity.getCcn(),
                statisticModel.getSh(),
                statisticModel.getH(),
                statisticModel.getM());
        statisticModel.setRiskValue(riskValue);
        statisticModel.incExistCount();
    }

    /**
     * 统计新增告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticNewDefect(CCNDefectEntity defectEntity, CcnDefectStatisticModel statisticModel) {
        CCNNotRepairedAuthorEntity authorStatistic = statisticModel.getNewAuthorMap().get(defectEntity.getAuthor());
        if (authorStatistic == null) {
            authorStatistic = new CCNNotRepairedAuthorEntity();
            authorStatistic.setName(defectEntity.getAuthor());
            statisticModel.getNewAuthorMap().put(defectEntity.getAuthor(), authorStatistic);
        }

        if (ComConstants.RiskFactor.SH.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewSuperHighCount();
            authorStatistic.setSuperHighCount(authorStatistic.getSuperHighCount() + 1);
        } else if (ComConstants.RiskFactor.H.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewHighCount();
            authorStatistic.setHighCount(authorStatistic.getHighCount() + 1);
        } else if (ComConstants.RiskFactor.M.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewMediumCount();
            authorStatistic.setMediumCount(authorStatistic.getMediumCount() + 1);
        } else if (ComConstants.RiskFactor.L.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewLowCount();
            authorStatistic.setLowCount(authorStatistic.getLowCount() + 1);
        }
    }

    /**
     * 统计遗留告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticOldDefect(CCNDefectEntity defectEntity, CcnDefectStatisticModel statisticModel) {
        CCNNotRepairedAuthorEntity authorStatistic = statisticModel.getExistAuthorMap().get(defectEntity.getAuthor());
        if (authorStatistic == null) {
            authorStatistic = new CCNNotRepairedAuthorEntity();
            authorStatistic.setName(defectEntity.getAuthor());
            statisticModel.getExistAuthorMap().put(defectEntity.getAuthor(), authorStatistic);
        }

        if (ComConstants.RiskFactor.SH.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldSuperHighCount();
            authorStatistic.setSuperHighCount(authorStatistic.getSuperHighCount() + 1);
        } else if (ComConstants.RiskFactor.H.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldHighCount();
            authorStatistic.setHighCount(authorStatistic.getHighCount() + 1);
        } else if (ComConstants.RiskFactor.M.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldMediumCount();
            authorStatistic.setMediumCount(authorStatistic.getMediumCount() + 1);
        } else if (ComConstants.RiskFactor.L.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldLowCount();
            authorStatistic.setLowCount(authorStatistic.getLowCount() + 1);
        }
    }

    /**
     * 统计相对于上一次扫描的告警变动数
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefectChange(CcnDefectStatisticModel statisticModel) {
        CCNStatisticEntity baseBuildCcnStatistic = ccnStatisticRepository.findFirstByTaskIdAndBuildId(
                statisticModel.getTaskId(), statisticModel.getBaseBuildId());
        if (baseBuildCcnStatistic != null) {
            statisticModel.setDefectChange(
                    statisticModel.getExistCount() - (baseBuildCcnStatistic.getDefectCount() == null
                            ? 0 : baseBuildCcnStatistic.getDefectCount()));
            statisticModel.setAverageCcnChange(
                    statisticModel.getAverageCcn() - (baseBuildCcnStatistic.getAverageCCN() == null
                            ? 0 : baseBuildCcnStatistic.getAverageCCN()));
            statisticModel.setLastDefectCount(baseBuildCcnStatistic.getDefectCount());
            statisticModel.setLastAverageCcn(baseBuildCcnStatistic.getAverageCCN());
        } else {
            statisticModel.setDefectChange(statisticModel.getExistCount());
            statisticModel.setAverageCcnChange(statisticModel.getAverageCcn());
            statisticModel.setLastDefectCount(0);
            statisticModel.setLastAverageCcn(0.0F);
        }
    }

    /**
     * 统计所有"待修复"告警的规则信息
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChecker(CcnDefectStatisticModel statisticModel) {

    }

    /**
     * 统计告警图表数据,DUPC CCN
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChart(CcnDefectStatisticModel statisticModel) {
        // 获取最近5日平均圈复杂度趋势数据，由于需要使用最新统计结果，所以先保存再获取趋势数据然后再次保存
        List<ChartAverageEntity> averageList = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(
                statisticModel.getToolName(),
                ComConstants.BusinessType.DATA_REPORT.value(),
                IDataReportBizService.class);
        CCNDataReportRspVO ccnDataReportRspVO = (CCNDataReportRspVO) dataReportBizService
                .getDataReport(statisticModel.getTaskId(),
                        statisticModel.getToolName(),
                        5,
                        null,
                        null);
        if (ccnDataReportRspVO != null) {
            //平均圈复杂度按日期从早到晚排序
            ccnDataReportRspVO.getChartAverageList()
                    .getAverageList()
                    .sort(Comparator.comparing(ChartAverageVO::getDate));

            //平均圈复杂度图表数值保留两位小数
            ccnDataReportRspVO.getChartAverageList()
                    .getAverageList()
                    .forEach(chartAverageVO -> {
                        BigDecimal averageCcnBd = BigDecimal.valueOf(chartAverageVO.getAverageCCN());
                        chartAverageVO.setAverageCCN(averageCcnBd.setScale(2, RoundingMode.HALF_DOWN)
                                .floatValue());
                    });
            //本次扫描是今天的最新数值
            String currentDate = LocalDate.now().toString();
            averageList.addAll(ccnDataReportRspVO.getChartAverageList()
                    .getAverageList()
                    .stream()
                    .map(chartAverageVO -> {
                        ChartAverageEntity chartAverageEntity = new ChartAverageEntity();
                        if (chartAverageVO.getDate().equals(currentDate)) {
                            BigDecimal averageCcnBd = BigDecimal.valueOf(statisticModel.getAverageCcn());
                            chartAverageVO.setAverageCCN(averageCcnBd.setScale(2, RoundingMode.HALF_DOWN)
                                    .floatValue());
                        }
                        BeanUtils.copyProperties(chartAverageVO, chartAverageEntity);
                        return chartAverageEntity;
                    }).collect(Collectors.toList()));
        }
        statisticModel.setAverageList(averageList);
    }

    /**
     * 将 statisticModel 转化为 statisticEntity，保存到对应表
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void buildAndSaveStatisticResult(CcnDefectStatisticModel statisticModel) {
        // 查询是否存在与当前相同标识的数据
        CCNStatisticEntity ccnStatisticEntity = ccnStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
                statisticModel.getTaskId(), statisticModel.getToolName(), statisticModel.getBuildId()
        );
        // 将当前数据进行转换，如果数据库存在，则进行修改，不存在进行新增
        CCNStatisticEntity saveCcnStatisticEntity = statisticModel.getBuilder().convert(ccnStatisticEntity);

        statisticModel.setCcnStatisticEntity(saveCcnStatisticEntity);
        ccnStatisticRepository.save(saveCcnStatisticEntity);
    }

    /**
     * 异步统计 "已修复"、"已屏蔽"、"已忽略" 状态的告警
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void asyncStatisticDefect(CcnDefectStatisticModel statisticModel) {
        CCNStatisticEntity mqObj = statisticModel.getCcnStatisticEntity();
        mqObj.setFastIncrementFlag(statisticModel.getFastIncrementFlag());
        mqObj.setBaseBuildId(statisticModel.getBaseBuildId());

        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(statisticModel.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_CCN_OPENSOURCE, mqObj);
        } else {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_CCN, ROUTE_CLOSE_DEFECT_STATISTIC_CCN,
                    mqObj);
        }
    }

    /**
     * 将统计数据推送到数据平台
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void pushDataKafka(CcnDefectStatisticModel statisticModel) {
        commonKafkaClient.pushCCNStatisticToKafka(statisticModel.getCcnStatisticEntity());
    }

    /**
     * 根据复杂度获取风险等级枚举值
     */
    private int getRiskFactorVal(int ccn, int sh, int h, int m) {
        if (ccn >= sh) {
            return ComConstants.RiskFactor.SH.value();
        } else if (ccn < sh && ccn >= h) {
            return ComConstants.RiskFactor.H.value();
        } else if (ccn < h && ccn >= m) {
            return ComConstants.RiskFactor.M.value();
        } else {
            return ComConstants.RiskFactor.L.value();
        }
    }
}
