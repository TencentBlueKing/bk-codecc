package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoredNegativeDefectDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.defect.IgnoredNegativeDefectEntity;
import com.tencent.bk.codecc.defect.service.IIgnoredNegativeDefectService;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectStatisticVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class IIgnoredNegativeDefectServiceImpl implements IIgnoredNegativeDefectService {

    @Autowired
    IgnoredNegativeDefectDao ignoredNegativeDefectDao;

    @Autowired
    private CheckerRepository checkerRepository;

    private static class Period {
        public long startTime;
        public long endTime;

        Period(long st, long et) {
            this.startTime = st;
            this.endTime = et;
        }
    }

    /**
     * 生成 list 接口和 count 接口通用的过滤条件: 开始时间, 结束时间, checker name
     *
     * @date 2024/3/9
     * @param n
     * @param listNegativeDefectReq    对 checker name 的过滤会 set 到这个 request 中
     * @return Period   最终的过滤条件: 开始时间, 结束时间
     */
    private Period genCommonCriteria(String toolName, Integer n, ListNegativeDefectReqVO listNegativeDefectReq) {
        List<Set<String>> twoCheckerNameSet = new ArrayList<>(Arrays.asList(null, null));
        int pre = 0;
        boolean flag = false;

        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getCheckerNames())) {
            twoCheckerNameSet.set(pre, listNegativeDefectReq.getCheckerNames());
            flag = true;
        }

        // publishers, checkerTags 这些筛选条件转换成 checkerNames 来体现
        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getPublishers())) {
            List<CheckerDetailEntity> checkers = checkerRepository.findByToolNameAndPublisherIn(
                    toolName,
                    listNegativeDefectReq.getPublishers()
            );
            int now = pre ^ 1;
            Set<String> emptySet = new HashSet<>();
            twoCheckerNameSet.set(now, emptySet);
            checkers.forEach(it -> twoCheckerNameSet.get(now).add(it.getCheckerName()));
            if (flag) {
                twoCheckerNameSet.get(now).retainAll(twoCheckerNameSet.get(pre));
            }
            pre = now;
            flag = true;
        }
        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getCheckerTags())) {
            List<CheckerDetailEntity> checkers = checkerRepository.findByToolNameAndCheckerTagIn(
                    toolName,
                    listNegativeDefectReq.getCheckerTags()
            );
            int now = pre ^ 1;
            Set<String> emptySet = new HashSet<>();
            twoCheckerNameSet.set(now, emptySet);
            checkers.forEach(it -> twoCheckerNameSet.get(now).add(it.getCheckerName()));
            if (flag) {
                twoCheckerNameSet.get(now).retainAll(twoCheckerNameSet.get(pre));
            }
            pre = now;
            flag = true;
        }

        if (flag) {
            listNegativeDefectReq.setCheckerNames(twoCheckerNameSet.get(pre));
        }

        long endTime = DateTimeUtils.getTodayZeroMillis() + DateTimeUtils.day2Millis(1);
        long startTime = endTime - DateTimeUtils.day2Millis(n);

        if (StringUtils.isNotBlank(listNegativeDefectReq.getStartDate())
                && StringUtils.isNotBlank(listNegativeDefectReq.getEndDate())) {
            String startTimeS2 = listNegativeDefectReq.getStartDate();
            String endTimeS2 = listNegativeDefectReq.getEndDate();

            // 定义时间格式: 协调世界时 (UTC)
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

            TemporalAccessor temporalAccessor = formatter.parse(startTimeS2);
            Instant instant = Instant.from(temporalAccessor);
            long startTime2 = instant.toEpochMilli();

            temporalAccessor = formatter.parse(endTimeS2);
            instant = Instant.from(temporalAccessor);
            long endTime2 = instant.toEpochMilli();

            if (startTime2 > endTime || startTime > endTime2) {
                startTime = 0;
                endTime = 0;
            } else {
                startTime = Math.max(startTime, startTime2);
                endTime = Math.min(endTime, endTime2);
            }
        }

        return new Period(startTime, endTime);
    }

    private List<IgnoredNegativeDefectVO> genIgnoredNegativeDefectResultVO(
            String toolName,
            List<IgnoredNegativeDefectEntity> entities
    ) {
        List<IgnoredNegativeDefectVO> result = new ArrayList<>();
        Map<String, String> checkerName2Publisher = new HashMap<>();
        entities.forEach(it -> {
            IgnoredNegativeDefectVO defectVO = new IgnoredNegativeDefectVO();
            BeanUtils.copyProperties(it, defectVO);
            if (!checkerName2Publisher.containsKey(defectVO.getChecker())) {
                checkerName2Publisher.put(defectVO.getChecker(), "");
            }
            result.add(defectVO);
        });

        genCheckerName2PublisherMap(toolName, checkerName2Publisher);
        result.forEach(it -> it.setPublisher(checkerName2Publisher.get(it.getChecker())));

        return result;
    }

    private void genCheckerName2PublisherMap(String toolName, Map<String, String> map) {
        List<CheckerDetailEntity> entities = checkerRepository.findByToolNameAndCheckerKeyIn(toolName, map.keySet());
        entities.forEach(it -> map.put(it.getCheckerName(), it.getPublisher()));
    }

    @Override
    public Boolean processNegativeDefect(String entityId, ProcessNegativeDefectReqVO processNegativeDefectReq) {
        if (processNegativeDefectReq.getProcessProgress() == null) {
            log.error("参数配置错误");
            return false;
        }

        if ((processNegativeDefectReq.getProcessProgress().equals(ComConstants.ProcessProgressType.NONEED.value())
                || processNegativeDefectReq.getProcessProgress().equals(ComConstants.ProcessProgressType.OTHER.value()))
                && processNegativeDefectReq.getProcessReasonType() == null) {
            log.error("参数配置错误");
            return false;
        }

        return ignoredNegativeDefectDao.updateProcessProgressByDefectId(entityId, processNegativeDefectReq);
    }

    @Override
    public Long countDefectAfterFilter(String toolName, Integer n, ListNegativeDefectReqVO listNegativeDefectReq) {
        Period period = genCommonCriteria(toolName, n, listNegativeDefectReq);

        log.info("#listDefect: startTime = {}, endTime = {}", period.startTime, period.endTime);
        return ignoredNegativeDefectDao.countEntitiesAfterFilter(
                toolName,
                period.startTime,
                period.endTime,
                listNegativeDefectReq
        );
    }

    @Override
    public List<IgnoredNegativeDefectVO> listDefectAfterFilter(
            String toolName,
            Integer n,
            String lastInd,
            Integer pageSize,
            String orderBy,
            String orderDirection,
            ListNegativeDefectReqVO listNegativeDefectReq
    ) {
        Period period = genCommonCriteria(toolName, n, listNegativeDefectReq);

        log.info("#listDefect: startTime = {}, endTime = {}", period.startTime, period.endTime);
        List<IgnoredNegativeDefectEntity> ignoredNegativeDefectEntities =
                ignoredNegativeDefectDao.queryEntitiesAfterFilter(
                        toolName,
                        period.startTime,
                        period.endTime,
                        lastInd,
                        pageSize,
                        orderBy,
                        orderDirection,
                        listNegativeDefectReq
                );

        return genIgnoredNegativeDefectResultVO(toolName, ignoredNegativeDefectEntities);
    }

    @Override
    public IgnoredNegativeDefectStatisticVO statistic(String toolName, Integer n) {
        long endTime = DateTimeUtils.getTodayZeroMillis() + DateTimeUtils.day2Millis(1);
        long startTime = endTime - DateTimeUtils.day2Millis(n);

        log.info("queryByToolNameAndPeriod({}, {}, {})", toolName, startTime, endTime);

        // 只返回这 2 个字段的数据
        List<String> includeField = Arrays.asList("process_progress", "url");
        List<IgnoredNegativeDefectEntity> ignoredNegativeDefectEntities =
                ignoredNegativeDefectDao.queryByToolNameAndPeriod(toolName, startTime, endTime, includeField);

        IgnoredNegativeDefectStatisticVO result = new IgnoredNegativeDefectStatisticVO();

        if (ignoredNegativeDefectEntities != null && !ignoredNegativeDefectEntities.isEmpty()) {
            long total = ignoredNegativeDefectEntities.size();
            long confirmed = 0;
            long unconfirmed;
            long repos;

            Set<String> repoSet = new HashSet<>();
            for (IgnoredNegativeDefectEntity entity : ignoredNegativeDefectEntities) {
                if (entity.getProcessProgress() > 0) {
                    confirmed++;
                }
                repoSet.add(entity.getUrl());
            }
            unconfirmed = total - confirmed;
            repos = repoSet.size();

            result.setTotal(total);
            result.setConfirmed(confirmed);
            result.setUnconfirmed(unconfirmed);
            result.setRepos(repos);
        }

        return result;
    }
}
