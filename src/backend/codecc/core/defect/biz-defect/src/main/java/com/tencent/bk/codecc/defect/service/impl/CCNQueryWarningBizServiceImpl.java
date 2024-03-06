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

package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.bk.codecc.defect.utils.CCNUtils.fillingRiskFactor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.QueryWarningLogicComponent;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.TaskPersonalStatisticDao;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.FileContentQueryParams;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CCNDefectVO;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.DefectFileContentSegmentQueryRspVO;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.PathUtils;
import java.text.Collator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * 圈复杂度告警管理服务实现
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
@Service("CCNQueryWarningBizService")
public class CCNQueryWarningBizServiceImpl extends AbstractQueryWarningBizService {

    private static Logger logger = LoggerFactory.getLogger(CCNQueryWarningBizServiceImpl.class);
    @Autowired
    CheckerService checkerService;
    @Autowired
    private Client client;
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;
    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private QueryWarningLogicComponent queryWarningLogicComponent;
    @Autowired
    private TaskPersonalStatisticDao taskPersonalStatisticDao;

    @Override
    public Long countNewDefectFile(CountDefectFileRequest request) {
        List<Integer> severityList = request.getSeverityList();
        Set<Map.Entry<Integer, Integer>> ccnThresholdSet = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(severityList)) {
            for (Integer intSeverity : severityList) {
                ComConstants.RiskFactor riskFactor = ComConstants.RiskFactor.get(intSeverity);
                ccnThresholdSet.add(thirdPartySystemCaller.getCCNRiskFactorConfig(riskFactor));
            }
        }

