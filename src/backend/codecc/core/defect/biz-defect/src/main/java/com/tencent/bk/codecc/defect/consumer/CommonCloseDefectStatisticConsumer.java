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

package com.tencent.bk.codecc.defect.consumer;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.DimensionStatisticEntity;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IConsumer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Lint已关闭告警统计消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component
@Slf4j
public class CommonCloseDefectStatisticConsumer implements IConsumer<CommonStatisticEntity> {

    @Autowired
    private DefectRepository defectRepository;
    @Autowired
    private CommonStatisticRepository commonStatisticRepository;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private DefectConverter defectConverter;
    @Autowired
    private CheckerRepository checkerRepository;

    @Override
    public void consumer(CommonStatisticEntity statisticEntity) {
        try {
            businessCore(statisticEntity);
        } catch (Throwable e) {
            log.error("CommonCloseDefectStatisticConsumer error, mq obj: {}", statisticEntity, e);
        }
    }

    private void businessCore(CommonStatisticEntity commonStatisticEntity) {
        Long taskId = commonStatisticEntity.getTaskId();
        String toolName = commonStatisticEntity.getToolName();

        long totalSeriousFixedCount = 0L;
        long totalNormalFixedCount = 0L;
        long totalPromptFixedCount = 0L;
        long totalSeriousIgnoreCount = 0L;
        long totalNormalIgnoreCount = 0L;
        long totalPromptIgnoreCount = 0L;
        long totalSeriousMaskCount = 0L;
        long totalNormalMaskCount = 0L;
        long totalPromptMaskCount = 0L;
        int defectFixCount = 0;
        int defectMaskCount = 0;
        int standardFixCount = 0;
        int standardMaskCount = 0;
        int securityFixCount = 0;
        int securityMaskCount = 0;

        // 查询所有已关闭的告警
        boolean migrationSuccessful = commonDefectMigrationService.isMigrationSuccessful(taskId);
        List<CommonDefectEntity> allCloseDefectList;
        if (migrationSuccessful) {
            allCloseDefectList = defectConverter.lintToCommon(
                    lintDefectV2Repository.findCloseDefectByTaskIdAndToolName(taskId, toolName)
            );
        } else {
            allCloseDefectList = defectRepository.findCloseDefectByTaskIdAndToolName(taskId, toolName);
        }

        Map<String, String> checkerKeyToCategoryMap = commonStatisticEntity.getDimensionStatistic() != null
                ? getCheckerKeyToCategoryMap(allCloseDefectList, toolName) : Maps.newHashMap();

        for (CommonDefectEntity defect : allCloseDefectList) {
            int status = defect.getStatus();
            int severity = defect.getSeverity();
            String checkerCategory = checkerKeyToCategoryMap.get(defect.getChecker());

            if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value())) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousFixedCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalFixedCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptFixedCount++;
                        break;
                    default:
                        break;
                }

                if (!StringUtils.isEmpty(checkerCategory)) {
                    if (CheckerCategory.CODE_DEFECT.name().equalsIgnoreCase(checkerCategory)) {
                        defectFixCount++;
                    } else if (CheckerCategory.CODE_FORMAT.name().equalsIgnoreCase(checkerCategory)) {
                        standardFixCount++;
                    } else if (CheckerCategory.SECURITY_RISK.name().equalsIgnoreCase(checkerCategory)) {
                        securityFixCount++;
                    }
                }
            } else if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousIgnoreCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalIgnoreCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptIgnoreCount++;
                        break;
                    default:
                        break;
                }
            } else if (status >= ComConstants.DefectStatus.PATH_MASK.value()) {
                switch (severity) {
                    case ComConstants.SERIOUS:
                        totalSeriousMaskCount++;
                        break;
                    case ComConstants.NORMAL:
                        totalNormalMaskCount++;
                        break;
                    case ComConstants.PROMPT:
                    case ComConstants.PROMPT_IN_DB:
                        totalPromptMaskCount++;
                        break;
                    default:
                        break;
                }

                if (!StringUtils.isEmpty(checkerCategory)) {
                    if (CheckerCategory.CODE_DEFECT.name().equalsIgnoreCase(checkerCategory)) {
                        defectMaskCount++;
                    } else if (CheckerCategory.CODE_FORMAT.name().equalsIgnoreCase(checkerCategory)) {
                        standardMaskCount++;
                    } else if (CheckerCategory.SECURITY_RISK.name().equalsIgnoreCase(checkerCategory)) {
                        securityMaskCount++;
                    }
                }
            }
        }
        commonStatisticEntity.setSeriousFixedCount(totalSeriousFixedCount);
        commonStatisticEntity.setNormalFixedCount(totalNormalFixedCount);
        commonStatisticEntity.setPromptFixedCount(totalPromptFixedCount);
        commonStatisticEntity.setSeriousIgnoreCount(totalSeriousIgnoreCount);
        commonStatisticEntity.setNormalIgnoreCount(totalNormalIgnoreCount);
        commonStatisticEntity.setPromptIgnoreCount(totalPromptIgnoreCount);
        commonStatisticEntity.setSeriousMaskCount(totalSeriousMaskCount);
        commonStatisticEntity.setNormalMaskCount(totalNormalMaskCount);
        commonStatisticEntity.setPromptMaskCount(totalPromptMaskCount);

        DimensionStatisticEntity dimensionStatistic = commonStatisticEntity.getDimensionStatistic();
        if (dimensionStatistic != null) {
            dimensionStatistic.setDefectFixCount(defectFixCount);
            dimensionStatistic.setDefectMaskCount(defectMaskCount);
            dimensionStatistic.setStandardFixCount(standardFixCount);
            dimensionStatistic.setStandardMaskCount(standardMaskCount);
            dimensionStatistic.setSecurityFixCount(securityFixCount);
            dimensionStatistic.setSecurityMaskCount(securityMaskCount);
        }

        commonStatisticRepository.save(commonStatisticEntity);
    }

    private Map<String, String> getCheckerKeyToCategoryMap(
            List<CommonDefectEntity> allCloseDefectList,
            String toolName
    ) {
        if (CollectionUtils.isEmpty(allCloseDefectList)) {
            return Maps.newHashMap();
        }

        Set<String> checkers = allCloseDefectList.stream()
                .map(CommonDefectEntity::getChecker)
                .collect(Collectors.toSet());

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
