package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.DUPCDefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.devops.common.constant.ComConstants;

import java.util.List;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的重复率类model类
 *
 * @author warmli
 */
public class DupcDefectStatisticModelBuilder
        extends AbstractDefectStatisticModelBuilder<DupcDefectStatisticModel, DUPCStatisticEntity, DUPCDefectEntity> {

    {
        defectStatisticModel = new DupcDefectStatisticModel();
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    @Override
    public DupcDefectStatisticModel build() {
        defectStatisticModel.setBuilder(this);
        return defectStatisticModel;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    @Override
    public DUPCStatisticEntity convert() {
        DUPCStatisticEntity dupcStatisticEntity = new DUPCStatisticEntity();
        dupcStatisticEntity.setTaskId(defectStatisticModel.getTaskId());
        dupcStatisticEntity.setToolName(ComConstants.Tool.DUPC.name());
        dupcStatisticEntity.setBuildId(defectStatisticModel.getBuildId());
        dupcStatisticEntity.setTime(System.currentTimeMillis());
        dupcStatisticEntity.setDefectCount(defectStatisticModel.getExistCount());
        dupcStatisticEntity.setDefectChange(defectStatisticModel.getDefectChange());
        dupcStatisticEntity.setLastDefectCount(defectStatisticModel.getLastDefectCount());
        dupcStatisticEntity.setDupRate(defectStatisticModel.getDupRate());
        dupcStatisticEntity.setDupRateChange(defectStatisticModel.getDupRateChange());
        dupcStatisticEntity.setLastDupRate(defectStatisticModel.getLastDupRate());
        dupcStatisticEntity.setSuperHighCount(defectStatisticModel.getNewSuperHighCount()
                + defectStatisticModel.getOldSuperHighCount());
        dupcStatisticEntity.setHighCount(defectStatisticModel.getNewHighCount()
                + defectStatisticModel.getOldHighCount());
        dupcStatisticEntity.setMediumCount(defectStatisticModel.getNewMediumCount()
                + defectStatisticModel.getOldMediumCount());
        dupcStatisticEntity.setNewSuperHighCount(defectStatisticModel.getNewSuperHighCount());
        dupcStatisticEntity.setNewHighCount(defectStatisticModel.getNewHighCount());
        dupcStatisticEntity.setNewMediumCount(defectStatisticModel.getNewMediumCount());
        dupcStatisticEntity.setNewAuthorStatistic(Lists.newArrayList(defectStatisticModel.getNewAuthorMap().values()));
        dupcStatisticEntity.setExistAuthorStatistic(
                Lists.newArrayList(defectStatisticModel.getExistAuthorMap().values()));
        dupcStatisticEntity.setDupcChart(defectStatisticModel.getDupcChart());
        dupcStatisticEntity.setDupcScanSummary(defectStatisticModel.getDupcScanSummary());
        return dupcStatisticEntity;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    @Override
    public AbstractDefectStatisticModelBuilder<DupcDefectStatisticModel, DUPCStatisticEntity,
            DUPCDefectEntity> allDefectList(List<DUPCDefectEntity> defectList) {
        defectStatisticModel.setAllDefects(defectList);
        return this;
    }

    public DupcDefectStatisticModelBuilder sh(float sh) {
        defectStatisticModel.setSh(sh);
        return this;
    }

    public DupcDefectStatisticModelBuilder h(float h) {
        defectStatisticModel.setH(h);
        return this;
    }

    public DupcDefectStatisticModelBuilder m(float m) {
        defectStatisticModel.setM(m);
        return this;
    }

    public DupcDefectStatisticModelBuilder defectJsonFileEntity(
            DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity) {
        defectStatisticModel.setDefectJsonFileEntity(defectJsonFileEntity);
        return this;
    }
}