        return ccnDefectDao.countFileByCondition(
                request.getTaskId(),
                Sets.newHashSet(DefectStatus.NEW.value()),
                request.getAuthor(),
                ccnThresholdSet
        );
    }

    @Override
    public Object pageInit(String projectId, DefectQueryReqVO request) {
        CCNDefectQueryRspVO response = new CCNDefectQueryRspVO();
        // 跨任务查询时，不执行聚合统计
        if (Boolean.TRUE.equals(request.getMultiTaskQuery())) {
            return response;
        }

        String buildId = request.getBuildId();
        List<Long> taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(
                request.getTaskIdList(),
                request.getProjectId(),
                request.getUserId()
        );
        List<String> toolNameList = Lists.newArrayList(Tool.CCN.name());
        List<String> dimensionList = Lists.newArrayList(Tool.CCN.name());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                toolNameList,
                dimensionList,
                taskIdList,
                buildId
        );
        // 多任务维度，有些任务可能曾经开启过圈复杂度，但现在已经停用了
        taskIdList = Lists.newArrayList(taskToolMap.keySet());

        String type = request.getStatisticType();
        String author = request.getAuthor();
        Set<String> fileList = request.getFileList();
        boolean isSnapshotQuery = StringUtils.isNotEmpty(buildId);
        Set<String> defectIds = isSnapshotQuery
                ? getDefectIdsByBuildId(taskIdList.get(0), Lists.newArrayList(Tool.CCN.name()), buildId)
                : Sets.newHashSet();
        String startCreateTimeStr = request.getStartCreateTime();
        String endCreateTimeStr = request.getEndCreateTime();

        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(type)) {
            statisticByStatus(
                    taskIdList, author, fileList,
                    defectIds, isSnapshotQuery,
                    startCreateTimeStr, endCreateTimeStr,
                    response
            );
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(type)) {
            Set<Integer> statusFilters = getStatusFilter(request.getStatus());
            statisticBySeverity(
                    taskIdList, author, statusFilters, fileList,
                    defectIds, isSnapshotQuery,
                    startCreateTimeStr, endCreateTimeStr,
                    response
            );
        }

        return response;
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId, DefectQueryReqVO request,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    ) {
        return processQueryWarningRequestCore(request, pageNum, pageSize, sortField, sortType);
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
            String projectId, Long taskId, String userId,
            CommonDefectDetailQueryReqVO request, String sortField, Sort.Direction sortType
    ) {
        CCNDefectDetailQueryRspVO ccnDefectQueryRspVO = new CCNDefectDetailQueryRspVO();

        // 查询告警信息
        CCNDefectEntity ccnDefectEntity = ccnDefectRepository.findFirstByEntityId(request.getEntityId());
        // 前端不强制传入taskId
        // NOCC:IP-PARAMETER-IS-DEAD-BUT-OVERWRITTEN(设计如此:)
        taskId = ccnDefectEntity.getTaskId();

        String buildId = request.getBuildId();
        if (StringUtils.isNotBlank(buildId)) {
            List<CCNDefectEntity> snapshotList =
                    queryWarningLogicComponent.postHandleCCNDefect(Lists.newArrayList(ccnDefectEntity), buildId);

            if (CollectionUtils.isNotEmpty(snapshotList)) {
                ccnDefectEntity = snapshotList.get(0);
            }

            setDefectStatusOnLastBuild(taskId, buildId, ccnDefectEntity.getEntityId(), ccnDefectQueryRspVO);
        } else {
            buildId = ccnDefectEntity.getBuildId();
        }

        // 获取风险系数值
        Map<String, String> riskConfMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        fillingRiskFactor(ccnDefectEntity, riskConfMap);

        CCNDefectVO ccnDefectVO = new CCNDefectVO();

        BeanUtils.copyProperties(ccnDefectEntity, ccnDefectVO);
        BeanUtils.copyProperties(ccnDefectEntity, ccnDefectQueryRspVO); // todo delete
        ccnDefectQueryRspVO.setDefectVO(ccnDefectVO);

        // 校验传入的路径是否合法（路径是否是告警对应的文件）
        verifyFilePathIsValid(request.getFilePath(), ccnDefectEntity.getFilePath());

        // 如果没有指定 buildId, 则默认取最新的 buildId
        if (StringUtils.isBlank(buildId)) {
            buildId = getLastestBuildIdByTaskIdAndToolName(taskId, request.getToolName());
        }

        //根据文件路径从分析集群获取文件内容
        FileContentQueryParams queryParams = FileContentQueryParams.queryParams(taskId, "", userId,
                ccnDefectEntity.getUrl(), ccnDefectEntity.getRepoId(), ccnDefectEntity.getRelPath(),
                ccnDefectEntity.getFilePath(), ccnDefectEntity.getRevision(), ccnDefectEntity.getBranch(),
                ccnDefectEntity.getSubModule(), buildId
        );
        String content = getFileContent(queryParams);
        content = trimCodeSegment(content, ccnDefectEntity.getStartLines(), ccnDefectEntity.getEndLines(),
                ccnDefectQueryRspVO);

        //设置代码评论
        if (null != ccnDefectEntity.getCodeComment() &&
                CollectionUtils.isNotEmpty(ccnDefectEntity.getCodeComment().getCommentList())) {
            CodeCommentVO codeCommentVO = convertCodeComment(ccnDefectEntity.getCodeComment());
            ccnDefectVO.setCodeComment(codeCommentVO);
            ccnDefectQueryRspVO.setCodeComment(codeCommentVO); // todo delete
        }

        //获取文件的相对路径
        String relativePath = PathUtils.getRelativePath(ccnDefectEntity.getUrl(), ccnDefectEntity.getRelPath());
        ccnDefectQueryRspVO.setRelativePath(relativePath);

        String filePath = ccnDefectEntity.getFilePath();

        //获取文件的url
        String url = PathUtils.getFileUrl(ccnDefectEntity.getUrl(), ccnDefectEntity.getBranch(),
                ccnDefectEntity.getRelPath());
        ccnDefectQueryRspVO.setFilePath(StringUtils.isEmpty(url) ? filePath : url);
        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\");
        }
        ccnDefectQueryRspVO.setFileName(filePath.substring(fileNameIndex + 1));
        ccnDefectQueryRspVO.setFileContent(content);
        ccnDefectQueryRspVO.setRevision(ccnDefectEntity.getRevision());

        return ccnDefectQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryDefectDetailWithoutFileContent(Long taskId, String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType) {
        return new CommonDefectDetailQueryRspVO();
    }

    @Override
    public DefectFileContentSegmentQueryRspVO processQueryDefectFileContentSegment(String projectId, String userId,
            QueryDefectFileContentSegmentReqVO request) {
        return new DefectFileContentSegmentQueryRspVO();
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            List<Long> taskIdList,
            List<String> toolNameList,
            List<String> dimensionList,
            Set<String> statusSet,
            String checkerSet,
            String buildId,
            String projectId,
            boolean isMultiTaskQuery
    ) {
        taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(taskIdList, projectId, userId);
        if (taskIdList.size() > 1 && StringUtils.isNotEmpty(buildId)) {
            throw new IllegalArgumentException("build id must be empty when task list size more than 1");
        }

        Set<String> authorSet = new TreeSet<>();
        Set<String> defectPaths = new TreeSet<>();
        String toolName = Tool.CCN.name();

        if (isMultiTaskQuery) {
            authorSet.addAll(taskPersonalStatisticDao.getCCNAuthorSet(Sets.newHashSet(taskIdList)));
        } else {
            Long taskId = taskIdList.get(0);
            List<CCNDefectEntity> ccnDefectEntityList = null;

            if (StringUtils.isEmpty(buildId)) {
                ccnDefectEntityList = ccnDefectRepository.findByTaskIdAndStatus(taskId, DefectStatus.NEW.value());
            } else {
                Set<String> defectIdSet = getDefectIdsByBuildId(taskId, Lists.newArrayList(toolName), buildId);
                if (CollectionUtils.isEmpty(defectIdSet)) {
                    ccnDefectEntityList = Lists.newArrayList();
                } else {
                    // 快照查补偿处理
                    Set<Integer> newAndFixedStatusSet = Sets.newHashSet(
                            DefectStatus.NEW.value(),
                            DefectStatus.NEW.value() | DefectStatus.FIXED.value()
                    );

                    ccnDefectEntityList =
                            ccnDefectRepository.findByEntityIdInAndStatusIn(defectIdSet, newAndFixedStatusSet);
                }
            }

            for (CCNDefectEntity defect : ccnDefectEntityList) {
                if (StringUtils.isNotEmpty(defect.getAuthor())) {
                    authorSet.add(defect.getAuthor());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(defect.getUrl(), defect.getRelPath());
                if (StringUtils.isNotBlank(relativePath)) {
                    defectPaths.add(relativePath);
                } else {
                    defectPaths.add(defect.getFilePath());
                }
            }
        }

        List<String> sortedAuthors = Lists.newArrayList(authorSet);
        Collections.sort(sortedAuthors, Collator.getInstance(Locale.SIMPLIFIED_CHINESE));

        QueryWarningPageInitRspVO response = new QueryWarningPageInitRspVO();
        response.setAuthorList(sortedAuthors);

        // 跨任务暂不生成文件路径树
        if (CollectionUtils.isNotEmpty(defectPaths)) {
            // 处理文件树
            TreeService treeService = treeServiceBizServiceFactory.createBizService(
                    toolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
            TreeNodeVO treeNode = treeService.getTreeNode(taskIdList.get(0), defectPaths);
            response.setFilePathTree(treeNode);
        }

        return response;
    }


    @Override
    @Deprecated
    public Set<String> filterDefectByCondition(long taskId, List<?> defectList,
            Set<String> allChecker,
            DefectQueryReqVO queryWarningReq,
            CommonDefectQueryRspVO defectQueryRspVO,
            List<String> toolNameSet) {
        Set<String> severity = queryWarningReq.getSeverity();
        String buildId = queryWarningReq.getBuildId();

        Set<String> condStatusList = queryWarningReq.getStatus();
        Set<Integer> ignoreReasonTypes = queryWarningReq.getIgnoreReasonTypes();
        boolean condIgnoreTypeNotEmpty = CollectionUtils.isNotEmpty(ignoreReasonTypes);

        Long startTime = StringUtils.isBlank(queryWarningReq.getStartCreateTime()) ? null :
                DateTimeUtils.convertStringDateToLongTime(queryWarningReq.getStartCreateTime(),
                        DateTimeUtils.yyyyMMddFormat);
        Long endTime = StringUtils.isBlank(queryWarningReq.getEndCreateTime()) ? null :
                DateTimeUtils.convertStringDateToLongTime(queryWarningReq.getEndCreateTime(),
                        DateTimeUtils.yyyyMMddFormat);

        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
        }
        if (condStatusList.contains(String.valueOf(DefectStatus.PATH_MASK.value()))) {
            condStatusList.add(String.valueOf(DefectStatus.CHECKER_MASK.value()));
        }

        // 获取风险系数值
        Map<String, String> riskFactorConfMap = thirdPartySystemCaller.getRiskFactorConfig(
                ComConstants.Tool.CCN.name());

        // 是否需要构建号过滤
        boolean needBuildIdFilter = StringUtils.isNotEmpty(buildId);
        Set<String> currentBuildEntityIds =
                getDefectIdsByBuildId(taskId, Lists.newArrayList(ComConstants.Tool.CCN.name()), buildId);

        // 需要统计的数据
        long superHighCount = 0L;
        long highCount = 0L;
        long mediumCount = 0L;
        long lowCount = 0L;
        long totalCheckerCount = 0L;
        long newDefectCount = 0L;
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        long historyDefectCount = 0L;
        long existCount = 0L;
        long fixCount = 0L;
        long ignoreCount = 0L;
        long maskCount = 0L;

        // 过滤计数
        Iterator<CCNDefectEntity> it = (Iterator<CCNDefectEntity>) defectList.iterator();
        while (it.hasNext()) {
            CCNDefectEntity ccnDefectEntity = it.next();

            // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
            boolean notMatchStatus = isNotMatchStatus(condStatusList, ccnDefectEntity.getStatus());

            // 按构建号筛选
            if (needBuildIdFilter && !currentBuildEntityIds.contains(ccnDefectEntity.getEntityId())) {
                it.remove();
                continue;
            }
            // 按忽略类型筛选
            if ((ccnDefectEntity.getStatus() & DefectStatus.IGNORE.value()) > 0
                    && condIgnoreTypeNotEmpty && !ignoreReasonTypes.contains(ccnDefectEntity.getIgnoreReasonType())) {
                it.remove();
                continue;
            }
            //按创建日期过滤
            if (startTime != null && (ccnDefectEntity.getCreateTime() == null
                    || ccnDefectEntity.getCreateTime() < startTime)) {
                it.remove();
                continue;
            }
            if (endTime != null && (ccnDefectEntity.getCreateTime() == null
                    || ccnDefectEntity.getCreateTime() >= endTime)) {
                it.remove();
                continue;
            }

            // 根据状态过滤，注：已修复和其他状态（忽略，路径屏蔽，规则屏蔽）不共存，已忽略状态优先于屏蔽
            int status = ccnDefectEntity.getStatus();
            if (DefectStatus.NEW.value() == status) {
                existCount++;
            } else if ((DefectStatus.FIXED.value() & status) > 0) {
                fixCount++;
            } else if ((DefectStatus.IGNORE.value() & status) > 0) {
                ignoreCount++;
            } else if ((DefectStatus.PATH_MASK.value() & status) > 0
                    || (DefectStatus.CHECKER_MASK.value() & status) > 0) {
                maskCount++;
            }
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            if (notMatchStatus) {
                it.remove();
                continue;
            }

            //5.按照严重程度统计缺陷数量并过滤
            fillingRiskFactor(ccnDefectEntity, riskFactorConfMap);
            int riskFactor = ccnDefectEntity.getRiskFactor();
            if (ComConstants.RiskFactor.SH.value() == riskFactor) {
                superHighCount++;
            } else if (ComConstants.RiskFactor.H.value() == riskFactor) {
                highCount++;
            } else if (ComConstants.RiskFactor.M.value() == riskFactor) {
                mediumCount++;
            } else if (ComConstants.RiskFactor.L.value() == riskFactor) {
                lowCount++;
            }
            boolean meetSeverity = CollectionUtils.isNotEmpty(severity) &&
                    !severity.contains(String.valueOf(riskFactor));
            if (meetSeverity) {
                it.remove();
                continue;
            }

            // 统计历史告警数和新告警数并过滤
            newDefectCount++;

            totalCheckerCount++;

            // String relativePath = PathUtils.getRelativePath(ccnDefectEntity.getUrl(), ccnDefectEntity.getRelPath());
            // ccnDefectEntity.setRelPath(relativePath);
        }

        CCNDefectQueryRspVO ccnFileQueryRspVO = (CCNDefectQueryRspVO) defectQueryRspVO;
        ccnFileQueryRspVO.setSuperHighCount(superHighCount);
        ccnFileQueryRspVO.setHighCount(highCount);
        ccnFileQueryRspVO.setMediumCount(mediumCount);
        ccnFileQueryRspVO.setLowCount(lowCount);
        if (needBuildIdFilter) {
            ccnFileQueryRspVO.setExistCount(existCount + fixCount);
            ccnFileQueryRspVO.setFixCount(0L);
        } else {
            ccnFileQueryRspVO.setExistCount(existCount);
            ccnFileQueryRspVO.setFixCount(fixCount);
        }
        ccnFileQueryRspVO.setIgnoreCount(ignoreCount);
        ccnFileQueryRspVO.setMaskCount(maskCount);
        ccnFileQueryRspVO.setNewDefectCount(newDefectCount);
        ccnFileQueryRspVO.setHistoryDefectCount(historyDefectCount);
        ccnFileQueryRspVO.setTotalCount(totalCheckerCount);

        return null;
    }

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public List<ToolDefectIdVO> queryDefectsByQueryCond(long taskId, DefectQueryReqVO reqVO) {
        log.warn("queryDefectsByQueryCond: taskId: {}, reqVO: {}", taskId, reqVO);
        Set<String> fileList = reqVO.getFileList();
        String author = reqVO.getAuthor();
        List<CCNDefectEntity> defectEntityList =
                ccnDefectDao.findByTaskIdAndAuthorAndRelPaths(taskId, author, fileList);

        filterDefectByCondition(taskId, defectEntityList, null, reqVO, new CCNDefectQueryRspVO(), null);

        log.info("defectEntityList size: {}", defectEntityList.size());
        return defectEntityList.stream()
                .map(item -> new ToolDefectIdVO(item.getTaskId(), ComConstants.Tool.CCN.name(), item.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 校验传入的路径是否合法（路径是否是告警对应的文件）
     * 修改原因：修复安全组检测出的通过告警详情可以获取codecc工具分析服务器的任意文件的安全漏洞：
     *
     * @param filePath
     * @param defectFilePath
     * @return
     * @date 2019/1/15
     * @version V3.4.1
     */
    private void verifyFilePathIsValid(String filePath, String defectFilePath) {
        if (!filePath.equals(defectFilePath)) {
            logger.error("传入参数错误：filePath是非法路径");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{filePath}, null);
        }
    }

    private int getCcnThreshold(long taskId) {
        // 从Task服务获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        if (taskInfoResult == null || taskInfoResult.isNotOk() || taskInfoResult.getData() == null) {
            log.error("get task info fail! task id: {}", taskId);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        if (CollectionUtils.isEmpty(taskInfoResult.getData().getToolConfigInfoList())) {
            return checkerService.getCcnThreshold(new ToolConfigInfoVO());
        }

        // 从任务信息中获取CCN配置信息
        ToolConfigInfoVO toolConfigInfoVO = taskInfoResult.getData()
                .getToolConfigInfoList()
                .stream()
                .filter(toolConfig -> toolConfig.getToolName().equalsIgnoreCase(ComConstants.Tool.CCN.name()))
                .findAny()
                .orElseGet(ToolConfigInfoVO::new);
        // 查询ccn圈复杂度阀值
        return checkerService.getCcnThreshold(toolConfigInfoVO);
    }

    protected CommonDefectQueryRspVO processQueryWarningRequestCore(
            DefectQueryReqVO request,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    ) {
        logger.info("query ccn defect list, request json: {}", JsonUtil.INSTANCE.toJson(request));

        List<Long> taskIdList = request.getTaskIdList();
        String buildId = request.getBuildId();
        // 跨任务不支持快照
        if (CollectionUtils.isNotEmpty(taskIdList)
                && taskIdList.size() == 1
                && isInvalidBuildId(taskIdList.get(0), buildId)) {
            return getInvalidBuildIdResp();
        }

        taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(
                request.getTaskIdList(),
                request.getProjectId(),
                request.getUserId()
        );
        List<String> dimensionList = Lists.newArrayList(Tool.CCN.name());
        List<String> toolNameList = Lists.newArrayList(Tool.CCN.name());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                toolNameList,
                dimensionList,
                taskIdList,
                buildId
        );
        log.info("query ccn defect list, task tool map: {}", taskToolMap);
        // 多任务维度，有些任务可能曾经开启过圈复杂度，但现在已经停用了
        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        request.setTaskIdList(taskIdList);

        // 严重级别
        Set<Map.Entry<Integer, Integer>> riskFactors = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(request.getSeverity())) {
            for (String severity : request.getSeverity()) {
                ComConstants.RiskFactor riskFactor = ComConstants.RiskFactor.get(Integer.parseInt(severity));
                riskFactors.add(thirdPartySystemCaller.getCCNRiskFactorConfig(riskFactor));
            }
        }

        // 分页
        pageNum = Math.max(pageNum - 1, 0);
        pageSize = pageSize <= 0 ? 10 : pageSize;

        // 排序
        Pair<String, Direction> sortPair = getSortFieldAndType(sortField, sortType);
        sortField = sortPair.getFirst();
        sortType = sortPair.getSecond();
        String startCreateTime = request.getStartCreateTime();
        String endCreateTime = request.getEndCreateTime();
        Set<String> fileList = request.getFileList();
        String author = request.getAuthor();
        Set<String> defectIds = StringUtils.isNotEmpty(buildId)
                ? getDefectIdsByBuildId(taskIdList.get(0), toolNameList, buildId)
                : Sets.newHashSet();
        Set<Integer> statusFilters = getStatusFilter(request.getStatus());
        Set<Integer> ignoreReasonTypes = request.getIgnoreReasonTypes();

        Pair<List<CCNDefectEntity>, Long> defectPair = ccnDefectDao.findDefectList(
                taskIdList, author, statusFilters,
                fileList, riskFactors, defectIds,
                pageNum, pageSize, sortField, sortType,
                buildId, startCreateTime, endCreateTime,
                ignoreReasonTypes
        );
        List<CCNDefectEntity> defectList = defectPair.getFirst();
        Long totalCount = defectPair.getSecond();
        defectList = queryWarningLogicComponent.postHandleCCNDefect(defectList, buildId);

        CCNDefectQueryRspVO response = new CCNDefectQueryRspVO();
        // 跨任务不展示阈值
        if (taskIdList.size() == 1) {
            response.setCcnThreshold(getCcnThreshold(taskIdList.get(0)));
        }

        final Map<Long, String> taskNameCnMap = getTaskNameCnMap(request, defectList);

        Map<String, String> riskFactorConfigMap =
                thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        List<CCNDefectVO> defectVOList = defectList.stream()
                .map(entity -> {
                    fillingRiskFactor(entity, riskFactorConfigMap);
                    CCNDefectVO defectVO = new CCNDefectVO();
                    BeanUtils.copyProperties(entity, defectVO);

                    if (Boolean.TRUE.equals(request.getShowTaskNameCn()) && taskNameCnMap != null) {
                        defectVO.setTaskNameCn(taskNameCnMap.get(defectVO.getTaskId()));
                    }

                    return defectVO;
                }).collect(Collectors.toList());
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortType, sortField));
        response.setDefectList(new PageImpl<>(defectVOList, pageable, totalCount));
        response.setTotalCount(totalCount);

        return response;
    }

    private Pair<String, Sort.Direction> getSortFieldAndType(String sortField, Sort.Direction sortType) {
        // sortField: ccn/latestDateTime/createBuildNumber
        if (StringUtils.isEmpty(sortField)) {
            return Pair.of("_id", Sort.Direction.DESC);
        } else {
            // 转换为db field
            if (sortField.equalsIgnoreCase("ccn")) {
                // 圈负责度
                sortField = "ccn";

            } else if (sortField.equalsIgnoreCase("latestDateTime")) {
                // 提交日期
                sortField = "latest_datetime";
            } else if (sortField.equalsIgnoreCase("createBuildNumber")) {
                // 首次发现
                sortField = "create_build_number";
            } else {
                return Pair.of("_id", Sort.Direction.DESC);
            }

            if (sortType == null) {
                sortType = Sort.Direction.DESC;
            }

            return Pair.of(sortField, sortType);
        }
    }

    /**
     * 获取状态过滤条件（默认是待修复）
     * @param status
     * @return
     */
    private Set<Integer> getStatusFilter(Set<String> status) {
        if (CollectionUtils.isEmpty(status)) {
            return Sets.newHashSet(DefectStatus.NEW.value());
        }

        Set<Integer> intStatusSet = status.stream()
                .map(Integer::valueOf)
                .collect(Collectors.toSet());

        // 前端传入: 1/2/4/8
        if (intStatusSet.contains(DefectStatus.PATH_MASK.value())) {
            intStatusSet.add(DefectStatus.CHECKER_MASK.value());
            intStatusSet.add(DefectStatus.CHECKER_MASK.value() | DefectStatus.PATH_MASK.value());
        }

        return intStatusSet;
    }

    private void statisticByStatus(
            List<Long> taskIdList, String author, Set<String> fileList,
            Set<String> defectIds, boolean isSnapshotQuery,
            String startTimeStr, String endTimeStr,
            CCNDefectQueryRspVO response
    ) {
        List<CCNDefectGroupStatisticVO> aggList = ccnDefectDao.statisticDefectCountByStatus(
                taskIdList, author, fileList,
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr
        );

        long existCount = 0;
        long fixCount = 0;
        long ignoreCount = 0;
        long maskCount = 0;

        if (CollectionUtils.isNotEmpty(aggList)) {
            for (CCNDefectGroupStatisticVO agg : aggList) {
                int status = agg.getStatus();

                if (DefectStatus.NEW.value() == status) {
                    existCount += agg.getDefectCount();
                } else if ((DefectStatus.FIXED.value() & status) > 0) {
                    fixCount += agg.getDefectCount();
                } else if ((DefectStatus.IGNORE.value() & status) > 0) {
                    ignoreCount += agg.getDefectCount();
                } else if ((DefectStatus.PATH_MASK.value() & status) > 0
                        || (DefectStatus.CHECKER_MASK.value() & status) > 0) {
                    maskCount += agg.getDefectCount();
                }
            }
        }

        if (isSnapshotQuery) {
            response.setExistCount(existCount + fixCount);
            response.setFixCount(0L);
        } else {
            response.setExistCount(existCount);
            response.setFixCount(fixCount);
        }

        response.setIgnoreCount(ignoreCount);
        response.setMaskCount(maskCount);
    }

    private void statisticBySeverity(
            List<Long> taskIdList, String author, Set<Integer> status, Set<String> fileList,
            Set<String> defectIds, boolean isSnapshotQuery,
            String startTimeStr, String endTimeStr, CCNDefectQueryRspVO response
    ) {

        long superHighCount = ccnDefectDao.countByCondition(
                taskIdList, author, status, fileList,
                Sets.newHashSet(thirdPartySystemCaller.getCCNRiskFactorConfig(ComConstants.RiskFactor.SH)),
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr
        );

        long highCount = ccnDefectDao.countByCondition(
                taskIdList, author, status, fileList,
                Sets.newHashSet(thirdPartySystemCaller.getCCNRiskFactorConfig(ComConstants.RiskFactor.H)),
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr
        );

        long mediumCount = ccnDefectDao.countByCondition(
                taskIdList, author, status, fileList,
                Sets.newHashSet(thirdPartySystemCaller.getCCNRiskFactorConfig(ComConstants.RiskFactor.M)),
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr
        );

        long lowCount = ccnDefectDao.countByCondition(
                taskIdList, author, status, fileList,
                Sets.newHashSet(thirdPartySystemCaller.getCCNRiskFactorConfig(ComConstants.RiskFactor.L)),
                defectIds, isSnapshotQuery,
                startTimeStr, endTimeStr
        );

        response.setSuperHighCount(superHighCount);
        response.setHighCount(highCount);
        response.setMediumCount(mediumCount);
        response.setLowCount(lowCount);
    }

    /**
     * 分页查询符合条件的告警
     * @param taskId 任务id
     * @param reqVO 查询条件
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public ToolDefectPageVO queryDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO reqVO, Integer pageNum,
            Integer pageSize) {
        log.warn("queryDefectsByQueryCond is unimplemented: taskId: {} reqVO: {}, pageNum: {}, pageSize: {}",
                taskId, reqVO, pageNum, pageSize);
        CCNDefectQueryRspVO repVo = (CCNDefectQueryRspVO) processQueryWarningRequestCore(reqVO, pageNum, pageSize,
                "file_path", Direction.ASC);
        List<CCNDefectVO> defectEntityList = repVo.getDefectList().toList();
        log.info("defectEntityList size: {}", defectEntityList.size());

        List<String> ids = defectEntityList.stream()
                .map(CCNDefectVO::getId).collect(Collectors.toList());

        return idListToToolDefectPageVO(
                taskId, Tool.CCN.name(), ids, pageNum, pageSize, repVo.getDefectList().getTotalElements());
    }
}
