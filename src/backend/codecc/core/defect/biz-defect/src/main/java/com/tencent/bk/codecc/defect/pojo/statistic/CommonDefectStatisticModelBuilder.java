package com.tencent.bk.codecc.defect.pojo.statistic;

import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.DimensionStatisticEntity;
import java.util.List;
import org.springframework.beans.BeanUtils;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的编译类model类
 *
 * @author warmli
 */
public class CommonDefectStatisticModelBuilder extends AbstractDefectStatisticModelBuilder<
        CommonDefectStatisticModel, CommonStatisticEntity, CommonDefectEntity> {

    {
        this.defectStatisticModel = new CommonDefectStatisticModel();
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    @Override
    public CommonDefectStatisticModel build() {
        defectStatisticModel.setBuilder(this);
        return defectStatisticModel;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    @Override
    public CommonStatisticEntity convert() {
        CommonStatisticEntity commonStatisticEntity = new CommonStatisticEntity();
        commonStatisticEntity.setTaskId(defectStatisticModel.getTaskId());
        commonStatisticEntity.setToolName(defectStatisticModel.getToolName());
        commonStatisticEntity.setTime(System.currentTimeMillis());
        commonStatisticEntity.setBuildId(defectStatisticModel.getBuildId());
        commonStatisticEntity.setNewCount(0);
        commonStatisticEntity.setFixedCount(0);
        commonStatisticEntity.setExistCount(defectStatisticModel.getAllNewDefects().size());
        commonStatisticEntity.setCloseCount(0);
        commonStatisticEntity.setExcludeCount(0);
        commonStatisticEntity.setExistPromptCount(defectStatisticModel.getTotalNewPrompt()
                + defectStatisticModel.getTotalOldPrompt());
        commonStatisticEntity.setExistNormalCount(defectStatisticModel.getTotalNewNormal()
                + defectStatisticModel.getTotalOldNormal());
        commonStatisticEntity.setExistSeriousCount(defectStatisticModel.getTotalNewSerious()
                + defectStatisticModel.getTotalOldSerious());
        commonStatisticEntity.setNewPromptCount(defectStatisticModel.getTotalNewPrompt());
        commonStatisticEntity.setNewNormalCount(defectStatisticModel.getTotalNewNormal());
        commonStatisticEntity.setNewSeriousCount(defectStatisticModel.getTotalNewSerious());
        commonStatisticEntity.setNewAuthors(defectStatisticModel.getNewAuthors());
        commonStatisticEntity.setExistAuthors(defectStatisticModel.getOldAuthors());
        commonStatisticEntity.setPromptAuthors(defectStatisticModel.getNewPromptAuthors());
        commonStatisticEntity.setNormalAuthors(defectStatisticModel.getNewNormalAuthors());
        commonStatisticEntity.setSeriousAuthors(defectStatisticModel.getNewSeriousAuthors());
        commonStatisticEntity.setExistPromptAuthors(defectStatisticModel.getOldPromptAuthors());
        commonStatisticEntity.setExistNormalAuthors(defectStatisticModel.getOldNormalAuthors());
        commonStatisticEntity.setExistSeriousAuthors(defectStatisticModel.getOldSeriousAuthors());

        // 维度相关统计
        if (defectStatisticModel.getDimensionStatisticModel() != null) {
            DimensionStatisticEntity dimensionStatisticEntity = new DimensionStatisticEntity();
            BeanUtils.copyProperties(defectStatisticModel.getDimensionStatisticModel(), dimensionStatisticEntity);
            commonStatisticEntity.setDimensionStatistic(dimensionStatisticEntity);
        }

        return commonStatisticEntity;
    }

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    @Override
    public AbstractDefectStatisticModelBuilder<CommonDefectStatisticModel, CommonStatisticEntity,
            CommonDefectEntity> allDefectList(List<CommonDefectEntity> defectList) {
        defectStatisticModel.setAllDefects(defectList);
        return this;
    }
}
