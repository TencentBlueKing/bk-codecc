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

import static com.tencent.devops.common.constant.ComConstants.StaticticItem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.MongoCommandException;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.dao.redis.StatisticDao;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.pojo.statistic.CommonDefectStatisticModel;
import com.tencent.bk.codecc.defect.pojo.statistic.DefectStatisticModel;
import com.tencent.bk.codecc.defect.service.FilterPathService;
import com.tencent.bk.codecc.defect.service.IUpdateDefectBizService;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.service.statistic.CommonDefectStatisticServiceImpl;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.DefectBaseVO;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.MarkStatus;
import com.tencent.devops.common.util.FileUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * 更新告警服务实现类
 *
 * @version V1.0
 * @date 2019/11/21
 */
@Service
@Slf4j
public class UpdateDefectBizServiceImpl implements IUpdateDefectBizService {

    private static final int BATCH_SIZE = 1000;
    @Autowired
    protected DefectRepository defectRepository;
    @Autowired
    protected DefectDao defectDao;
    @Autowired
    protected TaskLogRepository taskLogRepository;
    @Autowired
    protected StatisticDao statisticDao;
    @Autowired
    protected ScmFileInfoService scmFileInfoService;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private FilterPathService filterPathService;
    @Autowired
    private CommonDefectStatisticServiceImpl commonDefectStatisticService;
    @Autowired
    private CommonFilterPathBizServiceImpl commonFilterPathBizService;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private DefectConverter defectConverter;

    /**
     * 更新告警状态
     * 1.在codecc上new（即不被忽略、不被路径或规则屏蔽），在platform上已修复的告警，要标志为已修复
     * 2.在codecc上已修复，在platform上未修复的告警，要标志为未修复
     *
     * @param updateDefectVO
     * @return
     */
    @Override
    public void updateDefectStatus(UpdateDefectVO updateDefectVO) {
        log.info("begin to update defect status,taskId:{},toolName:{},buildId:{},size:{}", updateDefectVO.getTaskId(),
                updateDefectVO.getToolName(), updateDefectVO.getBuildId(), updateDefectVO.getDefectList().size());

        boolean migrationSuccessful = Boolean.TRUE.equals(updateDefectVO.getMigrationSuccessful());
        Long taskId = updateDefectVO.getTaskId();
        String toolName = updateDefectVO.getToolName();
        List<DefectDetailVO> defectList = updateDefectVO.getDefectList();
        String buildId = updateDefectVO.getBuildId();
        Map<String, Integer> checkerCountMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(defectList)) {
            TaskLogEntity taskLogEntity =
                    taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
            String buildNum = taskLogEntity.getBuildNum();

            TaskDetailVO taskDetailVO = thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId);

            Map<String, DefectDetailVO> defectDetailVOMap = defectList.stream()
                    .collect(Collectors.toMap(DefectBaseVO::getId, Function.identity(), (k, v) -> v));

            // 分批查询需要更新的告警
            Set<String> idSet = defectDetailVOMap.keySet();
            List<CommonDefectEntity> defectEntityList = getCommonDefectList(
                    idSet,
                    taskId,
                    toolName,
                    migrationSuccessful
            );
            Set<String> filterPathSet = filterPathService.getFilterPaths(taskDetailVO, toolName);
            Set<String> whitePaths = new HashSet<>();
            if (CollectionUtils.isNotEmpty(taskDetailVO.getWhitePaths())) {
                whitePaths.addAll(taskDetailVO.getWhitePaths());
            }
            ArrayList<CommonDefectEntity> toStatisticDefectList = new ArrayList<>();

