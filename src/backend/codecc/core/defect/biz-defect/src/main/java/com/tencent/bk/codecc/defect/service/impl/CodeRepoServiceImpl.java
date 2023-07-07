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

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoFromAnalyzeLogRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoStatDailyRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeRepoStatDailyDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeRepoStatisticDao;
import com.tencent.bk.codecc.defect.model.CodeRepoFromAnalyzeLogEntity;
import com.tencent.bk.codecc.defect.model.CodeRepoStatDailyEntity;
import com.tencent.bk.codecc.defect.model.CodeRepoStatisticEntity;
import com.tencent.bk.codecc.defect.service.CodeRepoService;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.DAY_THIRTY;

/**
 * 代码库服务逻辑实现
 *
 * @version V1.0
 * @date 2019/12/3
 */
@Slf4j
@Service
public class CodeRepoServiceImpl implements CodeRepoService {
    @Autowired
    private CodeRepoFromAnalyzeLogRepository codeRepoFromAnalyzeLogRepository;
    @Autowired
    private CodeRepoStatisticRepository codeRepoStatisticRepository;
    @Autowired
    private CodeRepoStatDailyRepository codeRepoStatDailyRepository;
    @Autowired
    private CodeRepoStatisticDao codeRepoStatisticDao;
    @Autowired
    private CodeRepoStatDailyDao codeRepoStatDailyDao;
    @Autowired
    private Client client;

    @Override
    public Set<CodeRepoVO> getCodeRepoInfoByTaskId(Long taskId, String buildId) {
        CodeRepoFromAnalyzeLogEntity codeRepoFromAnalyzeLogEntity;
        codeRepoFromAnalyzeLogEntity = codeRepoFromAnalyzeLogRepository.findFirstByTaskId(taskId);
        if (null == codeRepoFromAnalyzeLogEntity) {
            return new HashSet<>();
        }
        Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepoList = codeRepoFromAnalyzeLogEntity.getCodeRepoList();

        Iterator<CodeRepoFromAnalyzeLogEntity.CodeRepo> it = codeRepoList.iterator();
        //取最后一个代码库地址
        if (CollectionUtils.isEmpty(codeRepoList)) {
            return new HashSet<>();
        }
        CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo = codeRepoList.stream().max(Comparator.comparing(
                codeRepo1 -> null == codeRepo1.getCreateDate() ? 0L : codeRepo1.getCreateDate())).get();
        CodeRepoVO codeRepoVO = new CodeRepoVO();
        codeRepoVO.setUrl(codeRepo.getUrl());
        codeRepoVO.setBranch(codeRepo.getBranch());
        codeRepoVO.setVersion(codeRepo.getVersion());
        codeRepoVO.setToolNames(new HashSet<>());
        Set<CodeRepoVO> codeRepos;
        codeRepos = new HashSet<CodeRepoVO>() {{
            add(codeRepoVO);
        }};
        return codeRepos;

    }

    @Override
    public Map<Long, Set<CodeRepoVO>> getCodeRepoListByTaskIds(Set<Long> taskIds) {
        //根据任务id集查询代码仓库信息
        Set<CodeRepoFromAnalyzeLogEntity> codeRepoFromAnalyzeLogEntitySet = codeRepoFromAnalyzeLogRepository.findByTaskIdIn(taskIds);

        if (CollectionUtils.isNotEmpty(codeRepoFromAnalyzeLogEntitySet)) {
            return codeRepoFromAnalyzeLogEntitySet.stream().collect(Collectors.toMap(CodeRepoFromAnalyzeLogEntity::getTaskId,
                    codeRepoFromAnalyzeLogEntity -> codeRepoFromAnalyzeLogEntity.getCodeRepoList().stream().map(codeRepo -> {
                        CodeRepoVO codeRepoVO = new CodeRepoVO();
                        codeRepoVO.setUrl(codeRepo.getUrl());
                        codeRepoVO.setBranch(codeRepo.getBranch());
                        codeRepoVO.setVersion(codeRepo.getVersion());
                        codeRepoVO.setToolNames(new HashSet<>());
                        return codeRepoVO;
                    }).collect(Collectors.toSet()),
                    (k, v) -> v
            ));
        } else {
            return new HashMap<>();
        }
    }


