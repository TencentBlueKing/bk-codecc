package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.DUPCDefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.DUPCNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.DUPCScanSummaryEntity;
import com.tencent.bk.codecc.defect.model.DupcChartTrendEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用于保存重复率工具在统计流程中需要的初始化指标和计算出的中间结果和最终结果
 * 计算完成后通过 builder convert 为表实体类保存
 *
 * @author warmli
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DupcDefectStatisticModel extends AbstractDefectStatisticModel {

    protected DupcDefectStatisticModel() {
    }

    private float sh;

    private float h;

    private float m;

    private float dupRate;

    private int riskValue;

    private int existCount;

    private int newSuperHighCount;

    private int newHighCount;

    private int newMediumCount;

    private int oldSuperHighCount;

    private int oldHighCount;

    private int oldMediumCount;

    private int defectChange;

    private float dupRateChange;

    private int lastDefectCount;

    private float lastDupRate;

    private DUPCStatisticEntity dupcStatisticEntity;

    private DUPCScanSummaryEntity dupcScanSummary;

    private DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity;

    private Set<String> authorSet;

    private List<DUPCDefectEntity> allDefects;

    private Map<String, DUPCNotRepairedAuthorEntity> newAuthorMap = Maps.newHashMap();

    private Map<String, DUPCNotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();

    private List<DupcChartTrendEntity> dupcChart;

    private DupcDefectStatisticModelBuilder builder;

    public void incExistCount() {
        existCount++;
    }

    public void incNewSuperHighCount() {
        newSuperHighCount++;
    }

    public void incNewHighCount() {
        newHighCount++;
    }

    public void incNewMediumCount() {
        newMediumCount++;
    }

    public void incOldSuperHighCount() {
        oldSuperHighCount++;
    }

    public void incOldHighCount() {
        oldHighCount++;
    }

    public void incOldMediumCount() {
        oldMediumCount++;
    }
}
