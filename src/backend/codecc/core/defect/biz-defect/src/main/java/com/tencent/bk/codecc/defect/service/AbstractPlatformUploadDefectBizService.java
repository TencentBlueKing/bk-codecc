/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.OpenCheckerVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.FileUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jodd.cli.Cli;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 告警上报抽象类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Slf4j
public abstract class AbstractPlatformUploadDefectBizService extends AbstractUploadDefectBizService
{
    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private TransferAuthorRepository transferAuthorRepository;
    @Autowired
    protected CheckerService checkerService;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private FilterPathService filterPathService;
    @Autowired
    private IV3CheckerSetBizService iv3CheckerSetBizService;
    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ReallocateDefectAuthorService reallocateDefectAuthorService;

    @Override
    public Result processBiz(UploadDefectVO uploadDefectVO)
    {
        String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
        List<CommonDefectEntity> defectList = JsonUtil.INSTANCE.to(
                defectListJson, new TypeReference<List<CommonDefectEntity>>()
        {
        });
        uploadDefectVO.setDefectsCompress(null);
        if (CollectionUtils.isEmpty(defectList))
        {
            log.error("defect list is empty.");
            return new Result(CommonMessageCode.SUCCESS, "defect list is empty.");
        }
        //设置FileName
        for (CommonDefectEntity entity : defectList) {
            String fileName = StringUtils.isNotBlank(entity.getFileName())
                    ? entity.getFileName() :
                    FileUtils.getFileNameByPath(entity.getFilePath());
            entity.setFileName(fileName);
        }

        long taskId = uploadDefectVO.getTaskId();
        String toolName = uploadDefectVO.getToolName();
        String buildId = uploadDefectVO.getBuildId();

        TaskLogEntity taskLogEntity = taskLogRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        String buildNum = taskLogEntity.getBuildNum();

        TaskDetailVO taskDetailVO = thirdPartySystemCaller.getTaskInfoWithoutToolsByTaskId(taskId);
        Set<String> filterPathSet = filterPathService.getFilterPaths(taskDetailVO, toolName);
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskId(taskId);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        Set<String> whitePaths = new HashSet<>();
        if (CollectionUtils.isNotEmpty(taskDetailVO.getWhitePaths())) {
            whitePaths.addAll(taskDetailVO.getWhitePaths());
        }

        log.info("begin to save defect");
        String redisKeyForAuthorReassign = String.format(
                RedisKeyConstants.IS_REALLOCATE + ":%d:%s", taskId, toolName);
        boolean isReallocate = false;
        // 查询上报工具是否处理人重新分配
        try {
            isReallocate = reallocateDefectAuthorService.isReallocate(taskId, toolName);
            if (isReallocate) {
                // 如果上报工具配置了重新分配配置，则加入缓存，op后台在修改时判断
                redisTemplate.opsForValue().setIfAbsent(
                        redisKeyForAuthorReassign, "reallocating", 1, TimeUnit.HOURS);
            }
            saveAndStatisticDefect(uploadDefectVO,
                    defectList,
                    taskDetailVO,
                    filterPathSet,
                    whitePaths,
                    transferAuthorList,
                    buildNum,
                    isReallocate);

            // 更新工具是否重新分配状态
            if (isReallocate) {
                // 更新重新分配状态为已分配
                reallocateDefectAuthorService.updateCurrentStatus(taskId, toolName);
            }
        } finally {
            if (isReallocate) {
                redisTemplate.delete(redisKeyForAuthorReassign);
            }
        }

        return new Result(CommonMessageCode.SUCCESS, "upload defect ok");
    }

    /**
     * 保存和统计告警数量
     *
     * @param uploadDefectVO
     * @param defectList
     * @param taskDetailVO
     * @param filterPathSet
     * @param whitePaths
     * @param transferAuthorList
     * @param buildNum
     */
    public abstract void saveAndStatisticDefect(
            UploadDefectVO uploadDefectVO,
            List<CommonDefectEntity> defectList,
            TaskDetailVO taskDetailVO,
            Set<String> filterPathSet,
            Set<String> whitePaths,
            List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList,
            String buildNum,
            boolean isReallocate);

