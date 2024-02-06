package com.tencent.bk.codecc.defect.service.statistic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.model.DUPCNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.DUPCScanSummaryEntity;
import com.tencent.bk.codecc.defect.model.DupcChartTrendEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DupcDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DupcDefectStatisticModelBuilder;
import com.tencent.bk.codecc.defect.service.AbstractDefectStatisticService;
import com.tencent.bk.codecc.defect.service.IDataReportBizService;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.bk.codecc.defect.vo.DupcChartTrendVO;
import com.tencent.bk.codecc.defect.vo.DupcDataReportRspVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 重复率工具告警统计类
 * 遍历逻辑由抽象类实现
 *
 * @author warmli
 */
@Slf4j
@Service
public class DupcDefectStatisticServiceImpl
        extends AbstractDefectStatisticService<DUPCDefectEntity, DupcDefectStatisticModel> {

    
    @Autowired
    private CommonKafkaClient commonKafkaClient;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private BizServiceFactory<IDataReportBizService> dataReportBizServiceBizServiceFactory;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    @Override
    public DupcDefectStatisticModel buildStatisticModel(DefectStatisticModel<DUPCDefectEntity> defectStatisticModel) {
        Map<String, String> riskConfigMap = defectStatisticModel.getRiskConfigMap();
        float sh = Float.parseFloat(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        float h = Float.parseFloat(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        float m = Float.parseFloat(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        return new DupcDefectStatisticModelBuilder()
                .sh(sh)
                .h(h)
                .m(m)
                .defectJsonFileEntity(defectStatisticModel.getDefectJsonFileEntity())
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
    public boolean isStatusNew(DUPCDefectEntity defectEntity) {
        return ComConstants.DefectStatus.NEW.value() == defectEntity.getStatus();
    }

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefect(DUPCDefectEntity defectEntity, DupcDefectStatisticModel statisticModel) {
        // 根据重复率计算重复严重程度 riskValue
        float dupcRate = defectEntity.getDupRateValue();
        int riskValue = getRiskFactorVal(
                dupcRate, statisticModel.getSh(), statisticModel.getH(), statisticModel.getM());
        // 获取当前告警处理人
        Set<String> authorSet = null;
        try {
            authorSet = StringUtils.isEmpty(defectEntity.getAuthorList()) ? Collections.emptySet()
                    : Sets.newHashSet(StringUtils.split(defectEntity.getAuthorList(), ";"));
        } catch (Exception e) {
            log.error("get dupc author list fail, source string: {} {} {}",
                    statisticModel.getTaskId(),
                    statisticModel.getBuildId(),
                    defectEntity.getAuthorList(),
                    e);
        }

        statisticModel.setAuthorSet(authorSet);
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
    public void statisticNewDefect(DUPCDefectEntity defectEntity, DupcDefectStatisticModel statisticModel) {
        if (ComConstants.RiskFactor.SH.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewSuperHighCount();
        } else if (ComConstants.RiskFactor.H.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewHighCount();
        } else if (ComConstants.RiskFactor.M.value() == statisticModel.getRiskValue()) {
            statisticModel.incNewMediumCount();
        }

        if (CollectionUtils.isEmpty(statisticModel.getAuthorSet())) {
            return;
        }

        for (String author : statisticModel.getAuthorSet()) {
            String name = author.trim();
            DUPCNotRepairedAuthorEntity authorEntity = statisticModel.getNewAuthorMap().get(name);
            if (authorEntity == null) {
                authorEntity = new DUPCNotRepairedAuthorEntity();
                authorEntity.setName(name);
                statisticModel.getNewAuthorMap().put(name, authorEntity);
            }

            statisticAuthorDefectCount(statisticModel, authorEntity);
        }
    }

    /**
     * 统计遗留告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticOldDefect(DUPCDefectEntity defectEntity, DupcDefectStatisticModel statisticModel) {
        if (ComConstants.RiskFactor.SH.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldSuperHighCount();
        } else if (ComConstants.RiskFactor.H.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldHighCount();
        } else if (ComConstants.RiskFactor.M.value() == statisticModel.getRiskValue()) {
            statisticModel.incOldMediumCount();
        }

        if (CollectionUtils.isEmpty(statisticModel.getAuthorSet())) {
            return;
        }

        for (String author : statisticModel.getAuthorSet()) {
            String name = author.trim();
            DUPCNotRepairedAuthorEntity authorEntity = statisticModel.getExistAuthorMap().get(name);
            if (authorEntity == null) {
                authorEntity = new DUPCNotRepairedAuthorEntity();
                authorEntity.setName(name);
                statisticModel.getExistAuthorMap().put(name, authorEntity);
            }

            statisticAuthorDefectCount(statisticModel, authorEntity);
        }
    }

    /**
     * 统计相对于上一次扫描的告警变动数
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefectChange(DupcDefectStatisticModel statisticModel) {
        // 计算平均重复率
        long dupLineCount = statisticModel.getDefectJsonFileEntity().getDupLineCount();
        long rawlineCount = statisticModel.getDefectJsonFileEntity().getTotalLineCount();
        float dupRate = 0.00F;
        if (rawlineCount != 0) {
            dupRate = (float) dupLineCount * 100 / rawlineCount;
        }

        DUPCScanSummaryEntity dupcScanSummary = new DUPCScanSummaryEntity();
        dupcScanSummary.setRawlineCount(rawlineCount);
        dupcScanSummary.setDupLineCount(dupLineCount);
        statisticModel.setDupcScanSummary(dupcScanSummary);
        statisticModel.setDupRate(dupRate);

        // 计算重复率变化
        DUPCStatisticEntity baseStatisticEntity = dupcStatisticRepository.findFirstByTaskIdAndBuildId(
                statisticModel.getTaskId(), statisticModel.getBaseBuildId());
        if (baseStatisticEntity != null) {
            statisticModel.setDefectChange(statisticModel.getExistCount() - baseStatisticEntity.getDefectCount());
            statisticModel.setDupRateChange(dupRate - baseStatisticEntity.getDupRate());
            statisticModel.setLastDefectCount(baseStatisticEntity.getDefectCount());
            statisticModel.setLastDupRate(baseStatisticEntity.getDupRate());
        } else {
            statisticModel.setDefectChange(statisticModel.getExistCount());
            statisticModel.setDupRateChange(dupRate);
            statisticModel.setLastDefectCount(0);
            statisticModel.setLastDupRate(0.0F);
        }
    }

    /**
     * 统计所有"待修复"告警的规则信息
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChecker(DupcDefectStatisticModel statisticModel) {

    }

    /**
     * 统计告警图表数据,DUPC CCN
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChart(DupcDefectStatisticModel statisticModel) {
        // 获取最近5天重复率趋势
        List<DupcChartTrendEntity> dupcChart = Lists.newArrayList();
        IDataReportBizService dataReportBizService = dataReportBizServiceBizServiceFactory.createBizService(
                ComConstants.Tool.DUPC.name(),
                ComConstants.BusinessType.DATA_REPORT.value(), IDataReportBizService.class);
        DupcDataReportRspVO dupcDataReportRspVO = (DupcDataReportRspVO) dataReportBizService.getDataReport(
                statisticModel.getTaskId(), ComConstants.Tool.DUPC.name(), 5, null, null);
        if (dupcDataReportRspVO != null) {
            //按日期排序
            dupcDataReportRspVO.getChartTrendList()
                    .getDucpChartList()
                    .sort(Comparator.comparing(DupcChartTrendVO::getDate));
            //重复率值保留两位小数
            dupcDataReportRspVO.getChartTrendList()
                    .getDucpChartList()
                    .forEach(dupcChartTrendVO -> {
                        BigDecimal averageDupc = BigDecimal.valueOf(dupcChartTrendVO.getDupc());
                        dupcChartTrendVO.setDupc(averageDupc.setScale(2, RoundingMode.HALF_DOWN).floatValue());
            });
            //本次扫描是今天的最新数值
            String currentDate = LocalDate.now().toString();
            dupcChart.addAll(dupcDataReportRspVO.getChartTrendList()
                    .getDucpChartList()
                    .stream()
                    .map(dupcChartTrendVO -> {
                        DupcChartTrendEntity dupcChartTrendEntity = new DupcChartTrendEntity();
                        if (dupcChartTrendVO.getDate().equals(currentDate)) {
                            dupcChartTrendVO.setDupc((float) (Math.round(statisticModel.getDupRate() * 100)) / 100);
                        }
                        BeanUtils.copyProperties(dupcChartTrendVO, dupcChartTrendEntity);
                        return dupcChartTrendEntity;
                    }).collect(Collectors.toList())
            );
        }
        statisticModel.setDupcChart(dupcChart);
    }

    /**
     * 将 statisticModel 转化为 statisticEntity，保存到对应表
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void buildAndSaveStatisticResult(DupcDefectStatisticModel statisticModel) {
        DUPCStatisticEntity dupcStatisticEntity = statisticModel.getBuilder().convert();
        statisticModel.setDupcStatisticEntity(dupcStatisticEntity);
        dupcStatisticRepository.save(dupcStatisticEntity);
    }

    /**
     * 异步统计 "已修复"、"已屏蔽"、"已忽略" 状态的告警
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void asyncStatisticDefect(DupcDefectStatisticModel statisticModel) {

    }

    /**
     * 将统计数据推送到数据平台
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void pushDataKafka(DupcDefectStatisticModel statisticModel) {
        //将数据加入数据平台
        commonKafkaClient.pushDUPCStatisticToKafka(statisticModel.getDupcStatisticEntity());
    }

    /**
     * 根据重复率获取风险等级枚举值
     *
     * @param dupcRate 重复率数值
     * @param sh 超高风险重复率界定值
     * @param h 高风险重复率界定值
     * @param m 中等风险重复率界定值
     */
    private int getRiskFactorVal(float dupcRate, float sh, float h, float m) {
        if (dupcRate >= sh) {
            return ComConstants.RiskFactor.SH.value();
        } else if (dupcRate < sh && dupcRate >= h) {
            return ComConstants.RiskFactor.H.value();
        } else if (dupcRate < h && dupcRate >= m) {
            return ComConstants.RiskFactor.M.value();
        } else {
            return ComConstants.RiskFactor.L.value();
        }
    }

    /**
     * 计算每个告警处理人的个严重级别告警数量
     *
     * @param statisticModel 统计数据记录实体
     * @param authorEntity 告警处理人统计信息记录实体
     */
    private void statisticAuthorDefectCount(
            DupcDefectStatisticModel statisticModel, DUPCNotRepairedAuthorEntity authorEntity) {
        if (ComConstants.RiskFactor.SH.value() == statisticModel.getRiskValue()) {
            authorEntity.setSuperHighCount(authorEntity.getSuperHighCount() + 1);
        } else if (ComConstants.RiskFactor.H.value() == statisticModel.getRiskValue()) {
            authorEntity.setHighCount(authorEntity.getHighCount() + 1);
        } else if (ComConstants.RiskFactor.M.value() == statisticModel.getRiskValue()) {
            authorEntity.setMediumCount(authorEntity.getMediumCount() + 1);
        }
    }
}
