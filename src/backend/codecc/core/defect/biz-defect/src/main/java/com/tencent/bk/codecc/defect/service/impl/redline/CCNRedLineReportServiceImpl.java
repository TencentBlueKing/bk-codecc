package com.tencent.bk.codecc.defect.service.impl.redline;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT_F;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT_D;

import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractRedLineReportService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLCcnAndDupcDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.util.DateTimeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CCNRedLineReportServiceImpl extends AbstractRedLineReportService<CCNDefectEntity> {

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    /**
     * 查询圈复杂度和重复率分析结果
     *
     * @param taskDetailVO         任务信息
     * @param toolInfo         工具的基本信息[t_tool_meta]
     * @param metadataModel    元数据
     * @param metadataCallback 发送到蓝盾的元数据
     * @param effectiveTools   有效的工具
     * @param toolConfig       工具配置
     */
    @Override
    public void getAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo, ToolConfigInfoVO toolConfig,
            Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
            List<String> effectiveTools, String buildId, List<CCNDefectEntity> newDefectList,
                                  RedLineExtraParams<CCNDefectEntity> extraParams) {
        log.info("start to get ccn analysis result for task: {} {} {}",
                taskDetailVO.getTaskId(), toolInfo.getName(), buildId);
        long codeLine = getCodeLine(taskDetailVO, toolConfig.getCurrentBuildId());
        long taskId = taskDetailVO.getTaskId();
        String toolName = toolInfo.getName();
        RLCcnAndDupcDefectVO ccnDupcDefect = new RLCcnAndDupcDefectVO()
                .average(PASS_COUNT_D)
                .singleFuncMax(PASS_COUNT)
                .extreme(PASS_COUNT)
                .high(PASS_COUNT)
                .middle(PASS_COUNT)
                .newSingleFuncMax(PASS_COUNT)
                .newFuncCount(PASS_COUNT)
                .newFuncBeyondThresholdSum(PASS_COUNT)
                .historyFuncBeyondThresholdSum(PASS_COUNT)
                .c30(PASS_COUNT)
                .c25(PASS_COUNT)
                .c20(PASS_COUNT)
                .c15(PASS_COUNT)
                .c10(PASS_COUNT)
                .c5(PASS_COUNT);
        boolean toolMatchCodeLang = (toolInfo.getLang() & taskDetailVO.getCodeLang()) != 0;

        if (toolMatchCodeLang) {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName)) {
                log.info("ccn red line for task is not match tool: {} {} {}",
                        taskDetailVO.getTaskId(), toolName, buildId);
                ccnDupcDefect.setAverage(FORBIDDEN_COUNT_F);
                ccnDupcDefect.setExtreme(FORBIDDEN_COUNT);
                ccnDupcDefect.setHigh(FORBIDDEN_COUNT);
                ccnDupcDefect.setMiddle(FORBIDDEN_COUNT);
                ccnDupcDefect.setSingleFuncMax(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewSingleFuncMax(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewFuncCount(FORBIDDEN_COUNT);
                ccnDupcDefect.setNewFuncBeyondThresholdSum(FORBIDDEN_COUNT);
                ccnDupcDefect.setHistoryFuncBeyondThresholdSum(FORBIDDEN_COUNT);
                ccnDupcDefect.setC30(FORBIDDEN_COUNT);
                ccnDupcDefect.setC25(FORBIDDEN_COUNT);
                ccnDupcDefect.setC20(FORBIDDEN_COUNT);
                ccnDupcDefect.setC15(FORBIDDEN_COUNT);
                ccnDupcDefect.setC10(FORBIDDEN_COUNT);
                ccnDupcDefect.setC5(FORBIDDEN_COUNT);
            } else {
                // TODO 由statistic计算存入redis，再从redis取出来使用
                // 查询圈复杂度的告警数量
                List<CCNDefectEntity> ignoreDefectList = extraParams == null
                        || CollectionUtils.isEmpty(extraParams.getIgnoreDefectList()) ? Collections.emptyList()
                        : extraParams.getIgnoreDefectList();
                getCcnDefectCount(ccnDupcDefect, taskId, toolConfig, newDefectList, ignoreDefectList);
                // 查询圈复杂度的统计数据
                getCcnAndDupcStatisticResult(taskId, toolName, ccnDupcDefect, ignoreDefectList);
            }
        }

        batchUpdateValue(ccnDupcDefect, metadataModel, metadataCallback, codeLine);
    }

    /**
     * 查询cloc代码行数
     *
     * @param taskDetailVO         任务信息
     */
    private long getCodeLine(TaskDetailVO taskDetailVO, String buildId) {
        long taskId = taskDetailVO.getTaskId();
        List<CLOCStatisticEntity> clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(
                taskId, ComConstants.Tool.SCC.name(), buildId);
        if (CollectionUtils.isEmpty(clocStatisticEntities)) {
            clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(
                    taskId, ComConstants.Tool.CLOC.name(), buildId);
        }

        if (CollectionUtils.isEmpty(clocStatisticEntities)) {
            return 0;
        }

        Optional<Long> codeLine = clocStatisticEntities.stream()
                .map(CLOCStatisticEntity::getSumCode)
                .reduce(Long::sum);
        return codeLine.orElse(0L);
    }

    private String getKlocValue(long cx, long codeLine) {
        if (codeLine == 0) {
            return "0";
        }
        if (cx == -1) {
            return "-1";
        }
        double ccnCount = (cx * 1000) + 0.0;
        return String.format("%.2f", ccnCount / codeLine);
    }

    private void getCcnDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect, long taskId, ToolConfigInfoVO toolConfig,
            List<CCNDefectEntity> newDefectList, List<CCNDefectEntity> ignoreDefectList) {
        // 获取超标圈复杂度阈值，优先从规则里面取，取不到从个性化参数里面取，再取不到就是用默认值
        int ccnThreshold = checkerService.getCcnThreshold(toolConfig);

        long maxCcn = 0L;
        long newMaxCcn = 0L;
        int newDefectCount = 0;
        int newFuncBeyondThresholdSum = 0;
        int historyFuncBeyondThresholdSum = 0;
        long c30 = -1;
        long c25 = -1;
        long c20 = -1;
        long c15 = -1;
        long c10 = -1;
        long c5 = -1;
        if (ccnThreshold <= 30) {
            c30 = 0;
        }
        if (ccnThreshold <= 25) {
            c25 = 0;
        }
        if (ccnThreshold <= 20) {
            c20 = 0;
        }
        if (ccnThreshold <= 15) {
            c15 = 0;
        }
        if (ccnThreshold <= 10) {
            c10 = 0;
        }
        if (ccnThreshold <= 5) {
            c5 = 0;
        }

        ccnDupcDefect.setSingleFuncMax(maxCcn);
        ccnDupcDefect.setNewFuncCount((long) newDefectCount);
        ccnDupcDefect.setNewSingleFuncMax(newMaxCcn);
        ccnDupcDefect.setNewFuncBeyondThresholdSum((long) newFuncBeyondThresholdSum);
        ccnDupcDefect.setHistoryFuncBeyondThresholdSum((long) historyFuncBeyondThresholdSum);
        ccnDupcDefect.setC30(c30);
        ccnDupcDefect.setC25(c25);
        ccnDupcDefect.setC20(c20);
        ccnDupcDefect.setC15(c15);
        ccnDupcDefect.setC10(c10);
        ccnDupcDefect.setC5(c5);

        getNewCCNDefectCount(ccnDupcDefect, taskId, newDefectList, ccnThreshold);

        getHistoryCCNDefectCount(ccnDupcDefect, taskId, ignoreDefectList, ccnThreshold);
    }

    private void getNewCCNDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect, long taskId,
                                      List<CCNDefectEntity> ccnDefectList, int ccnThreshold) {
        long maxCcn = ccnDupcDefect.getSingleFuncMax();
        long newMaxCcn = ccnDupcDefect.getNewSingleFuncMax();
        int newDefectCount = ccnDupcDefect.getNewFuncCount().intValue();
        int newFuncBeyondThresholdSum = ccnDupcDefect.getNewFuncBeyondThresholdSum().intValue();
        long c30 = ccnDupcDefect.getC30();
        long c25 = ccnDupcDefect.getC25();
        long c20 = ccnDupcDefect.getC20();
        long c15 = ccnDupcDefect.getC15();
        long c10 = ccnDupcDefect.getC10();
        long c5 = ccnDupcDefect.getC5();
        if (CollectionUtils.isNotEmpty(ccnDefectList)) {
            log.info("start to get new ccn defect: {}, {}, {}", taskId, ccnDefectList.size(), ccnDefectList.get(0));
            for (CCNDefectEntity defectModel : ccnDefectList) {
                // 获取圈复杂度和代码修改时间
                long ccn = defectModel.getCcn();
                // 获取新增代码圈复杂度最大值
                if (ccn > ccnDupcDefect.getSingleFuncMax()) {
                    ccnDupcDefect.setSingleFuncMax(ccn);
                }
                newDefectCount++;

                // 统计新函数超过阈值部分的圈复杂度之和
                newFuncBeyondThresholdSum += ccn - ccnThreshold;


                // 获取圈复杂度最大值
                if (ccn > maxCcn) {
                    maxCcn = ccn;
                }

                // 获取新圈复杂度最大值
                if (ccn > newMaxCcn) {
                    newMaxCcn = ccn;
                }

                // 求CXX指标
                if (ccnThreshold <= 30 && ccn - 30 > 0) {
                    c30 += ccn - 30;
                }
                if (ccnThreshold <= 25 && ccn - 25 > 0) {
                    c25 += ccn - 25;
                }
                if (ccnThreshold <= 20 && ccn - 20 > 0) {
                    c20 += ccn - 20;
                }
                if (ccnThreshold <= 15 && ccn - 15 > 0) {
                    c15 += ccn - 15;
                }
                if (ccnThreshold <= 10 && ccn - 10 > 0) {
                    c10 += ccn - 10;
                }
                if (ccnThreshold <= 5 && ccn - 5 > 0) {
                    c5 += ccn - 5;
                }
            }
            ccnDupcDefect.setSingleFuncMax(maxCcn);
            ccnDupcDefect.setNewFuncCount((long) newDefectCount);
            ccnDupcDefect.setNewSingleFuncMax(newMaxCcn);
            ccnDupcDefect.setNewFuncBeyondThresholdSum((long) newFuncBeyondThresholdSum);
            ccnDupcDefect.setC30(c30);
            ccnDupcDefect.setC25(c25);
            ccnDupcDefect.setC20(c20);
            ccnDupcDefect.setC15(c15);
            ccnDupcDefect.setC10(c10);
            ccnDupcDefect.setC5(c5);
        } else {
            log.info("start to get new ccn defect is empty: {}", taskId);
        }
    }

    private void getHistoryCCNDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect, long taskId,
                                      List<CCNDefectEntity> ccnDefectList, int ccnThreshold) {
        long maxCcn = ccnDupcDefect.getSingleFuncMax();
        int historyFuncBeyondThresholdSum = ccnDupcDefect.getHistoryFuncBeyondThresholdSum().intValue();
        long c30 = ccnDupcDefect.getC30();
        long c25 = ccnDupcDefect.getC25();
        long c20 = ccnDupcDefect.getC20();
        long c15 = ccnDupcDefect.getC15();
        long c10 = ccnDupcDefect.getC10();
        long c5 = ccnDupcDefect.getC5();
        if (CollectionUtils.isNotEmpty(ccnDefectList)) {
            log.info("start to get history ccn defect: {}, {}, {}", taskId, ccnDefectList.size(), ccnDefectList.get(0));
            for (CCNDefectEntity defectModel : ccnDefectList) {
                // 获取圈复杂度和代码修改时间
                long ccn = defectModel.getCcn();
                // 获取新增代码圈复杂度最大值
                if (ccn > ccnDupcDefect.getSingleFuncMax()) {
                    ccnDupcDefect.setSingleFuncMax(ccn);
                }

                // 统计新函数超过阈值部分的圈复杂度之和
                historyFuncBeyondThresholdSum += ccn - ccnThreshold;


                // 获取圈复杂度最大值
                if (ccn > maxCcn) {
                    maxCcn = ccn;
                }

                // 求CXX指标
                if (ccnThreshold <= 30 && ccn - 30 > 0) {
                    c30 += ccn - 30;
                }
                if (ccnThreshold <= 25 && ccn - 25 > 0) {
                    c25 += ccn - 25;
                }
                if (ccnThreshold <= 20 && ccn - 20 > 0) {
                    c20 += ccn - 20;
                }
                if (ccnThreshold <= 15 && ccn - 15 > 0) {
                    c15 += ccn - 15;
                }
                if (ccnThreshold <= 10 && ccn - 10 > 0) {
                    c10 += ccn - 10;
                }
                if (ccnThreshold <= 5 && ccn - 5 > 0) {
                    c5 += ccn - 5;
                }
            }
            ccnDupcDefect.setSingleFuncMax(maxCcn);
            ccnDupcDefect.setHistoryFuncBeyondThresholdSum((long) historyFuncBeyondThresholdSum);
            ccnDupcDefect.setC30(c30);
            ccnDupcDefect.setC25(c25);
            ccnDupcDefect.setC20(c20);
            ccnDupcDefect.setC15(c15);
            ccnDupcDefect.setC10(c10);
            ccnDupcDefect.setC5(c5);
        } else {
            log.info("start to get history ccn defect is empty: {}", taskId);
        }
    }

    /**
     * 查询圈复杂度和重复率的统计数据
     *
     * @param taskId
     * @param toolName
     * @param ccnDupcDefect
     */
    private void getCcnAndDupcStatisticResult(long taskId, String toolName, RLCcnAndDupcDefectVO ccnDupcDefect,
                                              List<CCNDefectEntity> ignoreDefectList) {
        CCNStatisticEntity ccnStatistic =
                ccnStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
        ccnDupcDefect.setAverage(Double.parseDouble(String.format("%.4f", ccnStatistic.getAverageCCN() == null
                ? 0.0F
                : ccnStatistic.getAverageCCN())));

        int stockExtreme = 0;
        int stockHigh = 0;
        int stockMiddle = 0;
        if (CollectionUtils.isNotEmpty(ignoreDefectList)) {
            // 获取各严重级别定义
            Map<String, String> riskConfigMap =
                    thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
            int sh = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
            int h = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
            int m = Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.M.name()));
            for (CCNDefectEntity ccnDefectEntity : ignoreDefectList) {
                ComConstants.RiskFactor riskFactor = getRiskFactorVal(ccnDefectEntity.getCcn(), sh, h, m);
                switch (riskFactor) {
                    case SH:
                        stockExtreme++;
                        break;
                    case H:
                        stockHigh++;
                        break;
                    case M:
                        stockMiddle++;
                        break;
                    default:
                        break;
                }
            }
        }

        ccnDupcDefect.setExtreme((long) ((ccnStatistic.getSuperHighCount() == null
                ? 0
                : ccnStatistic.getSuperHighCount()) + stockExtreme));
        ccnDupcDefect.setHigh((long) ((ccnStatistic.getHighCount() == null
                ? 0
                : ccnStatistic.getHighCount()) + stockHigh));
        ccnDupcDefect.setMiddle((long) ((ccnStatistic.getMediumCount() == null
                ? 0
                : ccnStatistic.getMediumCount()) + stockMiddle));
    }

    private ComConstants.RiskFactor getRiskFactorVal(int ccn, int sh, int h, int m) {
        if (ccn >= sh) {
            return ComConstants.RiskFactor.SH;
        } else if (ccn < sh && ccn >= h) {
            return ComConstants.RiskFactor.H;
        } else if (ccn < h && ccn >= m) {
            return ComConstants.RiskFactor.M;
        } else {
            return ComConstants.RiskFactor.L;
        }
    }

    private void batchUpdateValue(RLCcnAndDupcDefectVO ccnDupcDefect, Map<String, RedLineVO> metadataModel,
                                  PipelineRedLineCallbackVO metadataCallback, long codeLine) {
        updateValue(ComConstants.Tool.CCN.name() + "_SINGLE_FUNC_MAX",
                String.valueOf(ccnDupcDefect.getSingleFuncMax()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_NEW_SINGLE_FUNC_MAX",
                String.valueOf(ccnDupcDefect.getNewSingleFuncMax()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_NEW_FUNC_COUNT",
                String.valueOf(ccnDupcDefect.getNewFuncCount()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_NEW_FUNC_BEYOND_THRESHOLD_SUM",
                String.valueOf(ccnDupcDefect.getNewFuncBeyondThresholdSum()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_HISTORY_FUNC_BEYOND_THRESHOLD_SUM",
                String.valueOf(ccnDupcDefect.getHistoryFuncBeyondThresholdSum()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C30_KLOC",
                getKlocValue(ccnDupcDefect.getC30(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C25_KLOC",
                getKlocValue(ccnDupcDefect.getC25(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C20_KLOC",
                getKlocValue(ccnDupcDefect.getC20(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C15_KLOC",
                getKlocValue(ccnDupcDefect.getC15(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C10_KLOC",
                getKlocValue(ccnDupcDefect.getC10(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_C5_KLOC",
                getKlocValue(ccnDupcDefect.getC5(), codeLine),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_AVERAGE",
                String.valueOf(ccnDupcDefect.getAverage()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_EXTREME",
                String.valueOf(ccnDupcDefect.getExtreme()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_HIGH",
                String.valueOf(ccnDupcDefect.getHigh()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.CCN.name() + "_MIDDLE",
                String.valueOf(ccnDupcDefect.getMiddle()),
                ComConstants.Tool.CCN.name(),
                metadataModel,
                metadataCallback);
    }
}