    /**
     * 初始化代码仓库及分支名数据
     *
     * @param reqVO     请求体
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param sortField 排序字段
     * @param sortType  排序类型
     * @return
     */
    @Override
    public Boolean initCodeRepoStatistic(DeptTaskDefectReqVO reqVO, Integer pageNum, Integer pageSize, String sortField,
            String sortType) {
        // 根据来源获取taskId集合
        List<Long> taskIdList =
                client.get(ServiceTaskRestResource.class).queryTaskIdByCreateFrom(reqVO.getCreateFrom()).getData();
        if (CollectionUtils.isEmpty(taskIdList)) {
            log.error("taskId is empty!");
            return null;
        }
        Map<String, Set<CodeRepoFromAnalyzeLogEntity.CodeRepo>> urlMap = Maps.newHashMap();
        int size;
        int pageNumber = pageNum;
        Pageable pageable = PageableUtils.getPageable(pageNumber, pageSize);
        do {
            // 通过taskId集合查询代码库数据
            List<CodeRepoFromAnalyzeLogEntity> codeRepoFromAnalyzeLogEntityList =
                    codeRepoFromAnalyzeLogRepository.findCodeRepoFromAnalyzeLogEntityByTaskIdIn(taskIdList, pageable);

            size = codeRepoFromAnalyzeLogEntityList.size();
            pageNumber = pageNumber + 1;
            for (CodeRepoFromAnalyzeLogEntity codeRepoFromAnalyzeLogEntity : codeRepoFromAnalyzeLogEntityList) {
                if (CollectionUtils.isNotEmpty(codeRepoFromAnalyzeLogEntity.getCodeRepoList())) {
                    List<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepoList =
                            new ArrayList<>(codeRepoFromAnalyzeLogEntity.getCodeRepoList());
                    for (CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo : codeRepoList) {
                        if (codeRepo != null && codeRepo.getUrl().contains("http")
                                && codeRepo.getCreateDate() != null
                                && StringUtils.isNotEmpty(codeRepo.getBranch())) {
                            Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepos =
                                    urlMap.computeIfAbsent(codeRepo.getUrl(), v -> Sets.newHashSet());
                            codeRepos.add(codeRepo);
                        }
                    }
                }
            }
            pageable = PageableUtils.getPageable(pageNumber, pageSize);
        } while (size >= pageSize);

        String dataFromStr = ComConstants.DefectStatType.USER.value();
        if (reqVO.getCreateFrom() != null
                && reqVO.getCreateFrom().iterator().next().equals(ComConstants.DefectStatType.GONGFENG_SCAN.value())) {
            dataFromStr = ComConstants.DefectStatType.GONGFENG_SCAN.value();
        }
        List<CodeRepoStatisticEntity> codeRepoStatisticEntityList = new ArrayList<>();
        for (Map.Entry<String, Set<CodeRepoFromAnalyzeLogEntity.CodeRepo>> entry : urlMap.entrySet()) {
            ArrayList<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepos = new ArrayList<>(entry.getValue());
            codeRepos.sort(Comparator.comparingLong(CodeRepoFromAnalyzeLogEntity.CodeRepo::getCreateDate));
            Long createDate = codeRepos.get(0).getCreateDate();
            for (CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo : codeRepos) {
                CodeRepoStatisticEntity codeRepoStatisticEntity = new CodeRepoStatisticEntity();
                BeanUtils.copyProperties(codeRepo, codeRepoStatisticEntity);
                codeRepoStatisticEntity.setDataFrom(dataFromStr);
                codeRepoStatisticEntity.setUrlFirstScan(createDate);
                codeRepoStatisticEntity.setBranchFirstScan(codeRepo.getCreateDate());
                codeRepoStatisticEntity.setBranchLastScan(codeRepo.getCreateDate());
                codeRepoStatisticEntityList.add(codeRepoStatisticEntity);
            }
        }
        codeRepoStatisticRepository.saveAll(codeRepoStatisticEntityList);
        return true;
    }

