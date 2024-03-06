package com.tencent.bk.codecc.defect.service.statistic;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.CommonDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.CommonDefectStatisticModelBuilder;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DimensionStatisticModel;
import com.tencent.bk.codecc.defect.service.AbstractDefectStatisticService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * 编译工具告警统计类
 * 遍历逻辑由抽象类实现
 *
 * @author warmli
 */
@Service
@Slf4j
public class CommonDefectStatisticServiceImpl
        extends AbstractDefectStatisticService<CommonDefectEntity, CommonDefectStatisticModel> {

    @Autowired
    private StatisticDao statisticDao;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private CheckerRepository checkerRepository;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    @Override
    public CommonDefectStatisticModel buildStatisticModel(
            DefectStatisticModel<CommonDefectEntity> defectStatisticModel) {
        boolean isMigrationSuccessful =
                commonDefectMigrationService.isMigrationSuccessful(defectStatisticModel.getTaskDetailVO().getTaskId());
        Map<String, String> checkerKeyToCategoryMap = !isMigrationSuccessful ? Maps.newHashMap()
                : getCheckerKeyToCategoryMap(defectStatisticModel);
        DimensionStatisticModel dimensionStatistic = isMigrationSuccessful ? new DimensionStatisticModel() : null;

        return new CommonDefectStatisticModelBuilder()
                .fastIncrementFlag(defectStatisticModel.getFastIncrementFlag())
                .newCountCheckerList(defectStatisticModel.getNewCountCheckers())
                .taskId(defectStatisticModel.getTaskDetailVO().getTaskId())
                .toolName(defectStatisticModel.getToolName())
                .createFrom(defectStatisticModel.getTaskDetailVO().getCreateFrom())
                .buildId(defectStatisticModel.getBuildId())
                .allDefectList(defectStatisticModel.getDefectList())
                .migrationSuccessful(isMigrationSuccessful)
                .checkerKeyToCategoryMap(checkerKeyToCategoryMap)
                .dimensionStatistic(dimensionStatistic)
                .build();
    }

    /**
     * 判断告警是否是 "待修复" 状态，statisticService 只统计待修复告警
     *
     * @param defectEntity 抽象告警实体类
     * @return 返回 true 时代表当前告警状态是 "待修复"
     */
    @Override
    public boolean isStatusNew(CommonDefectEntity defectEntity) {
        return defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value();
    }

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefect(CommonDefectEntity defectEntity, CommonDefectStatisticModel statisticModel) {
        statisticModel.getAllNewDefects().add(defectEntity);
    }

    /**
     * 统计新增告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticNewDefect(CommonDefectEntity defectEntity, CommonDefectStatisticModel statisticModel) {
        Set<String> authors = CollectionUtils.isEmpty(defectEntity.getAuthorList())
                ? new HashSet<>() : defectEntity.getAuthorList();
        statisticModel.getNewAuthors().addAll(authors);
        if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0) {
            statisticModel.incTotalNewPrompt();
            statisticModel.getNewPromptAuthors().addAll(authors);
        }
        if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0) {
            statisticModel.incTotalNewNormal();
            statisticModel.getNewNormalAuthors().addAll(authors);
        }
        if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0) {
            statisticModel.incTotalNewSerious();
            statisticModel.getNewSeriousAuthors().addAll(authors);
        }

        // common类的新增待修复是指platform真实新增，逻辑处理在#statisticPostHandleBeforeSave(..)，这里只统计出total总待修复
        statisticTotalDefectByDimension(statisticModel, defectEntity);
    }

    /**
     * 统计遗留告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticOldDefect(CommonDefectEntity defectEntity, CommonDefectStatisticModel statisticModel) {
        Set<String> authors = CollectionUtils.isEmpty(defectEntity.getAuthorList())
                ? new HashSet<>() : defectEntity.getAuthorList();
        statisticModel.getOldAuthors().addAll(authors);
        if ((defectEntity.getSeverity() & ComConstants.PROMPT) > 0) {
            statisticModel.incTotalOldPrompt();
            statisticModel.getOldPromptAuthors().addAll(authors);
        }
        if ((defectEntity.getSeverity() & ComConstants.NORMAL) > 0) {
            statisticModel.incTotalOldNormal();
            statisticModel.getOldNormalAuthors().addAll(authors);
        }
        if ((defectEntity.getSeverity() & ComConstants.SERIOUS) > 0) {
            statisticModel.incTotalOldSerious();
            statisticModel.getOldSeriousAuthors().addAll(authors);
        }

        statisticTotalDefectByDimension(statisticModel, defectEntity);
    }

    /**
     * 统计相对于上一次扫描的告警变动数
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefectChange(CommonDefectStatisticModel statisticModel) {

    }

    /**
     * 统计所有"待修复"告警的规则信息
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChecker(CommonDefectStatisticModel statisticModel) {

    }

    /**
     * 统计告警图表数据,DUPC CCN
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChart(CommonDefectStatisticModel statisticModel) {

    }

    /**
     * 将 statisticModel 转化为 statisticEntity，保存到对应表
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void buildAndSaveStatisticResult(CommonDefectStatisticModel statisticModel) {

    }

    /**
     * 异步统计 "已修复"、"已屏蔽"、"已忽略" 状态的告警
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void asyncStatisticDefect(CommonDefectStatisticModel statisticModel) {

    }

    /**
     * 将统计数据推送到数据平台
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void pushDataKafka(CommonDefectStatisticModel statisticModel) {

    }

    @Override
    public void statisticPostHandleBeforeSave(CommonDefectStatisticModel statisticModel) {
        statisticNewDefectByDimension(statisticModel);
    }

    /**
     * 保存统计信息至redis
     *
     * @param statisticModel
     * @param taskId
     * @param toolName
     * @param buildNum
     */
    public void saveStatisticToRedis(CommonDefectStatisticModel statisticModel, long taskId, String toolName,
            String buildNum) {
        List<Pair<ComConstants.StaticticItem, Integer>> counterList = Arrays.asList(
                Pair.of(ComConstants.StaticticItem.EXIST_PROMPT,
                        statisticModel.getTotalNewPrompt() + statisticModel.getTotalOldPrompt()),
                Pair.of(ComConstants.StaticticItem.EXIST_NORMAL,
                        statisticModel.getTotalNewNormal() + statisticModel.getTotalOldNormal()),
                Pair.of(ComConstants.StaticticItem.EXIST_SERIOUS,
                        statisticModel.getTotalNewSerious() + statisticModel.getTotalOldSerious()),
                Pair.of(ComConstants.StaticticItem.NEW_PROMPT, statisticModel.getTotalNewPrompt()),
                Pair.of(ComConstants.StaticticItem.NEW_NORMAL, statisticModel.getTotalNewNormal()),
                Pair.of(ComConstants.StaticticItem.NEW_SERIOUS, statisticModel.getTotalNewSerious()));

        statisticDao.increaseDefectCountByStatusBatch(taskId, toolName, buildNum, counterList);
        statisticDao.addNewAndExistAuthors(taskId, toolName, buildNum, statisticModel.getNewAuthors(),
                statisticModel.getOldAuthors());
        statisticDao.addSeverityAuthors(taskId, toolName, buildNum, statisticModel.getNewPromptAuthors(),
                statisticModel.getNewNormalAuthors(), statisticModel.getNewSeriousAuthors());
        statisticDao.addExistSeverityAuthors(taskId, toolName, buildNum, statisticModel.getOldPromptAuthors(),
                statisticModel.getOldNormalAuthors(), statisticModel.getOldSeriousAuthors());

        statisticDao.addDimensionStatistic(taskId, toolName, buildNum, statisticModel.getDimensionStatisticModel());
    }

    private Map<String, String> getCheckerKeyToCategoryMap(
            DefectStatisticModel<CommonDefectEntity> defectStatisticModel
    ) {
        return checkerRepository.findStatisticFieldByToolName(defectStatisticModel.getToolName())
                .stream()
                .collect(
                        Collectors.toMap(
                                CheckerDetailEntity::getCheckerKey,
                                CheckerDetailEntity::getCheckerCategory,
                                (k, v) -> v
                        )
                );
    }
}
