package com.tencent.bk.codecc.defect.service.impl.redline;

import static com.tencent.bk.codecc.defect.constant.DefectConstants.FORBIDDEN_COUNT_F;
import static com.tencent.bk.codecc.defect.constant.DefectConstants.PASS_COUNT_D;

import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractRedLineReportService;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RLCcnAndDupcDefectVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author warmli
 */
@Slf4j
@Service
public class DupcRedLineReportServiceImpl extends AbstractRedLineReportService<DUPCDefectEntity> {
    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Override
    public void getAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo, ToolConfigInfoVO toolConfig,
                                  Map<String, RedLineVO> metadataModel, PipelineRedLineCallbackVO metadataCallback,
                                  List<String> effectiveTools, String buildId, List<DUPCDefectEntity> newDefectList,
                                  RedLineExtraParams<DUPCDefectEntity> extraParams) {
        log.info("start to get dupc analysis result for task: {} {} {}",
                taskDetailVO.getTaskId(), toolInfo.getName(), buildId);
        long taskId = taskDetailVO.getTaskId();
        String toolName = toolInfo.getName();
        RLCcnAndDupcDefectVO ccnDupcDefect = new RLCcnAndDupcDefectVO()
                .singleFileMax(PASS_COUNT_D);

        if ((toolInfo.getLang() & taskDetailVO.getCodeLang()) != 0) {
            // 语言符合但未接入工具时，数值全部为-1
            if (!effectiveTools.contains(toolName)) {
                log.info("dupc red line for task is not match tool: {} {} {}",
                        taskDetailVO.getTaskId(), toolName, buildId);
                ccnDupcDefect.setSingleFileMax(FORBIDDEN_COUNT_F);
            } else {
                // 查询重复率的告警数量
                getDupcDefectCount(ccnDupcDefect, taskId, newDefectList);

                // 查询圈复杂度和重复率的统计数据
                getCcnAndDupcStatisticResult(taskId, buildId, ccnDupcDefect);
            }
        }

        batchUpdateValue(ccnDupcDefect, metadataModel, metadataCallback);
    }

    /**
     * 查询圈复杂度和重复率的统计数据
     *
     * @param taskId
     * @param buildId
     * @param ccnDupcDefect
     */
    private void getCcnAndDupcStatisticResult(long taskId, String buildId, RLCcnAndDupcDefectVO ccnDupcDefect) {
        DUPCStatisticEntity dupcStatistic =
                dupcStatisticRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
        float dupRate = dupcStatistic.getDupRate() == null ? 0.0F : dupcStatistic.getDupRate();
        ccnDupcDefect.setAverage(Double.parseDouble(String.format("%.4f", dupRate / 100)));
        ccnDupcDefect.setExtreme(
                (long) (dupcStatistic.getSuperHighCount() == null ? 0 : dupcStatistic.getSuperHighCount()));
        ccnDupcDefect.setHigh((long) (dupcStatistic.getHighCount() == null ? 0 : dupcStatistic.getHighCount()));
        ccnDupcDefect.setMiddle((long) (dupcStatistic.getMediumCount() == null ? 0 : dupcStatistic.getMediumCount()));
    }

    /**
     * 查询圈复杂度和重复率的告警数量
     *
     * @param ccnDupcDefect
     * @param taskId
     */
    private void getDupcDefectCount(RLCcnAndDupcDefectVO ccnDupcDefect,
            long taskId,
            List<DUPCDefectEntity> newDefectList
    ) {
        List<DUPCDefectEntity> dupcDefects = newDefectList;
                //dupcDefectRepository.getByTaskIdAndStatus(taskId, ComConstants.DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(dupcDefects)) {
            double maxDupc = 0.0D;
            for (DUPCDefectEntity fileInfoModel : dupcDefects) {
                double dupc;
                if (fileInfoModel.getDupRate().contains("%")) {
                    dupc = Double.parseDouble(fileInfoModel.getDupRate().replace("%", "")) / 100;
                } else {
                    dupc = 0.0D;
                }

                if (dupc > maxDupc) {
                    maxDupc = dupc;
                }
            }
            ccnDupcDefect.setSingleFileMax(Double.parseDouble(String.format("%.4f", maxDupc)));
        }
    }

    private void batchUpdateValue(RLCcnAndDupcDefectVO ccnDupcDefect, Map<String, RedLineVO> metadataModel,
                                  PipelineRedLineCallbackVO metadataCallback) {
        updateValue(ComConstants.Tool.DUPC.name() + "_SINGLE_FILE_MAX",
                String.valueOf(ccnDupcDefect.getSingleFileMax()),
                ComConstants.Tool.DUPC.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.DUPC.name() + "_AVERAGE",
                String.valueOf(ccnDupcDefect.getAverage()),
                ComConstants.Tool.DUPC.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.DUPC.name() + "_EXTREME",
                String.valueOf(ccnDupcDefect.getExtreme()),
                ComConstants.Tool.DUPC.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.DUPC.name() + "_HIGH",
                String.valueOf(ccnDupcDefect.getHigh()),
                ComConstants.Tool.DUPC.name(),
                metadataModel,
                metadataCallback);
        updateValue(ComConstants.Tool.DUPC.name() + "_MIDDLE",
                String.valueOf(ccnDupcDefect.getMiddle()),
                ComConstants.Tool.DUPC.name(),
                metadataModel,
                metadataCallback);
    }
}
