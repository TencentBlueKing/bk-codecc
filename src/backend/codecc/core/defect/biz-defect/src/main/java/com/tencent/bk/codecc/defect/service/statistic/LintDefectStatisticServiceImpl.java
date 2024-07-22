/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.statistic;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DimensionStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.LintDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.LintDefectStatisticModelBuilder;
import com.tencent.bk.codecc.defect.service.AbstractDefectStatisticService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.utils.CommonKafkaClient;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.IterableUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Lint告警统计
 *
 * @author warmli
 * @version V2.0
 * @date 2021/10/29
 */
@Component
@Slf4j
public class LintDefectStatisticServiceImpl
        extends AbstractDefectStatisticService<LintDefectV2Entity, LintDefectStatisticModel> {
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private CommonKafkaClient commonKafkaClient;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    /**
     * 构建对应工具的统计数据记录实体
     *
     * @param defectStatisticModel 告警统计入参
     * @return 统计数据记录实体
     */
    @Override
    public LintDefectStatisticModel buildStatisticModel(DefectStatisticModel<LintDefectV2Entity> defectStatisticModel) {
        boolean isMigrationSuccessful =
                commonDefectMigrationService.isMigrationSuccessful(defectStatisticModel.getTaskDetailVO().getTaskId());
        Map<String, String> checkerKeyToCategoryMap = !isMigrationSuccessful ? Maps.newHashMap()
                : getCheckerKeyToCategoryMap(defectStatisticModel);
        DimensionStatisticModel dimensionStatistic = isMigrationSuccessful ? new DimensionStatisticModel() : null;

        return new LintDefectStatisticModelBuilder()
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
    public boolean isStatusNew(LintDefectV2Entity defectEntity) {
        return defectEntity.getStatus() == ComConstants.DefectStatus.NEW.value();
    }

    /**
     * 统计所有"待修复"告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefect(LintDefectV2Entity defectEntity, LintDefectStatisticModel statisticModel) {
        statisticModel.getAllNewDefects().add(defectEntity);
        statisticModel.getFilePathSet().add(defectEntity.getFilePath());
    }

    /**
     * 统计新增告警
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticNewDefect(LintDefectV2Entity defectEntity, LintDefectStatisticModel statisticModel) {
        Map<String, NotRepairedAuthorEntity> authorDefectMap = statisticModel.getAuthorDefectMap();
        String author = IterableUtils.getFirst(defectEntity.getAuthor(), "");
        NotRepairedAuthorEntity notRepairedAuthorEntity = authorDefectMap.get(author);
        if (StringUtils.isNotEmpty(author) && notRepairedAuthorEntity == null) {
            notRepairedAuthorEntity = new NotRepairedAuthorEntity();
            notRepairedAuthorEntity.setName(author);
            authorDefectMap.put(author, notRepairedAuthorEntity);
        }

        if (defectEntity.getSeverity() == ComConstants.SERIOUS) {
            statisticModel.incTotalNewSerious();
            if (notRepairedAuthorEntity != null) {
                notRepairedAuthorEntity.incSeriousCount();
            }
        } else if (defectEntity.getSeverity() == ComConstants.NORMAL) {
            statisticModel.incTotalNewNormal();
            if (notRepairedAuthorEntity != null) {
                notRepairedAuthorEntity.incNormalCount();
            }
        } else if (defectEntity.getSeverity() == ComConstants.PROMPT
                || defectEntity.getSeverity() == ComConstants.PROMPT_IN_DB) {
            statisticModel.incTotalNewPrompt();
            if (notRepairedAuthorEntity != null) {
                notRepairedAuthorEntity.incPromptCount();
            }
        }

        statisticTotalDefectByDimension(statisticModel, defectEntity);

        // 统计用户新增告警数总和
        if (notRepairedAuthorEntity != null) {
            notRepairedAuthorEntity.incTotalCount();
        }
    }

    /**
     * 统计遗留告警
     *
     * @param defectEntity 抽象告警实体类
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticOldDefect(LintDefectV2Entity defectEntity, LintDefectStatisticModel statisticModel) {
        Map<String, NotRepairedAuthorEntity> existAuthorMap = statisticModel.getExistAuthorMap();
        String author = IterableUtils.getFirst(defectEntity.getAuthor(), "");
        NotRepairedAuthorEntity existAuthorEntity = existAuthorMap.get(author);
        if (StringUtils.isNotEmpty(author) && existAuthorEntity == null) {
            existAuthorEntity = new NotRepairedAuthorEntity();
            existAuthorEntity.setName(author);
            existAuthorMap.put(author, existAuthorEntity);
        }

        if (defectEntity.getSeverity() == ComConstants.SERIOUS) {
            statisticModel.incTotalOldSerious();
            if (existAuthorEntity != null) {
                existAuthorEntity.incSeriousCount();
            }
        } else if (defectEntity.getSeverity() == ComConstants.NORMAL) {
            statisticModel.incTotalOldNormal();
            if (existAuthorEntity != null) {
                existAuthorEntity.incNormalCount();
            }
        } else if (defectEntity.getSeverity() == ComConstants.PROMPT
                || defectEntity.getSeverity() == ComConstants.PROMPT_IN_DB) {
            statisticModel.incTotalOldPrompt();
            if (existAuthorEntity != null) {
                existAuthorEntity.incPromptCount();
            }
        }

        statisticTotalDefectByDimension(statisticModel, defectEntity);

        // 统计用户新增告警数总和
        if (existAuthorEntity != null) {
            existAuthorEntity.incTotalCount();
        }
    }

    /**
     * 统计相对于上一次扫描的告警变动数
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticDefectChange(LintDefectStatisticModel statisticModel) {
        log.info("lint statistic defect change, cur: {}, base: {}", statisticModel.getBuildId(),
                statisticModel.getBaseBuildId());

        int defectChange;
        int fileChange;
        int fileCount = statisticModel.getFilePathSet().size();
        int defectCount = statisticModel.getAllNewDefects().size();
        LintStatisticEntity lastLintStatisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(
                statisticModel.getTaskId(), statisticModel.getToolName(), statisticModel.getBaseBuildId());


        if (lastLintStatisticEntity == null) {
            defectChange = defectCount;
            fileChange = fileCount;
        } else {
            defectChange = defectCount - (lastLintStatisticEntity.getDefectCount() == null
                    ? 0 : lastLintStatisticEntity.getDefectCount());
            fileChange = fileCount - (lastLintStatisticEntity.getFileCount() == null
                    ? 0 : lastLintStatisticEntity.getFileCount());
        }
        statisticModel.setFileChange(fileChange);
        statisticModel.setDefectChange(defectChange);
    }

    /**
     * 统计所有"待修复"告警的规则信息
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChecker(LintDefectStatisticModel statisticModel) {
        String toolName = statisticModel.getToolName();
        List<LintDefectV2Entity> allDefectEntityList = statisticModel.getAllNewDefects();
        // get checker map
        Set<String> checkerIds = allDefectEntityList.stream()
                .map(LintDefectV2Entity::getChecker).collect(Collectors.toSet());
        Map<String, CheckerDetailEntity> checkerDetailMap = new HashMap<>();
        checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkerIds)
                .forEach(it -> checkerDetailMap.put(it.getCheckerKey(), it));

        // get lint checker statistic data
        Map<String, CheckerStatisticEntity> checkerStatisticEntityMap = new HashMap<>();
        Map<String, Map<Long, CheckerStatisticEntity.CheckerStatisticLanguage>> checkerStatisticLanguageMap
                = new HashMap<>();
        for (LintDefectV2Entity entity : allDefectEntityList) {
            CheckerStatisticEntity item = checkerStatisticEntityMap.get(entity.getChecker());
            if (item == null) {
                item = new CheckerStatisticEntity();
                item.setName(entity.getChecker());

                CheckerDetailEntity checker = checkerDetailMap.get(entity.getChecker());
                if (checker != null) {
                    item.setId(checker.getEntityId());
                    item.setName(checker.getCheckerName());
                    item.setSeverity(checker.getSeverity());
                } else {
                    log.warn("not found checker for tool: {}, {}", toolName, entity.getChecker());
                }
            }
            item.setDefectCount(item.getDefectCount() + 1);
            checkerStatisticEntityMap.put(entity.getChecker(), item);

            if (entity.getLangValue() != null && entity.getLangValue() > 0) {
                Map<Long, CheckerStatisticEntity.CheckerStatisticLanguage> languageMaps
                        = checkerStatisticLanguageMap.getOrDefault(entity.getChecker(), new HashMap<>());
                CheckerStatisticEntity.CheckerStatisticLanguage langStat =
                        languageMaps.getOrDefault(entity.getLangValue(),
                                CheckerStatisticEntity.CheckerStatisticLanguage.newInstance(entity.getLangValue(),
                                        entity.getLanguage()));
                langStat.incDefectCount();
                languageMaps.put(entity.getLangValue(), langStat);
                checkerStatisticLanguageMap.put(entity.getChecker(), languageMaps);
            }

        }
        checkerStatisticEntityMap.values().forEach(entity -> {
            if (checkerStatisticLanguageMap.containsKey(entity.getName())) {
                entity.setLangStats(new ArrayList<>(checkerStatisticLanguageMap.get(entity.getName()).values()));
            }
        });

        statisticModel.setCheckerStatisticList(new ArrayList<>(checkerStatisticEntityMap.values()));
    }

    /**
     * 统计告警图表数据,DUPC CCN
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void statisticChart(LintDefectStatisticModel statisticModel) {

    }

    /**
     * 将 statisticModel 转化为 statisticEntity，保存到对应表
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void buildAndSaveStatisticResult(LintDefectStatisticModel statisticModel) {
        LintStatisticEntity lintStatisticEntity = statisticModel.getBuilder().convert();
        statisticModel.setLintStatisticEntity(lintStatisticEntity);
        lintStatisticRepository.save(lintStatisticEntity);
    }

    /**
     * 异步统计 "已修复"、"已屏蔽"、"已忽略" 状态的告警
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void asyncStatisticDefect(LintDefectStatisticModel statisticModel) {
        LintStatisticEntity mqObj = statisticModel.getLintStatisticEntity();
        mqObj.setFastIncrementFlag(statisticModel.getFastIncrementFlag());
        mqObj.setBaseBuildId(statisticModel.getBaseBuildId());

        // 异步统计非new状态的告警数
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(statisticModel.getCreateFrom())) {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE,
                    ROUTE_CLOSE_DEFECT_STATISTIC_LINT_OPENSOURCE, mqObj);
        } else {
            rabbitTemplate.convertAndSend(EXCHANGE_CLOSE_DEFECT_STATISTIC_LINT,
                    ROUTE_CLOSE_DEFECT_STATISTIC_LINT, mqObj);
        }
    }

    /**
     * 将统计数据推送到数据平台
     *
     * @param statisticModel 统计数据记录实体
     */
    @Override
    public void pushDataKafka(LintDefectStatisticModel statisticModel) {
        commonKafkaClient.pushLintStatisticToKafka(statisticModel.getLintStatisticEntity());
    }

    @Override
    public void statisticPostHandleBeforeSave(LintDefectStatisticModel statisticModel) {
        statisticNewDefectByDimension(statisticModel);
    }

    private Map<String, String> getCheckerKeyToCategoryMap(
            DefectStatisticModel<LintDefectV2Entity> defectStatisticModel
    ) {
        List<LintDefectV2Entity> allNewDefects = defectStatisticModel.getDefectList();
        if (CollectionUtils.isEmpty(allNewDefects)) {
            return Maps.newHashMap();
        }

        String toolName = defectStatisticModel.getToolName();
        Set<String> checkers = allNewDefects.stream().map(LintDefectV2Entity::getChecker).collect(Collectors.toSet());
        return checkerRepository.findStatisticFieldByToolNameAndCheckerKeyIn(toolName, checkers)
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