            // 统计新增、关闭、遗留
            int closeCount = 0;
            int existCount = 0;
            int fixedCount = 0;
            int excludeCount = 0;
            List<CommonDefectEntity> needUpdateDefectList = new ArrayList<>();
            long currTime = System.currentTimeMillis();
            // 获取文件作者信息
            Map<String, ScmBlameVO> fileChangeRecordsMap = scmFileInfoService.loadAuthorInfoMap(
                    taskId,
                    updateDefectVO.getStreamName(),
                    toolName,
                    buildId);
            for (CommonDefectEntity commonDefectEntity : defectEntityList) {
                fillRelPath(commonDefectEntity, fileChangeRecordsMap);
                String id = commonDefectEntity.getId();
                int status = commonDefectEntity.getStatus();

                int codeccStatus = status;
                DefectDetailVO platformDefect = defectDetailVOMap.get(id);
                Integer platformStatus = platformDefect.getStatus();

                // 1.在codecc上new（即不被忽略、不被路径或规则屏蔽），在platform上已修复的告警，要标志为已修复
                if (codeccStatus == ComConstants.DefectStatus.NEW.value()
                        && platformStatus == ComConstants.DefectStatus.FIXED.value()) {
                    status = codeccStatus | ComConstants.DefectStatus.FIXED.value();
                    commonDefectEntity.setFixedTime(currTime);
                    commonDefectEntity.setFixedBuildNumber(buildNum);
                    closeCount++;
                    fixedCount++;
                } else if ((codeccStatus & ComConstants.DefectStatus.FIXED.value()) > 0
                        && (platformStatus & ComConstants.DefectStatus.FIXED.value()) == 0) {
                    status = codeccStatus - ComConstants.DefectStatus.FIXED.value();
                    commonDefectEntity.setFixedBuildNumber(null);
                }

                // 如果经过上面判断后，告警是未修复的，需要判断告警是否被路径屏蔽
                if ((status & ComConstants.DefectStatus.FIXED.value()) == 0) {
                    commonDefectEntity.setStatus(status);
                    // 注：路径屏蔽逻辑里面会修改entity的值
                    if (commonFilterPathBizService.processBiz(
                            new FilterPathInputVO<>(
                                    whitePaths,
                                    filterPathSet,
                                    commonDefectEntity,
                                    currTime,
                                    toolName)).getData()
                            && (commonDefectEntity.getStatus() & ComConstants.DefectStatus.PATH_MASK.value()) > 0) {
                        closeCount++;
                        excludeCount++;
                    }
                    status = commonDefectEntity.getStatus();
                }

                if (status != codeccStatus
                        || (StringUtils.isNotEmpty(platformDefect.getFilePath())
                        && !platformDefect.getFilePath().equals(commonDefectEntity.getFilePath()))
                ) {
                    commonDefectEntity.setStatus(status);
                    String fileName = StringUtils.isNotBlank(commonDefectEntity.getFileName())
                            ? commonDefectEntity.getFileName() :
                            FileUtils.getFileNameByPath(commonDefectEntity.getFilePath());
                    commonDefectEntity.setFileName(fileName);
                    commonDefectEntity.setFilePath(platformDefect.getFilePath());
                    needUpdateDefectList.add(commonDefectEntity);
                }

                if (status == ComConstants.DefectStatus.NEW.value()) {
                    toStatisticDefectList.add(commonDefectEntity);
                    existCount++;
                }
                // 获取规则数据
                Integer count = checkerCountMap.get(commonDefectEntity.getChecker());
                if (count == null) {
                    count = 0;
                }
                checkerCountMap.put(commonDefectEntity.getChecker(), ++count);
            }

            CommonDefectStatisticModel statistic = commonDefectStatisticService.statistic(
                    new DefectStatisticModel<>(
                            taskDetailVO,
                            toolName,
                            0,
                            buildId,
                            null,
                            toStatisticDefectList,
                            null,
                            null,
                            Lists.newArrayList(),
                            false
                    )
            );

            List<Pair<StaticticItem, Integer>> defectCountList = Arrays.asList(
                    Pair.of(StaticticItem.EXIST, existCount),
                    Pair.of(StaticticItem.CLOSE, closeCount),
                    Pair.of(StaticticItem.FIXED, fixedCount),
                    Pair.of(StaticticItem.EXCLUDE, excludeCount));
            statisticDao.increaseDefectCountByStatusBatch(taskId, toolName, buildNum, defectCountList);
            commonDefectStatisticService.saveStatisticToRedis(statistic, taskId, toolName, buildNum);
            // 写入规则统计数据
            statisticDao.increaseDefectCheckerCountBatch(taskId, toolName, buildNum, checkerCountMap);

