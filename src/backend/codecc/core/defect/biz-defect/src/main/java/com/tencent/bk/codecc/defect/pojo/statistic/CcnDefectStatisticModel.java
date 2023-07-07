package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.CCNNotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.ChartAverageEntity;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 用于保存圈复杂度工具在统计流程中需要的初始化指标和计算出的中间结果和最终结果
 * 计算完成后通过 builder convert 为表实体类保存
 *
 * @author warmli
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CcnDefectStatisticModel extends AbstractDefectStatisticModel {

    protected CcnDefectStatisticModel() {
    }

    private int sh;

    private int h;

    private int m;

    private int riskValue;

    private int existCount;

    private int oldSuperHighCount;

    private int oldHighCount;

    private int oldMediumCount;

    private int oldLowCount;

    private int newSuperHighCount;

    private int newHighCount;

    private int newMediumCount;

    private int newLowCount;

    private int ccnThreshold;

    private int ccnBeyondThresholdSum;

    private int lastDefectCount;

    private int defectChange;

    private float averageCcn;

    private float lastAverageCcn;

    private float averageCcnChange;

    private CCNStatisticEntity ccnStatisticEntity;

    private List<CCNDefectEntity> allDefects;

    private Map<String, CCNNotRepairedAuthorEntity> newAuthorMap = Maps.newHashMap();

    private Map<String, CCNNotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();

    private List<ChartAverageEntity> averageList;

    private CcnDefectStatisticModelBuilder builder;

    public void incExistCount() {
        existCount++;
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

    public void incOldLowCount() {
        oldLowCount++;
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

    public void incNewLowCount() {
        newLowCount++;
    }
}
