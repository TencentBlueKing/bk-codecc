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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.ThreadUtils;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * CCN已关闭告警统计消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("ccnCloseDefectStatisticConsumer")
@Slf4j
public class CCNCloseDefectStatisticConsumer implements IConsumer<CCNStatisticEntity> {

    private static final int BATCH_SIZE = 3_0000;
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    private CCNDefectDao ccnDefectDao;


    @Override
    public void consumer(CCNStatisticEntity ccnStatisticEntity) {
        try {
            long taskId = ccnStatisticEntity.getTaskId();
            String buildId = ccnStatisticEntity.getBuildId();
            log.info("CCNCloseDefectStatisticConsumer begin, {}, {}", taskId, buildId);
            businessCore(ccnStatisticEntity);
            log.info("CCNCloseDefectStatisticConsumer end, {}, {}", taskId, buildId);
        } catch (Throwable t) {
            log.error("CCNCloseDefectStatisticConsumer error, mq obj: {}", ccnStatisticEntity, t);
        }
    }

    private void businessCore(CCNStatisticEntity ccnStatisticEntity) {
        if (Boolean.TRUE.equals(ccnStatisticEntity.getFastIncrementFlag())
                && !StringUtils.isEmpty(ccnStatisticEntity.getBaseBuildId())) {
            fastIncrementStatistic(ccnStatisticEntity);
            return;
        }

        RiskLevelConfig riskLevelConfig = RiskLevelConfig.get();
        int sh = riskLevelConfig.getSh();
        int h = riskLevelConfig.getH();
        int m = riskLevelConfig.getM();

        long totalSuperHighFixedCount = 0L;
        long totalHighFixedCount = 0L;
        long totalMiddleFixedCount = 0L;
        long totalLowFixedCount = 0L;
        long totalSuperHighIgnoreCount = 0L;
        long totalHighIgnoreCount = 0L;
        long totalMiddleIgnoreCount = 0L;
        long totalLowIgnoreCount = 0L;
        long totalSuperHighMaskCount = 0L;
        long totalHighMaskCount = 0L;
        long totalMiddleMaskCount = 0L;
        long totalLowMaskCount = 0L;

        // 查询所有已关闭的告警
        Long taskId = ccnStatisticEntity.getTaskId();
        long beginTime = System.currentTimeMillis();
        log.info("begin find all close defects for CCNCloseDefectStatisticConsumer, {}", taskId);
        List<CCNDefectEntity> allCloseDefectList = findAllCloseDefects(taskId);
        log.info("end find all close defects for CCNCloseDefectStatisticConsumer, {}, size: {}, cost: {}", taskId,
                allCloseDefectList.size(), System.currentTimeMillis() - beginTime);

        for (CCNDefectEntity defect : allCloseDefectList) {
            int status = defect.getStatus();
            int ccn = defect.getCcn();
            if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value())) {
                if (ccn >= sh) {
                    totalSuperHighFixedCount++;
                } else if (ccn >= h) {
                    totalHighFixedCount++;
                } else if (ccn >= m) {
                    totalMiddleFixedCount++;
                } else {
                    totalLowFixedCount++;
                }
            } else if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())) {
                if (ccn >= sh) {
                    totalSuperHighIgnoreCount++;
                } else if (ccn >= h) {
                    totalHighIgnoreCount++;
                } else if (ccn >= m) {
                    totalMiddleIgnoreCount++;
                } else {
                    totalLowIgnoreCount++;
                }
            } else if (status >= ComConstants.DefectStatus.PATH_MASK.value()) {
                if (ccn >= sh) {
                    totalSuperHighMaskCount++;
                } else if (ccn >= h) {
                    totalHighMaskCount++;
                } else if (ccn >= m) {
                    totalMiddleMaskCount++;
                } else {
                    totalLowMaskCount++;
                }
            }
        }
        ccnStatisticEntity.setSuperHighFixedCount(totalSuperHighFixedCount);
        ccnStatisticEntity.setHighFixedCount(totalHighFixedCount);
        ccnStatisticEntity.setMiddleFixedCount(totalMiddleFixedCount);
        ccnStatisticEntity.setLowFixedCount(totalLowFixedCount);
        ccnStatisticEntity.setSuperHighIgnoreCount(totalSuperHighIgnoreCount);
        ccnStatisticEntity.setHighIgnoreCount(totalHighIgnoreCount);
        ccnStatisticEntity.setMiddleIgnoreCount(totalMiddleIgnoreCount);
        ccnStatisticEntity.setLowIgnoreCount(totalLowIgnoreCount);
        ccnStatisticEntity.setSuperHighMaskCount(totalSuperHighMaskCount);
        ccnStatisticEntity.setHighMaskCount(totalHighMaskCount);
        ccnStatisticEntity.setMiddleMaskCount(totalMiddleMaskCount);
        ccnStatisticEntity.setLowMaskCount(totalLowMaskCount);

        ccnStatisticRepository.save(ccnStatisticEntity);
    }

    private List<CCNDefectEntity> findAllCloseDefects(Long taskId) {
        int pageIndex = 0;
        List<CCNDefectEntity> retList = Lists.newArrayList();

        while (true) {
            PageRequest pageRequest = PageRequest.of(pageIndex++, BATCH_SIZE);
            List<CCNDefectEntity> tempList = ccnDefectRepository.findCloseDefectByTaskId(taskId, pageRequest);

            if (tempList != null) {
                retList.addAll(tempList);
            }

            if (CollectionUtils.isEmpty(tempList) || tempList.size() < BATCH_SIZE) {
                break;
            }

            ThreadUtils.sleep(500);
        }

        return retList;
    }

    private List<CCNDefectEntity> findIgnoreDefects(Long taskId) {
        int pageIndex = 0;
        List<CCNDefectEntity> retList = Lists.newArrayList();
        Map<String, Boolean> fieldMap = Maps.newHashMap();
        fieldMap.put("ccn", true);
        fieldMap.put("_id", false);

        while (true) {
            PageRequest pageRequest = PageRequest.of(pageIndex++, BATCH_SIZE);
            List<CCNDefectEntity> tempList = ccnDefectDao.findIgnoreDefects(taskId, pageRequest, fieldMap);
            if (tempList != null) {
                retList.addAll(tempList);
            }

            if (CollectionUtils.isEmpty(tempList) || tempList.size() < BATCH_SIZE) {
                break;
            }

            ThreadUtils.sleep(500);
        }

        return retList;
    }

    private void fastIncrementStatistic(CCNStatisticEntity curStatisticEntity) {
        Long taskId = curStatisticEntity.getTaskId();
        long beginTime = System.currentTimeMillis();
        log.info("begin find ignore defects for CCNCloseDefectStatisticConsumer, {}", taskId);
        List<CCNDefectEntity> ignoreDefects = findIgnoreDefects(taskId);
        log.info("end find ignore defects for CCNCloseDefectStatisticConsumer, {}, size: {}, cost: {}", taskId,
                ignoreDefects.size(), System.currentTimeMillis() - beginTime);

        RiskLevelConfig riskLevelConfig = RiskLevelConfig.get();
        int sh = riskLevelConfig.getSh();
        int h = riskLevelConfig.getH();
        int m = riskLevelConfig.getM();

        long totalSuperHighIgnoreCount = 0L;
        long totalHighIgnoreCount = 0L;
        long totalMiddleIgnoreCount = 0L;
        long totalLowIgnoreCount = 0L;
        for (CCNDefectEntity defect : ignoreDefects) {
            int ccn = defect.getCcn();
            if (ccn >= sh) {
                totalSuperHighIgnoreCount++;
            } else if (ccn >= h) {
                totalHighIgnoreCount++;
            } else if (ccn >= m) {
                totalMiddleIgnoreCount++;
            } else {
                totalLowIgnoreCount++;
            }
        }
        curStatisticEntity.setSuperHighIgnoreCount(totalSuperHighIgnoreCount);
        curStatisticEntity.setHighIgnoreCount(totalHighIgnoreCount);
        curStatisticEntity.setMiddleIgnoreCount(totalMiddleIgnoreCount);
        curStatisticEntity.setLowIgnoreCount(totalLowIgnoreCount);

        CCNStatisticEntity lastStatisticEntity =
                ccnStatisticRepository.findFirstByTaskIdAndBuildId(taskId, curStatisticEntity.getBaseBuildId());
        if (lastStatisticEntity == null) {
            log.warn("last ccn statistic is null, current: {}", curStatisticEntity);
            curStatisticEntity.setSuperHighFixedCount(0L);
            curStatisticEntity.setHighFixedCount(0L);
            curStatisticEntity.setMiddleFixedCount(0L);
            curStatisticEntity.setLowFixedCount(0L);
            curStatisticEntity.setSuperHighMaskCount(0L);
            curStatisticEntity.setHighMaskCount(0L);
            curStatisticEntity.setMiddleMaskCount(0L);
            curStatisticEntity.setLowMaskCount(0L);
        } else {
            curStatisticEntity.setSuperHighFixedCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setHighFixedCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setMiddleFixedCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setLowFixedCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setSuperHighMaskCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setHighMaskCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setMiddleMaskCount(lastStatisticEntity.getSuperHighFixedCount());
            curStatisticEntity.setLowMaskCount(lastStatisticEntity.getSuperHighFixedCount());
        }

        ccnStatisticRepository.save(curStatisticEntity);
    }

    @AllArgsConstructor
    @Data
    public static class RiskLevelConfig {

        private static volatile RiskLevelConfig INSTANCE;
        private int sh;
        private int h;
        private int m;

        private RiskLevelConfig() {

        }

        /**
         * 获取圈复杂度风险级别配置
         *
         * @return
         */
        public static RiskLevelConfig get() {
            if (INSTANCE == null) {
                synchronized (RiskLevelConfig.class) {
                    if (INSTANCE == null) {
                        Map<String, String> riskConfigMap =
                                SpringContextUtil.Companion.getBean(ThirdPartySystemCaller.class)
                                        .getRiskFactorConfig(ComConstants.Tool.CCN.name());

                        INSTANCE = new RiskLevelConfig(
                                Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.SH.name())),
                                Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.H.name())),
                                Integer.parseInt(riskConfigMap.get(ComConstants.RiskFactor.M.name()))
                        );
                    }
                }
            }

            return INSTANCE;
        }
    }
}
