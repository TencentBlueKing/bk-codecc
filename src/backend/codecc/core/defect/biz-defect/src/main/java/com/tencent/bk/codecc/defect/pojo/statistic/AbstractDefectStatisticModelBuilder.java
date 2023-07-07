package com.tencent.bk.codecc.defect.pojo.statistic;

import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.StatisticEntity;
import java.util.List;
import java.util.Map;

/**
 * 告警统计模版类的抽象构造器
 * 用于构建告警统计所用的model类
 *
 * @author warmli
 */
public abstract class AbstractDefectStatisticModelBuilder<T extends AbstractDefectStatisticModel,
        S extends StatisticEntity, D extends DefectEntity> {

    protected T defectStatisticModel;

    /**
     * 初始化统计数据记录实体类
     *
     * @return 统计数据记录实体类
     */
    public abstract T build();

    /**
     * 初始化统计数据记录实体类
     *
     * @return 告警统计持久化类
     */
    public abstract S convert();

    /**
     * 初始化统计数据记录实体类
     *
     * @param defectList 告警列表
     * @return 统计数据记录实体类
     */
    public abstract AbstractDefectStatisticModelBuilder<T, S, D> allDefectList(List<D> defectList);

    public AbstractDefectStatisticModelBuilder<T, S, D> taskId(Long taskId) {
        defectStatisticModel.setTaskId(taskId);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> toolName(String toolName) {
        defectStatisticModel.setToolName(toolName);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> createFrom(String createFrom) {
        defectStatisticModel.setCreateFrom(createFrom);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> buildId(String buildId) {
        defectStatisticModel.setBuildId(buildId);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> baseBuildId(String baseBuildId) {
        defectStatisticModel.setBaseBuildId(baseBuildId);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> checkerKeyToCategoryMap(Map<String, String> map) {
        defectStatisticModel.setCheckerKeyToCategoryMap(map);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> migrationSuccessful(boolean isMigrationSuccessful) {
        defectStatisticModel.setMigrationSuccessful(isMigrationSuccessful);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> dimensionStatistic(
            DimensionStatisticModel dimensionStatisticModel
    ) {
        defectStatisticModel.setDimensionStatisticModel(dimensionStatisticModel);
        return this;
    }

    public AbstractDefectStatisticModelBuilder<T, S, D> newCountCheckerList(List<String> newCountChecker) {
        defectStatisticModel.setNewCountCheckerList(newCountChecker);
        return this;
    }
}