    /**
     * 定时任务及初始化 代码库/代码分支数数据
     *
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean initCodeRepoStatTrend(QueryTaskListReqVO reqVO) {
        log.info("op initCodeRepoStatTrend req: {}", reqVO);
        List<String> createFromList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(reqVO.getCreateFrom())) {
            createFromList = reqVO.getCreateFrom();
        } else {
            createFromList.add("user");
            createFromList.add("gongfeng_scan");
        }

        // 获取初始化天数 默认查询30天
        List<String> dates;
        if (reqVO.getInitDay() != null && reqVO.getInitDay() != 0) {
            dates = DateTimeUtils.getBeforeDaily(reqVO.getInitDay());
        } else {
            dates = DateTimeUtils.getBeforeDaily(DAY_THIRTY);
        }
        // 因为定时任务 避免数据重复 不需要初始化当天时间
        dates.remove(dates.size() - 1);
        log.info("initCodeRepoStatTrend, dates:[{}-{}]", dates.get(0), dates.get(dates.size() - 1));

        for (String createFrom : createFromList) {
            boolean isFirstDay = true;
            long yesterdayUrlCount = 0;
            long yesterdayBranchCount = 0;
            List<CodeRepoStatDailyEntity> codeRepoStatDailyEntityList = new ArrayList<>();
            for (String date : dates) {
                long endTime = DateTimeUtils.getTimeStampEnd(date);
                // 获取代码仓库/分支数量
                long urlCount = codeRepoStatisticDao.getUrlCountByEndTimeAndCreateFrom(endTime, createFrom);
                long branchCount = codeRepoStatisticDao.getBranchCount(endTime, createFrom);

                CodeRepoStatDailyEntity codeRepoStatDailyEntity = new CodeRepoStatDailyEntity();
                // 统计日期
                codeRepoStatDailyEntity.setDate(date);
                // 来源
                codeRepoStatDailyEntity.setDataFrom(createFrom);
                // 累计代码仓库数
                codeRepoStatDailyEntity.setCodeRepoCount(urlCount);
                // 累计分支数
                codeRepoStatDailyEntity.setBranchCount(branchCount);

                // 定时任务进入此判断
                if (yesterdayUrlCount == 0) {
                    String yesterdayDate = DateTimeUtils.getDayBeforeByStringDate(date);
                    log.info("initCodeRepoStatTrend, yesterdayDate:[{}]", yesterdayDate);
                    CodeRepoStatDailyEntity codeRepoStatDailyModel =
                            codeRepoStatDailyDao.getUrlCountByYesterdayDate(yesterdayDate, createFrom);
                    if (codeRepoStatDailyModel != null) {
                        yesterdayUrlCount = codeRepoStatDailyModel.getCodeRepoCount();
                        yesterdayBranchCount = codeRepoStatDailyModel.getBranchCount();
                    }
                    isFirstDay = false;
                }
                // 新增代码仓库数
                if (!isFirstDay) {
                    // 新增代码仓库数
                    codeRepoStatDailyEntity.setNewCodeRepoCount(urlCount - yesterdayUrlCount);
                    // 新增分支数
                    codeRepoStatDailyEntity.setNewBranchCount(branchCount - yesterdayBranchCount);
                }
                codeRepoStatDailyEntityList.add(codeRepoStatDailyEntity);
                // 保存当天的值
                yesterdayUrlCount = urlCount;
                yesterdayBranchCount = branchCount;
                isFirstDay = false;
            }
            log.info("initCodeRepoStatistic, codeRepoStatDailyEntityList.size:[{}]",
                    codeRepoStatDailyEntityList.size());
            codeRepoStatDailyRepository.saveAll(codeRepoStatDailyEntityList);
        }
        return true;
    }

    /**
     * 修复代码库分支总表数据
     */
    @Override
    public Boolean codeRepoStatisticFixed(DeptTaskDefectReqVO reqVO) {
        log.info("codeRepoStatisticFixed reqVO: {}", reqVO);

        // 根据来源获取taskId集合
        List<Long> taskIdList =
                client.get(ServiceTaskRestResource.class).queryTaskIdByCreateFrom(reqVO.getCreateFrom()).getData();
        if (CollectionUtils.isEmpty(taskIdList)) {
            log.warn("dataFrom task id list is empty: {}", reqVO.getCreateFrom());
            return false;
        }

        long timeStampStart = DateTimeUtils.getTimeStampStart(reqVO.getStartDate());
        long timeStampEnd = DateTimeUtils.getTimeStampEnd(reqVO.getEndDate());

        List<CodeRepoFromAnalyzeLogEntity> codeRepoFromAnalyzeLogEntities =
                codeRepoStatisticDao.findByTaskId(taskIdList, timeStampStart, timeStampEnd);
        log.info("codeRepoFromAnalyzeLogEntities size: {}", codeRepoFromAnalyzeLogEntities.size());

        Map<String, Set<CodeRepoFromAnalyzeLogEntity.CodeRepo>> urlMap = Maps.newHashMap();
        Map<String, Map<String, Set<Long>>> urlTaskIdMap = Maps.newHashMap();

        for (CodeRepoFromAnalyzeLogEntity entity : codeRepoFromAnalyzeLogEntities) {

            Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepoSet = entity.getCodeRepoList();
            if (codeRepoSet == null) {
                continue;
            }
            for (CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo : codeRepoSet) {
                if (codeRepo == null || StringUtils.isEmpty(codeRepo.getUrl())
                        || StringUtils.isEmpty(codeRepo.getBranch())) {
                    continue;
                }
                Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepos =
                        urlMap.computeIfAbsent(codeRepo.getUrl(), v -> Sets.newHashSet());
                codeRepos.add(codeRepo);

                Map<String, Set<Long>> branchTaskIdMap =
                        urlTaskIdMap.computeIfAbsent(codeRepo.getUrl(), k -> Maps.newHashMap());
                Set<Long> taskIdSet = branchTaskIdMap.computeIfAbsent(codeRepo.getBranch(), k -> Sets.newHashSet());
                taskIdSet.add(entity.getTaskId());
            }
        }

        String dataFromStr = ComConstants.DefectStatType.USER.value();
        if (reqVO.getCreateFrom() != null
                && reqVO.getCreateFrom().iterator().next().equals(ComConstants.DefectStatType.GONGFENG_SCAN.value())) {
            dataFromStr = ComConstants.DefectStatType.GONGFENG_SCAN.value();
        }

        List<CodeRepoStatisticEntity> codeRepoStatisticEntityList = Lists.newArrayList();
        for (Map.Entry<String, Set<CodeRepoFromAnalyzeLogEntity.CodeRepo>> entry : urlMap.entrySet()) {
            Set<CodeRepoFromAnalyzeLogEntity.CodeRepo> codeRepos = entry.getValue();

            Map<String, Set<Long>> branchTaskIdMap = urlTaskIdMap.get(entry.getKey());
            for (CodeRepoFromAnalyzeLogEntity.CodeRepo codeRepo : codeRepos) {

                // 如果已存在则忽略
                long count = codeRepoStatisticRepository
                        .countByDataFromAndUrlAndBranch(dataFromStr, entry.getKey(), codeRepo.getBranch());
                if (count > 0) {
                    continue;
                }

                CodeRepoStatisticEntity codeRepoStatisticEntity = new CodeRepoStatisticEntity();
                Long createDate = codeRepo.getCreateDate();
                codeRepoStatisticEntity.setUrl(entry.getKey());
                codeRepoStatisticEntity.setBranch(codeRepo.getBranch());
                codeRepoStatisticEntity.setDataFrom(dataFromStr);
                codeRepoStatisticEntity.setUrlFirstScan(createDate);
                codeRepoStatisticEntity.setBranchFirstScan(createDate);
                codeRepoStatisticEntity.setBranchLastScan(createDate);
                codeRepoStatisticEntity
                        .setTaskIdSet(branchTaskIdMap.getOrDefault(codeRepo.getBranch(), Sets.newHashSet()));

                codeRepoStatisticEntityList.add(codeRepoStatisticEntity);
            }
        }

        log.info("codeRepoStatisticFixed new add count: {}", codeRepoStatisticEntityList.size());
        codeRepoStatisticRepository.saveAll(codeRepoStatisticEntityList);
        log.info("codeRepoStatisticFixed finish");
        return true;
    }

}
