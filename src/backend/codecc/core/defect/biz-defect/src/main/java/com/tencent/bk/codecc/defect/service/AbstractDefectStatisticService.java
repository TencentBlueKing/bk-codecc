package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.AbstractDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DimensionStatisticModel;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 抽象告警统计类
 * 遍历抽象告警实体类
 *
 * @author warmli
 */
@Service
@Slf4j
public abstract class AbstractDefectStatisticService<T extends DefectEntity, S extends AbstractDefectStatisticModel>
        implements IDefectStatisticService {

    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    public abstract S buildStatisticModel(DefectStatisticModel<T> defectStatisticModel);

    /**
     * 判断告警是否是 "待修复" 状态，statisticService 只统计待修复告警
     *
     * @param defectEntity 抽象告警实体类
     * @return 返回 true 时代表当前告警状态是 "待修复"
     */
    public abstract boolean isStatusNew(T defectEntity);

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticDefect(T defectEntity, S statisticModel);

    /**
     * 统计新增告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticNewDefect(T defectEntity, S statisticModel);

    /**
     * 统计遗留告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticOldDefect(T defectEntity, S statisticModel);

    /**
     * 统计相对于上一次扫描的告警变动数
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticDefectChange(S statisticModel);

    /**
     * 统计所有"待修复"告警的规则信息
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticChecker(S statisticModel);

    /**
     * 统计告警图表数据,DUPC CCN
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void statisticChart(S statisticModel);

    /**
     * 将 statisticModel 转化为 statisticEntity，保存到对应表
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void buildAndSaveStatisticResult(S statisticModel);

    /**
     * 异步统计 "已修复"、"已屏蔽"、"已忽略" 状态的告警
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void asyncStatisticDefect(S statisticModel);

    /**
     * 将统计数据推送到数据平台
     *
     * @param statisticModel 统计数据记录实体
     */
    public abstract void pushDataKafka(S statisticModel);

    /**
     * 统计后置处理
     *
     * @param statisticModel
     */
    public void statisticPostHandleBeforeSave(S statisticModel) {

    }

    /**
     * 统计本次上报后存在的所有告警
     *
     * @param defectStatisticModel 告警统计入参
     */
    public S statistic(DefectStatisticModel<T> defectStatisticModel) {

        S statisticModel = buildStatisticModel(defectStatisticModel);

        for (T defectEntity : defectStatisticModel.getDefectList()) {
            if (!isStatusNew(defectEntity)) {
                continue;
            }
            // 统计"待修复"告警
            statisticDefect(defectEntity, statisticModel);

            statisticNewDefect(defectEntity, statisticModel);
        }

        // 确认上次构建号
        getBaseBuildId(statisticModel, defectStatisticModel.getToolBuildStackEntity());
        // 对比与上次构建结果的告警数
        statisticDefectChange(statisticModel);
        // 统计告警规则信息,只有 lint 工具
        statisticChecker(statisticModel);
        // 统计告警图表,只有 CCN 和 DUPC
        statisticChart(statisticModel);
        // 后置处理，自行重写
        statisticPostHandleBeforeSave(statisticModel);
        // 保存统计数据
        buildAndSaveStatisticResult(statisticModel);
        // 异步统计非"待修复"状态的告警
        asyncStatisticDefect(statisticModel);
        // 数据推送到数据平台
        pushDataKafka(statisticModel);

        return statisticModel;
    }

    /**
     * 获取当前工具本次构建前一次的成功构建信息
     *
     * @param statisticModel 统计数据记录实体
     * @param toolBuildStackEntity 构建信息实体类
     */
    protected void getBaseBuildId(S statisticModel, ToolBuildStackEntity toolBuildStackEntity) {
        String baseBuildId;
        if (toolBuildStackEntity == null) {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(
                    statisticModel.getTaskId(), statisticModel.getToolName());
            baseBuildId = toolBuildInfoEntity != null
                    && StringUtils.isNotEmpty(toolBuildInfoEntity.getDefectBaseBuildId())
                    ? toolBuildInfoEntity.getDefectBaseBuildId() : "";
        } else {
            baseBuildId = StringUtils.isNotEmpty(toolBuildStackEntity.getBaseBuildId())
                    ? toolBuildStackEntity.getBaseBuildId() : "";
        }
        statisticModel.setBaseBuildId(baseBuildId);
    }

    protected void statisticNewDefectByDimension(S statisticModel) {
        if (!statisticModel.isMigrationSuccessful()) {
            return;
        }

        Map<String, String> checkerKeyToCategoryMap = statisticModel.getCheckerKeyToCategoryMap();
        DimensionStatisticModel dimensionStatisticModel = statisticModel.getDimensionStatisticModel();

        for (String defectChecker : statisticModel.getNewCountCheckerList()) {
            String checkerCategory = checkerKeyToCategoryMap.get(defectChecker);
            if (CheckerCategory.CODE_DEFECT.name().equalsIgnoreCase(checkerCategory)) {
                dimensionStatisticModel.setDefectNewCount(dimensionStatisticModel.getDefectNewCount() + 1);
            } else if (CheckerCategory.CODE_FORMAT.name().equalsIgnoreCase(checkerCategory)) {
                dimensionStatisticModel.setStandardNewCount(dimensionStatisticModel.getStandardNewCount() + 1);
            } else if (CheckerCategory.SECURITY_RISK.name().equalsIgnoreCase(checkerCategory)) {
                dimensionStatisticModel.setSecurityNewCount(dimensionStatisticModel.getSecurityNewCount() + 1);
            }
        }
    }

    protected void statisticTotalDefectByDimension(S statisticModel, T defectEntity) {
        if (!statisticModel.isMigrationSuccessful()) {
            return;
        }

        // 告警对应的规则
        String defectChecker = getEntityChecker(defectEntity);
        if (org.springframework.util.StringUtils.isEmpty(defectChecker)) {
            log.info("checker is empty, entity id: {}", defectEntity.getEntityId());
            return;
        }

        DimensionStatisticModel dimensionStatisticModel = statisticModel.getDimensionStatisticModel();
        Map<String, String> checkerKeyToCategoryMap = statisticModel.getCheckerKeyToCategoryMap();
        String checkerCategory = checkerKeyToCategoryMap.get(defectChecker);

        if (CheckerCategory.CODE_DEFECT.name().equalsIgnoreCase(checkerCategory)) {
            dimensionStatisticModel.setDefectTotalCount(dimensionStatisticModel.getDefectTotalCount() + 1);
        } else if (CheckerCategory.CODE_FORMAT.name().equalsIgnoreCase(checkerCategory)) {
            dimensionStatisticModel.setStandardTotalCount(dimensionStatisticModel.getStandardTotalCount() + 1);
        } else if (CheckerCategory.SECURITY_RISK.name().equalsIgnoreCase(checkerCategory)) {
            dimensionStatisticModel.setSecurityTotalCount(dimensionStatisticModel.getSecurityTotalCount() + 1);
        }
    }

    private String getEntityChecker(T defectEntity) {
        String defectChecker = "";
        if (defectEntity instanceof CommonDefectEntity) {
            defectChecker = ((CommonDefectEntity) defectEntity).getChecker();
        } else if (defectEntity instanceof LintDefectV2Entity) {
            defectChecker = ((LintDefectV2Entity) defectEntity).getChecker();
        }

        return defectChecker;
    }
}
