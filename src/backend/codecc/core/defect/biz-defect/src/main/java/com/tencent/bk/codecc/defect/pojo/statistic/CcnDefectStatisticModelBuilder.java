package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;

import java.util.List;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的圈复杂度类model类
 *
 * @author warmli
 */
public class CcnDefectStatisticModelBuilder
        extends AbstractDefectStatisticModelBuilder<CcnDefectStatisticModel, CCNStatisticEntity, CCNDefectEntity> {

    {
        this.defectStatisticModel = new CcnDefectStatisticModel();
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    @Override
    public CcnDefectStatisticModel build() {
        defectStatisticModel.setBuilder(this);
        return defectStatisticModel;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    @Override
    public CCNStatisticEntity convert() {
        CCNStatisticEntity ccnStatisticEntity = new CCNStatisticEntity();
        ccnStatisticEntity.setTaskId(defectStatisticModel.getTaskId());
        ccnStatisticEntity.setToolName(defectStatisticModel.getToolName());
        ccnStatisticEntity.setBuildId(defectStatisticModel.getBuildId());
        ccnStatisticEntity.setTime(System.currentTimeMillis());
        ccnStatisticEntity.setDefectCount(defectStatisticModel.getExistCount());
        ccnStatisticEntity.setDefectChange(defectStatisticModel.getDefectChange());
        ccnStatisticEntity.setAverageCCN(defectStatisticModel.getAverageCcn());
        ccnStatisticEntity.setAverageCCNChange(defectStatisticModel.getAverageCcnChange());
        ccnStatisticEntity.setSuperHighCount(
                defectStatisticModel.getNewSuperHighCount() + defectStatisticModel.getOldSuperHighCount());
        ccnStatisticEntity.setHighCount(
                defectStatisticModel.getNewHighCount() + defectStatisticModel.getOldHighCount());
        ccnStatisticEntity.setMediumCount(
                defectStatisticModel.getNewMediumCount() + defectStatisticModel.getOldMediumCount());
        ccnStatisticEntity.setLowCount(defectStatisticModel.getNewLowCount() + defectStatisticModel.getOldLowCount());
        ccnStatisticEntity.setCcnBeyondThresholdSum(defectStatisticModel.getCcnBeyondThresholdSum());
        ccnStatisticEntity.setNewAuthorStatistic(Lists.newArrayList(defectStatisticModel.getNewAuthorMap().values()));
        ccnStatisticEntity.setExistAuthorStatistic(
                Lists.newArrayList(defectStatisticModel.getExistAuthorMap().values()));
        ccnStatisticEntity.setNewSuperHighCount(defectStatisticModel.getNewSuperHighCount());
        ccnStatisticEntity.setNewHighCount(defectStatisticModel.getNewHighCount());
        ccnStatisticEntity.setNewMediumCount(defectStatisticModel.getNewMediumCount());
        ccnStatisticEntity.setNewLowCount(defectStatisticModel.getNewLowCount());
        ccnStatisticEntity.setAverageList(defectStatisticModel.getAverageList());
        ccnStatisticEntity.setLastAverageCCN(defectStatisticModel.getLastAverageCcn());
        ccnStatisticEntity.setLastDefectCount(defectStatisticModel.getLastDefectCount());
        return ccnStatisticEntity;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    @Override
    public AbstractDefectStatisticModelBuilder<CcnDefectStatisticModel, CCNStatisticEntity,
            CCNDefectEntity> allDefectList(List<CCNDefectEntity> defectList) {
        defectStatisticModel.setAllDefects(defectList);
        return this;
    }

    public CcnDefectStatisticModelBuilder sh(int sh) {
        defectStatisticModel.setSh(sh);
        return this;
    }

    public CcnDefectStatisticModelBuilder h(int h) {
        defectStatisticModel.setH(h);
        return this;
    }

    public CcnDefectStatisticModelBuilder m(int m) {
        defectStatisticModel.setM(m);
        return this;
    }

    public CcnDefectStatisticModelBuilder ccnThreshold(int ccnThreshold) {
        defectStatisticModel.setCcnThreshold(ccnThreshold);
        return this;
    }

    public CcnDefectStatisticModelBuilder averageCcn(float averageCcn) {
        defectStatisticModel.setAverageCcn(averageCcn);
        return this;
    }
}