    /**
     * 获取打开规则映射
     * @param taskId
     * @param toolName
     * @param taskDetailVO
     * @return
     */
    protected Map<String, OpenCheckerVO> getOpenCheckerMap(Long taskId, String toolName, TaskDetailVO taskDetailVO)
    {
        //查询打开的规则
        AnalyzeConfigInfoVO analyzeConfigInfoVO = new AnalyzeConfigInfoVO();
        analyzeConfigInfoVO.setTaskId(taskId);
        analyzeConfigInfoVO.setMultiToolType(toolName);
        analyzeConfigInfoVO.setLanguage(taskDetailVO.getCodeLang());
        //pinpoint不需要工具参数
        analyzeConfigInfoVO.setParamJson("");
        AnalyzeConfigInfoVO result = checkerService.getTaskCheckerConfig(analyzeConfigInfoVO);
        List<OpenCheckerVO> openCheckers = result.getOpenCheckers();

        Map<String, OpenCheckerVO> openCheckerMap = Maps.newHashMap();

        if(CollectionUtils.isNotEmpty(openCheckers))
        {
            openCheckerMap = openCheckers.stream().filter(openCheckerVO -> StringUtils.isNotBlank(openCheckerVO.getCheckerName())).
                    collect(Collectors.toMap(OpenCheckerVO::getCheckerName, Function.identity(), (k, v) -> v));
        }
        return openCheckerMap;
    }

    /**
     * 检查告警是否应该被屏蔽，返回true表示要屏蔽，否则不屏蔽
     *
     * @param rule_key
     * @param openCheckers
     * @return
     */
    protected static boolean checkIfMaskByChecker(String rule_key, Map<String, OpenCheckerVO> openCheckers)
    {
        return !openCheckers.containsKey(rule_key);
    }


    protected void refreshDefectInfo(CommonDefectEntity oldDefect, CommonDefectEntity commonDefectEntity) {
        oldDefect.setLineNum(commonDefectEntity.getLineNum());
        oldDefect.setDisplayCategory(commonDefectEntity.getDisplayCategory());
        oldDefect.setDisplayType(commonDefectEntity.getDisplayType());
        String fileName = StringUtils.isNotBlank(commonDefectEntity.getFileName())
                ? commonDefectEntity.getFileName()
                : FileUtils.getFileNameByPath(commonDefectEntity.getFilePath());
        oldDefect.setFileName(fileName);
        oldDefect.setFilePath(commonDefectEntity.getFilePath());
        oldDefect.setPlatformBuildId(commonDefectEntity.getPlatformBuildId());
        oldDefect.setDefectInstances(commonDefectEntity.getDefectInstances());

        // scm带出的新值可能是null
        if (commonDefectEntity.getLineUpdateTime() != null && commonDefectEntity.getLineUpdateTime() > 0) {
            oldDefect.setLineUpdateTime(commonDefectEntity.getLineUpdateTime());
        }
    }

    /**
     * 获取task任务配置的规则，对齐: LintDefectCommitConsumer#uploadDefects
     *
     * @param taskDetailVO
     * @param toolName
     * @return
     */
    protected Set<String> getTaskCheckers(TaskDetailVO taskDetailVO, String toolName) {
        if (taskDetailVO == null || StringUtils.isEmpty(toolName)) {
            return Sets.newHashSet();
        }

        List<CheckerSetVO> checkers = iv3CheckerSetBizService.getTaskCheckerSets(taskDetailVO.getProjectId(),
                taskDetailVO.getTaskId(), toolName, "", true);

        // 配置的规则
        Set<String> checkersOnConfig = checkers.stream()
                .flatMap(x -> x.getCheckerProps().stream())
                .filter(y -> toolName.equalsIgnoreCase(y.getToolName()) && StringUtils.isNotEmpty(y.getCheckerKey()))
                .map(CheckerPropVO::getCheckerKey)
                .map(checkerKey -> checkerKey.contains("-tosa") ? checkerKey.replaceAll("-tosa", "") : checkerKey)
                .collect(Collectors.toSet());

        // 校验规则存在与否
        return checkerRepository.findByToolNameAndCheckerKeyIn(toolName, checkersOnConfig)
                .stream()
                .map(CheckerDetailEntity::getCheckerKey)
                .collect(Collectors.toSet());
    }
}