            if (migrationSuccessful) {
                List<LintDefectV2Entity> lintDefectList = defectConverter.commonToLint(needUpdateDefectList);
                lintDefectV2Dao.batchUpdateDefectStatusFixedBit(taskId, lintDefectList);
            } else {
                defectDao.batchUpdateDefectStatusFixedBit(taskId, needUpdateDefectList);
            }
        }

        log.info("update defectStatus success.");
    }

    @Override
    public void updateDefects(UpdateDefectVO updateDefectVO) {
        log.info("begin to update defects, taskId:{}, toolName:{}, buildId:{}, size:{}", updateDefectVO.getTaskId(),
                updateDefectVO.getToolName(), updateDefectVO.getBuildId(), updateDefectVO.getDefectList().size());

        String defectListJson = JsonUtil.INSTANCE.toJson(updateDefectVO.getDefectList());
        List<CommonDefectEntity> defectList =
                JsonUtil.INSTANCE.to(defectListJson, new TypeReference<List<CommonDefectEntity>>() {
                });
        boolean migrationSuccessful = Boolean.TRUE.equals(updateDefectVO.getMigrationSuccessful());
        Long taskId = updateDefectVO.getTaskId();
        String toolName = updateDefectVO.getToolName();
        //获取 Mark Info
        Set<String> ids = CollectionUtils.isEmpty(defectList) ? Collections.emptySet()
                : defectList.stream().map(CommonDefectEntity::getId).collect(Collectors.toSet());
        List<LintDefectV2Entity> defectMarkInfoList = CollectionUtils.isEmpty(ids) ? Collections.emptyList()
                : lintDefectV2Dao.getDefectWithMarkInfo(taskId, toolName, ids);
        Map<String, Integer> defectMarkInfoMap = CollectionUtils.isEmpty(defectMarkInfoList) ? Collections.emptyMap()
                : defectMarkInfoList.stream()
                        .collect(Collectors.toMap(LintDefectV2Entity::getId, LintDefectV2Entity::getMark));
        try {
            if (CollectionUtils.isNotEmpty(defectList)) {
                for (CommonDefectEntity entity : defectList) {
                    String fileName = StringUtils.isNotBlank(entity.getFileName())
                            ? entity.getFileName() :
                            FileUtils.getFileNameByPath(entity.getFilePath());
                    entity.setFileName(fileName);
                    // 设置MarkNotFixed标识
                    if (defectMarkInfoMap.containsKey(entity.getId())
                            && defectMarkInfoMap.get(entity.getId()).equals(MarkStatus.MARKED.value())) {
                        entity.setMarkButNoFixed(true);
                    }
                }
            }

            if (migrationSuccessful) {
                List<LintDefectV2Entity> lintDefectList = defectConverter.commonToLint(defectList);
                lintDefectV2Dao.batchUpdateDefectDetail(taskId, toolName, lintDefectList);
            } else {
                defectDao.batchUpdateDefectDetail(taskId, toolName, defectList);
            }
        } catch (MongoCommandException e) {
            log.error("fail to batch update defects, taskId:{}, toolName:{}, buildId:{}",
                    updateDefectVO.getTaskId(), updateDefectVO.getToolName(), updateDefectVO.getBuildId(), e);

            log.info("update defects one by one, taskId:{}, toolName:{}, buildId:{}",
                    updateDefectVO.getTaskId(), updateDefectVO.getToolName(), updateDefectVO.getBuildId());

            if (migrationSuccessful) {
                List<LintDefectV2Entity> lintDefectList = defectConverter.commonToLint(defectList);
                lintDefectV2Dao.updateDefectDetailOneByOne(taskId, toolName, lintDefectList);
            } else {
                defectDao.updateDefectDetailOneByOne(taskId, toolName, defectList);
            }
        }

        log.info("success update defects, taskId:{}, toolName:{}, buildId:{}, size:{}", updateDefectVO.getTaskId(),
                updateDefectVO.getToolName(), updateDefectVO.getBuildId(), defectList.size());
    }

    private void fillRelPath(CommonDefectEntity commonDefectEntity, Map<String, ScmBlameVO> fileChangeRecordsMap) {
        if (MapUtils.isNotEmpty(fileChangeRecordsMap)
                && fileChangeRecordsMap.get(commonDefectEntity.getFilePath()) != null) {
            ScmBlameVO fileLineAuthorInfo =
                    fileChangeRecordsMap.get(commonDefectEntity.getFilePath());
            commonDefectEntity.setRelPath(fileLineAuthorInfo.getFileRelPath());
        }
    }

    /**
     * 分页获取"Common类"告警
     *
     * @param idSet
     * @param taskId
     * @param toolName
     * @param migrationSuccessful 数据迁移是否成功
     * @return
     */
    private List<CommonDefectEntity> getCommonDefectList(
            Set<String> idSet,
            long taskId,
            String toolName,
            boolean migrationSuccessful
    ) {
        if (CollectionUtils.isEmpty(idSet)) {
            return Lists.newArrayList();
        }

        // 若数据迁移成功，则从lint表取出数据再转换为common实体，以mock回原来的代码逻辑
        if (migrationSuccessful) {
            List<LintDefectV2Entity> lintDefectList = Lists.newArrayList();

            if (idSet.size() > BATCH_SIZE) {
                List<List<String>> partitionList = Lists.partition(Lists.newArrayList(idSet), BATCH_SIZE);
                for (List<String> onePartition : partitionList) {
                    lintDefectList.addAll(
                            lintDefectV2Repository.findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(
                                    taskId,
                                    toolName,
                                    Sets.newHashSet(onePartition)
                            )
                    );
                }
            } else {
                lintDefectList.addAll(
                        lintDefectV2Repository.findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(
                                taskId,
                                toolName,
                                idSet
                        )
                );
            }

            return defectConverter.lintToCommon(lintDefectList);
        } else {
            List<CommonDefectEntity> commonDefectList = Lists.newArrayList();

            if (idSet.size() > BATCH_SIZE) {
                List<List<String>> partitionList = Lists.partition(Lists.newArrayList(idSet), BATCH_SIZE);
                partitionList.forEach(idList -> commonDefectList.addAll(
                                defectRepository.findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(
                                        taskId,
                                        toolName,
                                        Sets.newHashSet(idList)
                                )
                        )
                );
            } else {
                commonDefectList.addAll(
                        defectRepository.findStatusAndAuthorAndSeverityByTaskIdAndToolNameAndIdIn(
                                taskId,
                                toolName,
                                idSet
                        )
                );
            }

            return commonDefectList;
        }
    }
}
