/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
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
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.statistic.DimensionStatisticEntity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.util.ThreadUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
public class LintCloseDefectStatisticConsumer implements IConsumer<LintStatisticEntity> {

    private static final Map<String, Boolean> IGNORE_DEFECT_LIST_FILED_MAP = new HashMap<String, Boolean>() {{
        put("severity", true);
        put("_id", false);
    }};
    private static final int BATCH_SIZE = 3_0000;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Override
    public void consumer(LintStatisticEntity lintStatisticEntity) {
        try {
            long taskId = lintStatisticEntity.getTaskId();
            String buildId = lintStatisticEntity.getBuildId();
            long beginTime = System.currentTimeMillis();
            log.info("LintCloseDefectStatisticConsumer begin, {}, {}", taskId, buildId);
            businessCore(lintStatisticEntity);
            log.info("LintCloseDefectStatisticConsumer end, {}, {}, cost: {}", taskId, buildId,
                    System.currentTimeMillis() - beginTime);
        } catch (Throwable t) {
            log.error("LintCloseDefectStatisticConsumer error, mq obj: {}", lintStatisticEntity, t);
        }
    }

    private void businessCore(LintStatisticEntity lintStatisticEntity) {
        if (Boolean.TRUE.equals(lintStatisticEntity.getFastIncrementFlag())
                && !StringUtils.isEmpty(lintStatisticEntity.getBaseBuildId())) {
            fastIncrementStatistic(lintStatisticEntity);
            return;
        }

        Long taskId = lintStatisticEntity.getTaskId();
        String toolName = lintStatisticEntity.getToolName();
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
        int pageIndex = 0;

        while (true) {
            PageRequest pageRequest = PageRequest.of(pageIndex++, BATCH_SIZE);
            List<LintDefectV2Entity> defectList =
                    lintDefectV2Repository.findCloseDefectByTaskIdAndToolName(taskId, toolName, pageRequest);
            if (CollectionUtils.isEmpty(defectList)) {
                break;
            }

            // lintStatisticEntity.getDimensionStatistic()不为空，则说明上游服务判断已经数据迁移成功，需要记录按规则统计维度信息
            Map<String, String> checkerKeyToCategoryMap = lintStatisticEntity.getDimensionStatistic() != null
                    ? getCheckerKeyToCategoryMap(defectList, toolName) : Maps.newHashMap();

            for (LintDefectV2Entity defect : defectList) {
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
                } else if (status == (ComConstants.DefectStatus.NEW.value()
                        | ComConstants.DefectStatus.IGNORE.value())) {
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

            if (defectList.size() < BATCH_SIZE) {
                break;
            }

            defectList.clear();
            defectList = null;
            ThreadUtils.sleep(TimeUnit.MILLISECONDS.toMillis(500));
        }

        lintStatisticEntity.setSeriousFixedCount(totalSeriousFixedCount);
        lintStatisticEntity.setNormalFixedCount(totalNormalFixedCount);
        lintStatisticEntity.setPromptFixedCount(totalPromptFixedCount);
        lintStatisticEntity.setSeriousIgnoreCount(totalSeriousIgnoreCount);
        lintStatisticEntity.setNormalIgnoreCount(totalNormalIgnoreCount);
        lintStatisticEntity.setPromptIgnoreCount(totalPromptIgnoreCount);
        lintStatisticEntity.setSeriousMaskCount(totalSeriousMaskCount);
        lintStatisticEntity.setNormalMaskCount(totalNormalMaskCount);
        lintStatisticEntity.setPromptMaskCount(totalPromptMaskCount);

        DimensionStatisticEntity dimensionStatistic = lintStatisticEntity.getDimensionStatistic();
        if (dimensionStatistic != null) {
            dimensionStatistic.setDefectFixCount(defectFixCount);
            dimensionStatistic.setDefectMaskCount(defectMaskCount);
            dimensionStatistic.setStandardFixCount(standardFixCount);
            dimensionStatistic.setStandardMaskCount(standardMaskCount);
            dimensionStatistic.setSecurityFixCount(securityFixCount);
            dimensionStatistic.setSecurityMaskCount(securityMaskCount);
        }

        lintStatisticRepository.save(lintStatisticEntity);
    }

    private void fastIncrementStatistic(LintStatisticEntity curStatisticEntity) {
        Long taskId = curStatisticEntity.getTaskId();
        String toolName = curStatisticEntity.getToolName();

        long totalSeriousIgnoreCount = 0L;
        long totalNormalIgnoreCount = 0L;
        long totalPromptIgnoreCount = 0L;
        int pageIndex = 0;

        while (true) {
            PageRequest pageRequest = PageRequest.of(pageIndex++, BATCH_SIZE);
            List<LintDefectV2Entity> ignoreDefectList =
                    lintDefectV2Dao.findIgnoreDefects(taskId, toolName, pageRequest, IGNORE_DEFECT_LIST_FILED_MAP);
            if (CollectionUtils.isEmpty(ignoreDefectList)) {
                break;
            }

            for (LintDefectV2Entity defect : ignoreDefectList) {
                int severity = defect.getSeverity();
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
            }

            if (ignoreDefectList.size() < BATCH_SIZE) {
                break;
            }

            ignoreDefectList.clear();
            ignoreDefectList = null;
            // 覆盖索引效率高成本低，适当调低休眠时间
            ThreadUtils.sleep(TimeUnit.MILLISECONDS.toMillis(50));
        }

        curStatisticEntity.setSeriousIgnoreCount(totalSeriousIgnoreCount);
        curStatisticEntity.setNormalIgnoreCount(totalNormalIgnoreCount);
        curStatisticEntity.setPromptIgnoreCount(totalPromptIgnoreCount);

        String baseBuildId = curStatisticEntity.getBaseBuildId();
        LintStatisticEntity lastStatisticEntity =
                lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, baseBuildId);
        if (lastStatisticEntity == null) {
            log.warn("last lint statistic is null, current: {}", curStatisticEntity);
            curStatisticEntity.setSeriousFixedCount(0L);
            curStatisticEntity.setNormalFixedCount(0L);
            curStatisticEntity.setPromptFixedCount(0L);
            curStatisticEntity.setSeriousMaskCount(0L);
            curStatisticEntity.setNormalMaskCount(0L);
            curStatisticEntity.setPromptMaskCount(0L);
            curStatisticEntity.setDimensionStatistic(new DimensionStatisticEntity());
        } else {
            curStatisticEntity.setSeriousFixedCount(lastStatisticEntity.getSeriousFixedCount());
            curStatisticEntity.setNormalFixedCount(lastStatisticEntity.getNormalFixedCount());
            curStatisticEntity.setPromptFixedCount(lastStatisticEntity.getPromptFixedCount());
            curStatisticEntity.setSeriousMaskCount(lastStatisticEntity.getSeriousMaskCount());
            curStatisticEntity.setNormalMaskCount(lastStatisticEntity.getNormalMaskCount());
            curStatisticEntity.setPromptMaskCount(lastStatisticEntity.getPromptMaskCount());
            curStatisticEntity.setDimensionStatistic(lastStatisticEntity.getDimensionStatistic());
        }

        lintStatisticRepository.save(curStatisticEntity);
    }

    private Map<String, String> getCheckerKeyToCategoryMap(
            List<LintDefectV2Entity> allCloseDefectList,
            String toolName
    ) {
        if (CollectionUtils.isEmpty(allCloseDefectList)) {
            return Maps.newHashMap();
        }

        Set<String> checkers = allCloseDefectList.stream()
                .map(LintDefectV2Entity::getChecker)
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
