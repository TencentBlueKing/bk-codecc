package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.statistic.DimensionStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;

import java.util.List;
import org.springframework.beans.BeanUtils;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的lint类model类
 *
 * @author warmli
 */
public class LintDefectStatisticModelBuilder
        extends AbstractDefectStatisticModelBuilder<LintDefectStatisticModel, LintStatisticEntity, LintDefectV2Entity> {

    {
        this.defectStatisticModel = new LintDefectStatisticModel();
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    @Override
    public LintDefectStatisticModel build() {
        defectStatisticModel.setBuilder(this);
        return defectStatisticModel;
    }

    /**
     * 根据现有数据，初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    @Override
    public LintStatisticEntity convert(LintStatisticEntity lintStatisticEntity) {
        if (lintStatisticEntity == null) {
            lintStatisticEntity = new LintStatisticEntity();
        }
        lintStatisticEntity.setTaskId(defectStatisticModel.getTaskId());
        lintStatisticEntity.setBuildId(defectStatisticModel.getBuildId());
        lintStatisticEntity.setTime(System.currentTimeMillis());
        lintStatisticEntity.setToolName(defectStatisticModel.getToolName());
        lintStatisticEntity.setDefectCount(defectStatisticModel.getAllNewDefects().size());
        lintStatisticEntity.setDefectChange(defectStatisticModel.getDefectChange());
        lintStatisticEntity.setFileCount(defectStatisticModel.getFilePathSet().size());
        lintStatisticEntity.setFileChange(defectStatisticModel.getFileChange());
        lintStatisticEntity.setNewDefectCount(defectStatisticModel.getTotalNewSerious()
                + defectStatisticModel.getTotalNewNormal()
                + defectStatisticModel.getTotalNewPrompt());
        lintStatisticEntity.setHistoryDefectCount(defectStatisticModel.getTotalOldSerious()
                + defectStatisticModel.getTotalOldNormal()
                + defectStatisticModel.getTotalOldPrompt());
        lintStatisticEntity.setTotalNewNormal(defectStatisticModel.getTotalNewNormal());
        lintStatisticEntity.setTotalNewPrompt(defectStatisticModel.getTotalNewPrompt());
        lintStatisticEntity.setTotalNewSerious(defectStatisticModel.getTotalNewSerious());
        lintStatisticEntity.setTotalNormal(
                defectStatisticModel.getTotalNewNormal() + defectStatisticModel.getTotalOldNormal());
        lintStatisticEntity.setTotalPrompt(
                defectStatisticModel.getTotalNewPrompt() + defectStatisticModel.getTotalOldPrompt());
        lintStatisticEntity.setTotalSerious(
                defectStatisticModel.getTotalNewSerious() + defectStatisticModel.getTotalOldSerious());
        lintStatisticEntity.setTotalDefectCount((long) defectStatisticModel.getAllDefects().size());
        lintStatisticEntity.setAuthorStatistic(Lists.newArrayList(defectStatisticModel.getAuthorDefectMap().values()));
        lintStatisticEntity.setCheckerStatistic(defectStatisticModel.getCheckerStatisticList());
        lintStatisticEntity.setExistAuthorStatistic(
                Lists.newArrayList(defectStatisticModel.getExistAuthorMap().values()));
        // 维度相关统计
        if (defectStatisticModel.getDimensionStatisticModel() != null) {
            DimensionStatisticEntity dimensionStatisticEntity = new DimensionStatisticEntity();
            BeanUtils.copyProperties(defectStatisticModel.getDimensionStatisticModel(), dimensionStatisticEntity);
            lintStatisticEntity.setDimensionStatistic(dimensionStatisticEntity);
        }
        return lintStatisticEntity;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    @Override
    public AbstractDefectStatisticModelBuilder<LintDefectStatisticModel, LintStatisticEntity,
            LintDefectV2Entity> allDefectList(List<LintDefectV2Entity> defectList) {
        defectStatisticModel.setAllDefects(defectList);
        return this;
    }
}
