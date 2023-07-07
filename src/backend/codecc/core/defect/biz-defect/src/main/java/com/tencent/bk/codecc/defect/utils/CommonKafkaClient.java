package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.model.MetricsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class CommonKafkaClient {

    public void pushDefectEntityToKafka(List<CommonDefectEntity> commonDefectEntityList) {

    }

    /**
     * 推送编译型工具统计信息到数据平台
     * @param commonStatisticEntity
     */
    public void pushCommonStatisticToKafka(CommonStatisticEntity commonStatisticEntity) {

    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param lintStatisticEntity
     */
    public void pushLintStatisticToKafka(LintStatisticEntity lintStatisticEntity) {

    }


    /**
     * 推送lint类工具统计信息到数据平台
     * @param ccnStatisticEntity
     */
    public void pushCCNStatisticToKafka(CCNStatisticEntity ccnStatisticEntity) {

    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param dupcStatisticEntity
     */
    public void pushDUPCStatisticToKafka(DUPCStatisticEntity dupcStatisticEntity) {

    }


    /**
     * 推送代码行统计信息到数据平台
     * @param clocStatisticEntityList
     */
    public void pushCLOCStatisticToKafka(Collection<CLOCStatisticEntity> clocStatisticEntityList) {

    }

    public void pushMetricsToKafka(MetricsEntity metricsEntity) {

    }
}
