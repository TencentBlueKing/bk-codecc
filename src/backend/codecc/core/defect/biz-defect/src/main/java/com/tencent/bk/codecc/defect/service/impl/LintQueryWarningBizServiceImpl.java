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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 *  Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.constant.ComConstants.DEFAULT_LANDUN_WORKSPACE;
import static com.tencent.devops.common.constant.ComConstants.TOOL_NAMES_SEPARATOR;
import static com.tencent.devops.common.constant.ComConstants.Tool.SCAN_COMMIT_TOOLS;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.MongoExecutionTimeoutException;
import com.tencent.bk.codecc.defect.component.QueryWarningLogicComponent;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeFileUrlRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.file.ScmFileInfoCacheRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.file.ScmFileInfoSnapshotRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintStatisticDao;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.TaskPersonalStatisticDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.file.ScmFileInfoSnapshotDao;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.FileContentQueryParams;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.CommonQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.DefectIssueService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.LintQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCustomVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.DefectFileContentSegmentQueryRspVO;
import com.tencent.bk.codecc.defect.vo.DefectFilesInfoVO;
import com.tencent.bk.codecc.defect.vo.DefectInstanceVO;
import com.tencent.bk.codecc.defect.vo.GrayBuildNumAndTaskVO;
import com.tencent.bk.codecc.defect.vo.GrayDefectStaticVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailVO;
import com.tencent.bk.codecc.defect.vo.LintDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.bk.codecc.defect.vo.QueryCheckersAndAuthorsRequest;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVOBase.CheckerSet;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.service.utils.ToolParamUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.FileUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.MD5Utils;
import com.tencent.devops.common.util.PathUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tencent.devops.common.util.ToolUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * Lint类工具的告警查询实现
 *
 * @version V1.0
 * @date 2019/5/8
 */
@Service("LINTQueryWarningBizService")
@Slf4j
public class LintQueryWarningBizServiceImpl extends AbstractQueryWarningBizService implements
        LintQueryWarningSpecialService {

    @Autowired
    private CheckerService multitoolCheckerService;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private CheckerSetRepository checkerSetRepository;
    @Autowired
    private QueryWarningLogicComponent queryWarningLogicComponent;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private TaskPersonalStatisticDao taskPersonalStatisticDao;
    @Autowired
    private LintStatisticDao lintStatisticDao;

    @Autowired
    private ScmFileInfoSnapshotRepository scmFileInfoSnapshotRepository;
    @Autowired
    private CheckerDetailDao checkerDetailDao;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> queryWarningBizServiceFactory;
    @Autowired
    private CodeFileUrlRepository codeFileUrlRepository;
    @Autowired
    private ScmFileInfoSnapshotDao scmFileInfoSnapshotDao;
    @Autowired
    private ScmFileInfoCacheRepository scmFileInfoCacheRepository;

    @Autowired
    private DefectIssueService defectIssueService;

    @Override
    public Long countNewDefectFile(CountDefectFileRequest request) {
        String dimensions = List2StrUtil.toString(request.getDimensionList(), ComConstants.STRING_SPLIT);
        List<String> toolNameList = ParamUtils.getTools("", dimensions, request.getTaskId(), "", true);
        Set<Integer> statusSet = Sets.newHashSet(DefectStatus.NEW.value());

        return lintDefectV2Dao.countFileByCondition(
                request.getTaskId(),
                toolNameList,
                statusSet,
                request.getAuthor(),
                request.getChecker(),
                request.getSeverityList()
        );
    }

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    protected CommonDefectQueryRspVO processQueryWarningRequestCore(
            DefectQueryReqVO request,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    ) {
        log.info("query lint defect list, request json: {}", JsonUtil.INSTANCE.toJson(request));

        // 跨任务不支持快照
        List<Long> taskIdList = request.getTaskIdList();
        String buildId = request.getBuildId();
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
        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                dimensionList,
                taskIdList,
                buildId
        );

        // 多任务维度，有些任务可能曾经开启过某款工具，但现在已经停用了
        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        request.setTaskIdList(taskIdList);

        LintDefectQueryRspVO lintDefectQueryRsp = new LintDefectQueryRspVO();
        lintDefectQueryRsp.setDefectList(new Page<>(0, pageNum, pageSize, 0, Lists.newArrayList()));
        if (MapUtils.isEmpty(taskToolMap)) {
            return lintDefectQueryRsp;
        }

        log.info("query lint defect list, task tool map: {}, \n{}", taskToolMap.size(),
                JsonUtil.INSTANCE.toJson(taskToolMap));

        // 规则集筛选
        Set<String> pkgChecker = getCheckers(request.getCheckerSet(), request.getChecker(), taskToolMap, dimensionList);
        log.info("defect list pkgChecker: {}, {}", taskIdList, pkgChecker.size());
        if (request.getCheckerSet() != null && CollectionUtils.isEmpty(pkgChecker)) {
            return lintDefectQueryRsp;
        }

        // 快照
        Pair<Set<String>, Set<String>> defectIdsPair = StringUtils.isNotEmpty(buildId)
                ? getDefectIdsPairByBuildId(taskToolMap, buildId)
                : Pair.of(Sets.newHashSet(), Sets.newHashSet());
        Set<String> defectMongoIdSet = defectIdsPair.getFirst();
        Set<String> defectThirdPartyIdSet = defectIdsPair.getSecond();

        Set<String> condStatusList = request.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            if (condStatusList == null) {
                condStatusList = Sets.newHashSet();
                request.setStatus(condStatusList);
            }

            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
        }

        // 前端传入: 1/2/4/8
        if (condStatusList.contains(String.valueOf(DefectStatus.PATH_MASK.value()))) {
            condStatusList.add(String.valueOf(DefectStatus.CHECKER_MASK.value()));
            condStatusList.add(String.valueOf(DefectStatus.CHECKER_MASK.value() | DefectStatus.PATH_MASK.value()));
        }

        // 如果查询包含告警操作筛选，需要加上issueIds的过滤
        if (CollectionUtils.isNotEmpty(request.getOperates())
                && (request.getOperates().contains(ComConstants.CodeCCDefectOpsType.NO_OPS.name())
                || request.getOperates().contains(ComConstants.CodeCCDefectOpsType.ISSUE_SUBMIT.name()))) {
            Set<String> issueDefectIds = defectIssueService.getDefectIdByTaskIdAndToolMap(taskToolMap);
            request.setSubmitDefectIds(issueDefectIds);
        }

        // 按文件聚类
        String clusterType = request.getClusterType();
        if (StringUtils.isNotEmpty(clusterType) && ComConstants.ClusterType.file.name().equalsIgnoreCase(clusterType)) {
            // 前端已经没有按文件聚类查看告警
            return lintDefectQueryRsp;
        } else {
            // 按问题聚类
            Map<String, Boolean> filedMap = getDefectBaseFieldMap();
            Page<LintDefectV2Entity> result;

            try {
                result = lintDefectV2Dao.findDefectPageByCondition(
                        taskToolMap, request, defectMongoIdSet, pkgChecker,
                        filedMap, pageNum, pageSize, sortField,
                        sortType, defectThirdPartyIdSet, request.getProjectId(), request.getUserId()
                );
            } catch (Throwable t) {
                // 温馨提示
                if (t instanceof CodeCCException || (t instanceof UncategorizedMongoDbException
                        && t.getCause() instanceof MongoExecutionTimeoutException)) {
                    throw new CodeCCException(DefectMessageCode.PROJECT_DEFECT_TOO_MANY, t);
                }

                throw t;
            }

            log.info("get defect group by problem for task before snapshot post handle: {}, {}, {}",
                    taskIdList, result.getCount(), pkgChecker.size());

            List<LintDefectVO> defectVOList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(result.getRecords())) {
                List<LintDefectV2Entity> lintDefectList =
                        queryWarningLogicComponent.postHandleLintDefect(result.getRecords(), buildId);
                log.info("get defect group by problem for task after snapshot post handle: {}, {}, {}",
                        taskIdList, lintDefectList.size(), pkgChecker.size());

                final Map<Long, String> taskNameCnMap = getTaskNameCnMap(request, lintDefectList);
                final Set<String> hasCodeCommonDefectIds = hasCodeCommonDefectId(lintDefectList);
                defectVOList = lintDefectList.stream().map(defectV2Entity -> {
                    if (StringUtils.isEmpty(defectV2Entity.getFileName())) {
                        String filePathOrRelPath = StringUtils.isNotEmpty(defectV2Entity.getFilePath())
                                ? defectV2Entity.getFilePath() : defectV2Entity.getRelPath();
                        String fileNameByPath = FileUtils.getFileNameByPath(filePathOrRelPath);
                        defectV2Entity.setFileName(fileNameByPath);
                    }

                    LintDefectVO defectVO = new LintDefectVO();
                    BeanUtils.copyProperties(defectV2Entity, defectVO);
                    defectVO.setSeverity(
                            defectVO.getSeverity() == ComConstants.PROMPT_IN_DB
                                    ? ComConstants.PROMPT
                                    : defectVO.getSeverity()
                    );

                    if (Boolean.TRUE.equals(request.getShowTaskNameCn()) && taskNameCnMap != null) {
                        defectVO.setTaskNameCn(taskNameCnMap.get(defectVO.getTaskId()));
                    }
                    defectVO.setHasCodeComment(CollectionUtils.isNotEmpty(hasCodeCommonDefectIds)
                            && hasCodeCommonDefectIds.contains(defectVO.getEntityId()));
                    return defectVO;
                }).collect(Collectors.toList());
            }

            Page<LintDefectVO> pageResult = new Page<>(result.getCount(), result.getPage(),
                    result.getPageSize(), result.getTotalPages(), defectVOList);
            lintDefectQueryRsp.setDefectList(pageResult);
        }

        return lintDefectQueryRsp;
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId, DefectQueryReqVO request,
            int pageNum, int pageSize, String sortField, Direction sortType
    ) {
        return processQueryWarningRequestCore(request, pageNum, pageSize, sortField, sortType);
    }


    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
            String projectId,
            Long taskId,
            String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField,
            Sort.Direction sortType
    ) {
        if (!(queryWarningDetailReq instanceof LintDefectDetailQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"queryWarningDetailReq"}, null);
        }

        String buildId = queryWarningDetailReq.getBuildId();
        LintDefectDetailQueryRspVO responseVO = new LintDefectDetailQueryRspVO();
        LintDefectDetailQueryReqVO requestVO = (LintDefectDetailQueryReqVO) queryWarningDetailReq;
        LintDefectV2Entity defectEntity = lintDefectV2Repository.findByEntityId(requestVO.getEntityId());
        if (defectEntity == null) {
            log.info("defect not found by condition: {}", requestVO);
            return responseVO;
        }

        // 前端不强制传入taskId
        // NOCC:IP-PARAMETER-IS-DEAD-BUT-OVERWRITTEN(设计如此:)
        taskId = defectEntity.getTaskId();

        defectEntity =
                queryWarningLogicComponent.postHandleLintDefect(Lists.newArrayList(defectEntity), buildId).get(0);

        if (StringUtils.isNotEmpty(buildId)) {
            setDefectStatusOnLastBuild(taskId, buildId, defectEntity.getEntityId(), responseVO);
        }

        LintDefectDetailVO lintDefectDetailVO = JsonUtil.INSTANCE
                .to(JsonUtil.INSTANCE.toJson(defectEntity), LintDefectDetailVO.class);

        // 设置告警评论
        if (null != defectEntity.getCodeComment()
                && CollectionUtils.isNotEmpty(defectEntity.getCodeComment().getCommentList())) {
            CodeCommentVO codeCommentVO = convertCodeComment(defectEntity.getCodeComment());
            lintDefectDetailVO.setCodeComment(codeCommentVO);
        }

        // 设置忽略审批
        if (StringUtils.isNotBlank(defectEntity.getIgnoreApprovalId())) {
            IgnoreApprovalVO ignoreApproval = getIgnoreApprovalById(defectEntity.getIgnoreApprovalId());
            if (ignoreApproval != null) {
                lintDefectDetailVO.setIgnoreApprovalUrl(ignoreApproval.getItsmUrl());
            }
        }

        // 获取告警文件内容
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();
        getFilesContent(taskId, userId, taskDetailVO, buildId, defectEntity, lintDefectDetailVO);

        // 获取告警规则详情和规则类型
        getCheckerDetailAndType(
                lintDefectDetailVO, lintDefectDetailVO.getToolName(), queryWarningDetailReq.getPattern()
        );

        boolean isMigrationMatchTool =
                commonDefectMigrationService.matchToolNameSet().contains(defectEntity.getToolName());

        if (isMigrationMatchTool) {
            replaceFileNameWithURLForCommonTools(taskId, defectEntity.getFilePath(), lintDefectDetailVO);
            responseVO.setRelativePath("");
            responseVO.setFilePath(lintDefectDetailVO.getFilePath());
            responseVO.setFileName(lintDefectDetailVO.getFileName());
        } else {
            String relativePath = PathUtils.getRelativePath(defectEntity.getUrl(), defectEntity.getRelPath());
            String url = PathUtils.getFileUrl(defectEntity.getUrl(), defectEntity.getBranch(),
                    defectEntity.getRevision(), defectEntity.getRelPath());
            responseVO.setRelativePath(relativePath);
            responseVO.setFilePath(StringUtils.isEmpty(url) ? defectEntity.getFilePath() : url);
            responseVO.setFileName(defectEntity.getFileName());
        }

        responseVO.setLintDefectDetailVO(lintDefectDetailVO);

        return responseVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryDefectDetailWithoutFileContent(
            Long taskId,
            String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField,
            Sort.Direction sortType
    ) {
        if (!(queryWarningDetailReq instanceof LintDefectDetailQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"queryWarningDetailReq"}, null);
        }

        LintDefectDetailQueryRspVO responseVO = new LintDefectDetailQueryRspVO();
        LintDefectDetailQueryReqVO requestVO = (LintDefectDetailQueryReqVO) queryWarningDetailReq;
        LintDefectV2Entity defectEntity = lintDefectV2Repository.findByEntityId(requestVO.getEntityId());
        if (defectEntity == null) {
            log.info("defect not found by condition: {}", requestVO);
            return responseVO;
        }

        String buildId = queryWarningDetailReq.getBuildId();
        taskId = defectEntity.getTaskId();
        LintDefectDetailVO lintDefectDetailVO = getLintDefectDetailVO(defectEntity, buildId, taskId, responseVO);

        // 获取告警文件的信息
        getFilesInfo(defectEntity, lintDefectDetailVO);

        // 获取告警规则详情和规则类型
        getCheckerDetailAndType(
                lintDefectDetailVO, lintDefectDetailVO.getToolName(), queryWarningDetailReq.getPattern()
        );

        // 设置忽略审批
        if (StringUtils.isNotBlank(defectEntity.getIgnoreApprovalId())) {
            IgnoreApprovalVO ignoreApproval = getIgnoreApprovalById(defectEntity.getIgnoreApprovalId());
            if (ignoreApproval != null) {
                lintDefectDetailVO.setIgnoreApprovalUrl(ignoreApproval.getItsmUrl());
            }
        }

        setLintDefectDetailQueryRspVO(defectEntity, lintDefectDetailVO, responseVO, taskId);

        responseVO.setRevision(defectEntity.getRevision());

        return responseVO;
    }

    @Override
    public DefectFileContentSegmentQueryRspVO processQueryDefectFileContentSegment(
            String projectId,
            String userId,
            QueryDefectFileContentSegmentReqVO request
    ) {
        String toolName = request.getToolName();
        String filePath = request.getFilePath();

        DefectFileContentSegmentQueryRspVO resp = new DefectFileContentSegmentQueryRspVO();
        resp.setFilePath(filePath);
        resp.setFileName(getFileName(filePath));

        // BLACKDUCK工具没有代码文件内容，无需获取
        if (toolName.equals(ComConstants.Tool.BLACKDUCK.name())) {
            resp.setFileContent("empty");
            return resp;
        }

        String entityId = request.getEntityId();
        LintDefectV2Entity lintDefectV2Entity = lintDefectV2Repository.findByEntityId(entityId);
        if (lintDefectV2Entity == null) {
            log.error("Can't find lint defect entity(entityId:{}, toolName:{}, filePath:{})",
                    entityId,
                    toolName,
                    filePath);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"找不到该缺陷实体"}, null);
        }

        if (StringUtils.isEmpty(lintDefectV2Entity.getRelPath())
                && lintDefectV2Entity.getFilePath().length() > DEFAULT_LANDUN_WORKSPACE.length()) {
            // 从FilePath中截取默认的蓝盾工作路径，获得RelPath
            lintDefectV2Entity.setRelPath(lintDefectV2Entity.getFilePath()
                    .substring(DEFAULT_LANDUN_WORKSPACE.length()));
        }

        if (StringUtils.isEmpty(lintDefectV2Entity.getRelPath())) {
            resp.setFileContent("rel_path is null, scm info may be missing");
            return resp;
        }

        if (SCAN_COMMIT_TOOLS.contains(toolName)) {
            // 扫描Commit的工具，会再路径中自带commitId (例：/1.go@12312312 中 12312312 为commitId)，需要去除工具自带的CommitId
            if (StringUtils.isNotEmpty(lintDefectV2Entity.getRelPath())
                    && lintDefectV2Entity.getRelPath().contains("@")) {
                lintDefectV2Entity.setRelPath(lintDefectV2Entity.getRelPath().substring(0,
                        lintDefectV2Entity.getRelPath().lastIndexOf("@")));
            }
        }

        resp.setRevision(lintDefectV2Entity.getRevision());
        resp.setBranch(lintDefectV2Entity.getBranch());

        kotlin.Pair<String, String> relativePathAndRelPath = PathUtils.getRelativePathAndRelPath(
                lintDefectV2Entity.getUrl(), lintDefectV2Entity.getRelPath(),
                lintDefectV2Entity.getFilePath(), request.getFilePath());
        resp.setRelativePath(relativePathAndRelPath.getFirst());

        String buildId = lintDefectV2Entity.getBuildId();
        // 如果没有指定 buildId, 则默认取最新的 buildId
        if (StringUtils.isBlank(buildId)) {
            buildId = getLastestBuildIdByTaskIdAndToolName(lintDefectV2Entity.getTaskId(), request.getToolName());
        }

        FileContentQueryParams queryParams = FileContentQueryParams.queryParams(
                lintDefectV2Entity.getTaskId(), projectId, userId,
                lintDefectV2Entity.getUrl(), lintDefectV2Entity.getRepoId(),
                relativePathAndRelPath.getSecond(), lintDefectV2Entity.getFilePath(),
                lintDefectV2Entity.getRevision(), lintDefectV2Entity.getBranch(), lintDefectV2Entity.getSubModule(),
                buildId
        );
        queryParams.setTryBestForPrivate(request.isTryBestForPrivate());
        String content = getFileContent(queryParams);

        resp.setFileContent(content);
        resp.setBeginLine(lintDefectV2Entity.getLineNum());
        resp.setEndLine(lintDefectV2Entity.getLineNum());

        return resp;
    }


    /**
     * 设置 response 中各个字段的值
     *
     * @param defectEntity
     * @param lintDefectDetailVO
     * @param responseVO
     * @param taskId
     * @return void
     * @author weijianguan
     * @date 2023/6/22
     */
    private void setLintDefectDetailQueryRspVO(LintDefectV2Entity defectEntity, LintDefectDetailVO lintDefectDetailVO,
            LintDefectDetailQueryRspVO responseVO, Long taskId) {
        boolean isMigrationMatchTool =
                commonDefectMigrationService.matchToolNameSet().contains(defectEntity.getToolName());

        if (isMigrationMatchTool) {
            replaceFileNameWithURLForCommonTools(taskId, defectEntity.getFilePath(), lintDefectDetailVO);
            responseVO.setRelativePath("");
            responseVO.setFilePath(lintDefectDetailVO.getFilePath());
            responseVO.setFileName(lintDefectDetailVO.getFileName());
        } else {
            String relativePath;
            String url;
            if (SCAN_COMMIT_TOOLS.contains(defectEntity.getToolName())) {
                String relPath = ToolUtils.convertCommitToolPathToCommon(defectEntity.getRelPath());
                relativePath = PathUtils.getRelativePath(defectEntity.getUrl(), relPath);
                // 扫描Commit的工具，直接通过commitId获取代码
                url = PathUtils.getFileUrl(defectEntity.getUrl(), defectEntity.getRevision(), relPath);
            } else {
                relativePath = PathUtils.getRelativePath(defectEntity.getUrl(), defectEntity.getRelPath());
                url = PathUtils.getFileUrl(defectEntity.getUrl(), defectEntity.getBranch(),
                        defectEntity.getRevision(), defectEntity.getRelPath());
            }
            responseVO.setRelativePath(relativePath);
            responseVO.setFilePath(StringUtils.isEmpty(url) ? defectEntity.getFilePath() : url);
            responseVO.setFileName(defectEntity.getFileName());
        }
        // 当前行最新代码提交人
        String commitAuthor = getCommitAuthor(taskId, lintDefectDetailVO.getToolName(), defectEntity);
        lintDefectDetailVO.setCommitAuthor(commitAuthor);
        responseVO.setLintDefectDetailVO(lintDefectDetailVO);
    }

    /**
     * 设置 EMPTY_FILE_CONTENT_TIPS（文件内容为空）
     *
     * @param lintDefectDetailVO
     * @return void
     * @date 2023/6/22
     */
    private void setEmptyFileInfo(LintDefectDetailVO lintDefectDetailVO) {
        String md5 = MD5Utils.getMD5(lintDefectDetailVO.getFilePath());
        DefectFilesInfoVO fileInfo = new DefectFilesInfoVO();
        fileInfo.setFilePath(lintDefectDetailVO.getFilePath());
        fileInfo.setFileMd5(md5);
        fileInfo.setMinLineNum(lintDefectDetailVO.getLineNum());
        fileInfo.setMaxLineNum(lintDefectDetailVO.getLineNum());
        fileInfo.setContents(EMPTY_FILE_CONTENT_TIPS);
        lintDefectDetailVO.getFileInfoMap().put(md5, fileInfo);
    }

    /**
     * 获取所有跟告警相关的文件内容, 不带代码片段
     *
     * @param defectEntity
     * @param lintDefectDetailVO
     */
    private void getFilesInfo(LintDefectV2Entity defectEntity, LintDefectDetailVO lintDefectDetailVO) {
        String toolName = defectEntity.getToolName();

        // BLACKDUCK工具没有代码文件内容，无需获取
        if (toolName.equals(ComConstants.Tool.BLACKDUCK.name())) {
            setEmptyFileInfo(lintDefectDetailVO);

            return;
        }

        // 递归解析defectInstance，获取所有的相关文件
        getDefectFiles(lintDefectDetailVO);
        Map<String, DefectFilesInfoVO> fileMap = lintDefectDetailVO.getFileInfoMap();
        boolean isMigrationMatchTool
                = commonDefectMigrationService.matchToolNameSet().contains(defectEntity.getToolName());

        // 若是迁移的工具
        if (isMigrationMatchTool) {
            IQueryWarningBizService service = queryWarningBizServiceFactory.createBizService(
                    toolName,
                    BusinessType.QUERY_WARNING.value(),
                    IQueryWarningBizService.class
            );

            if (service instanceof CommonQueryWarningSpecialService) {
                DefectDetailVO requestDefect = covertToCommonDefectFileContentRequest(
                        defectEntity,
                        fileMap,
                        lintDefectDetailVO.getDefectInstances()
                );

                CommonQueryWarningSpecialService specialService = (CommonQueryWarningSpecialService) service;
                DefectDetailVO commonVO;
                try {
                    commonVO = specialService.getFilesInfo(requestDefect);
                } catch (Exception e) {
                    if (!(e instanceof CodeCCException)) {
                        log.error("get file content fail, entity id: {}", defectEntity.getEntityId(), e);
                    }

                    lintDefectDetailVO.setFileInfoMap(Maps.newHashMap());
                    lintDefectDetailVO.setMessage(defectEntity.getMessage());

                    return;
                }

                lintDefectDetailVO.setFileInfoMap(commonVO.getFileInfoMap());
                lintDefectDetailVO.setMessage(commonVO.getMessage());
            }
        }
    }

    private LintDefectDetailVO getLintDefectDetailVO(LintDefectV2Entity defectEntity, String buildId,
            Long taskId, LintDefectDetailQueryRspVO responseVO) {
        defectEntity =
                queryWarningLogicComponent.postHandleLintDefect(Lists.newArrayList(defectEntity), buildId).get(0);

        if (StringUtils.isNotEmpty(buildId)) {
            setDefectStatusOnLastBuild(taskId, buildId, defectEntity.getEntityId(), responseVO);
        }

        LintDefectDetailVO lintDefectDetailVO = JsonUtil.INSTANCE
                .to(JsonUtil.INSTANCE.toJson(defectEntity), LintDefectDetailVO.class);

        // 设置告警评论
        if (null != defectEntity.getCodeComment()
                && CollectionUtils.isNotEmpty(defectEntity.getCodeComment().getCommentList())) {
            CodeCommentVO codeCommentVO = convertCodeComment(defectEntity.getCodeComment());
            lintDefectDetailVO.setCodeComment(codeCommentVO);
        }

        return lintDefectDetailVO;
    }

    /**
     * mock common tools
     * see CommonQueryWarningBizServiceImpl#replaceFileNameWithURL(DefectBaseVO, Map)
     */
    private void replaceFileNameWithURLForCommonTools(
            long taskId,
            String filePath,
            LintDefectDetailVO lintDefectDetailVO
    ) {
        if (StringUtils.isEmpty(filePath)) {
            return;
        }

        CodeFileUrlEntity codeFileUrlEntity = codeFileUrlRepository.findFirstByTaskIdAndFile(taskId, filePath);
        if (codeFileUrlEntity != null) {
            lintDefectDetailVO.setFilePath(codeFileUrlEntity.getUrl());
            lintDefectDetailVO.setFileVersion(codeFileUrlEntity.getVersion());
        }

        int fileNameIndex = filePath.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePath.lastIndexOf("\\");
        }

        String fileName = filePath.substring(fileNameIndex + 1);
        lintDefectDetailVO.setFileName(fileName);
    }

    /**
     * 获取所有跟告警相关的文件内容
     *
     * @param taskId
     * @param userId
     * @param taskDetailVO
     * @param buildId
     * @param defectEntity
     * @param lintDefectDetailVO
     */
    private void getFilesContent(long taskId, String userId, TaskDetailVO taskDetailVO, String buildId,
            LintDefectV2Entity defectEntity, LintDefectDetailVO lintDefectDetailVO) {
        String toolName = defectEntity.getToolName();

        // BLACKDUCK工具没有代码文件内容，无需获取
        if (toolName.equals(ComConstants.Tool.BLACKDUCK.name())) {
            String md5 = MD5Utils.getMD5(lintDefectDetailVO.getFilePath());
            DefectFilesInfoVO fileInfo = new DefectFilesInfoVO();
            fileInfo.setFilePath(lintDefectDetailVO.getFilePath());
            fileInfo.setFileMd5(md5);
            fileInfo.setMinLineNum(lintDefectDetailVO.getLineNum());
            fileInfo.setMaxLineNum(lintDefectDetailVO.getLineNum());
            fileInfo.setContents(I18NUtils.getMessage("EMPTY_FILE_CONTENT_TIPS"));
            lintDefectDetailVO.getFileInfoMap().put(md5, fileInfo);
        } else {
            // 递归解析defectInstance，获取所有的相关文件
            getDefectFiles(lintDefectDetailVO);
            Map<String, DefectFilesInfoVO> fileMap = lintDefectDetailVO.getFileInfoMap();
            boolean isMigrationMatchTool
                    = commonDefectMigrationService.matchToolNameSet().contains(defectEntity.getToolName());

            // 若是迁移的工具
            if (isMigrationMatchTool) {
                DefectDetailVO request = covertToCommonDefectFileContentRequest(
                        defectEntity,
                        fileMap,
                        lintDefectDetailVO.getDefectInstances()
                );

                IQueryWarningBizService service = queryWarningBizServiceFactory.createBizService(
                        toolName,
                        BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class
                );

                if (service instanceof CommonQueryWarningSpecialService) {
                    CommonQueryWarningSpecialService specialService = (CommonQueryWarningSpecialService) service;
                    DefectDetailVO commonVO;
                    try {
                        commonVO = specialService.getFilesContent(request);
                    } catch (Exception e) {
                        if (!(e instanceof CodeCCException)) {
                            log.error("get file content fail, entity id: {}", defectEntity.getEntityId(), e);
                        }

                        lintDefectDetailVO.setFileInfoMap(Maps.newHashMap());
                        lintDefectDetailVO.setMessage(defectEntity.getMessage());

                        return;
                    }

                    lintDefectDetailVO.setFileInfoMap(commonVO.getFileInfoMap());
                    lintDefectDetailVO.setMessage(commonVO.getMessage());
                }

                return;
            }

            // 非主文件的文件需要从blame缓存里面获取文件信息，用于调用接口获取文件内容
            Set<String> notMainFileSet = fileMap.values().stream()
                    .filter(it -> !it.getFilePath().equals(lintDefectDetailVO.getFilePath()))
                    .map(DefectFilesInfoVO::getFilePath)
                    .collect(Collectors.toSet());

            final Map<String, ScmFileInfoSnapshotEntity> notMainFileScmSnapshotMap = Maps.newHashMap();

            if (CollectionUtils.isNotEmpty(notMainFileSet)) {
                // 如果接口没传buildId，则获取工具最新的buildId，通过最新的buildId获取最新的scm文件信息
                if (StringUtils.isBlank(buildId)) {
                    buildId = getLastestBuildIdByTaskIdAndToolName(taskId, lintDefectDetailVO.getToolName());
                }

                if (StringUtils.isNotEmpty(buildId)) {
                    Map<String, ScmFileInfoSnapshotEntity> scmSnapshotMap = getScmSnapshotMap(
                            taskId,
                            buildId,
                            notMainFileSet
                    );

                    notMainFileScmSnapshotMap.putAll(scmSnapshotMap);
                }
            }

            String relPath = StringUtils.isEmpty(lintDefectDetailVO.getRelPath())
                    ? lintDefectDetailVO.getFilePath().substring(22) : lintDefectDetailVO.getRelPath();
            String filePath = lintDefectDetailVO.getFilePath();
            String projectId = taskDetailVO.getProjectId();
            String url = defectEntity.getUrl();
            String repoId = defectEntity.getRepoId();
            String revision = defectEntity.getRevision();
            String branch = defectEntity.getBranch();
            String subModule = defectEntity.getSubModule();

            // 如果没有指定 buildId, 则默认取最新的 buildId
            if (StringUtils.isBlank(buildId)) {
                buildId = getLastestBuildIdByTaskIdAndToolName(taskId, toolName);
            }

            String finalBuildId = buildId;
            fileMap.forEach((fileMd5, fileInfoVO) -> {
                String content = null;
                if (!fileInfoVO.getFilePath().equals(lintDefectDetailVO.getFilePath())) {
                    ScmFileInfoSnapshotEntity snapshot = notMainFileScmSnapshotMap.get(fileInfoVO.getFilePath());
                    if (snapshot != null) {
                        FileContentQueryParams queryItem = FileContentQueryParams.queryParams(
                                taskId, projectId, userId, snapshot.getUrl(), snapshot.getRepoId(),
                                snapshot.getRelPath(), snapshot.getFilePath(), snapshot.getRevision(),
                                snapshot.getBranch(), snapshot.getSubModule(), finalBuildId
                        );
                        content = getFileContent(queryItem);
                    }
                } else {
                    FileContentQueryParams queryItem = FileContentQueryParams.queryParams(
                            taskId, projectId, userId,
                            url, repoId, relPath, filePath, revision,
                            branch, subModule, finalBuildId
                    );
                    content = getFileContent(queryItem);
                    if (!fileInfoVO.getFileMd5().equals(lintDefectDetailVO.getFileMd5())) {
                        lintDefectDetailVO.setFileMd5(fileInfoVO.getFileMd5());
                    }
                }
                if (StringUtils.isEmpty(content)) {
                    content = I18NUtils.getMessage("EMPTY_FILE_CONTENT_TIPS");
                }
                fileInfoVO.setContents(content);
            });
        }
    }

    private Map<String, ScmFileInfoSnapshotEntity> getScmSnapshotMap(
            Long taskId,
            String buildId,
            Set<String> notMainFileSet

    ) {
        if (CollectionUtils.isEmpty(notMainFileSet) || StringUtils.isEmpty(buildId)) {
            return Maps.newHashMap();
        }

        List<ScmFileInfoSnapshotEntity> snapshotList =
                scmFileInfoSnapshotRepository.findByTaskIdAndBuildIdAndFilePathIn(
                        taskId, buildId, notMainFileSet
                );

        // 若快照SCM为空集，则说明该buildId可能是超快增量没有缓存SCM，则获取最近1次缓存
        if (CollectionUtils.isEmpty(snapshotList)) {
            snapshotList = scmFileInfoSnapshotDao.aggByTaskIdAndFilePathInOrderByUpdateTimeDesc(taskId, notMainFileSet);
        }

        return snapshotList.stream()
                .collect(Collectors.toMap(ScmFileInfoSnapshotEntity::getFilePath, Function.identity(), (k, v) -> k));
    }

    /**
     * 获取告警详情（将告警entity转成VO，整理告警相关文件）
     *
     * @param lintDefectDetailVO
     * @return
     */
    protected void getDefectFiles(LintDefectDetailVO lintDefectDetailVO) {
        List<DefectInstanceVO> defectInstanceList = lintDefectDetailVO.getDefectInstances();
        if (CollectionUtils.isNotEmpty(defectInstanceList)) {
            for (DefectInstanceVO defectInstance : defectInstanceList) {
                List<DefectInstanceVO.Trace> traces = defectInstance.getTraces();
                for (int i = 0; i < traces.size(); i++) {
                    DefectInstanceVO.Trace trace = traces.get(i);
                    if (trace.getTraceNum() == null) {
                        trace.setTraceNum(i + 1);
                    }
                    parseTrace(lintDefectDetailVO.getFileInfoMap(), trace);
                }
            }
        }
        if (lintDefectDetailVO.getFileInfoMap().size() < 1) {
            String md5 = lintDefectDetailVO.getFileMd5();
            if (StringUtils.isEmpty(md5)) {
                md5 = MD5Utils.getMD5(lintDefectDetailVO.getFilePath());
            }
            DefectFilesInfoVO fileInfo = new DefectFilesInfoVO();
            fileInfo.setFilePath(lintDefectDetailVO.getFilePath());
            fileInfo.setFileMd5(md5);
            fileInfo.setMinLineNum(lintDefectDetailVO.getLineNum());
            fileInfo.setMaxLineNum(lintDefectDetailVO.getLineNum());
            lintDefectDetailVO.getFileInfoMap().put(md5, fileInfo);
        }
    }

    @NotNull
    protected Map<String, Boolean> getDefectBaseFieldMap() {
        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("id", true);
        filedMap.put("file_name", true);
        filedMap.put("line_num", true);
        filedMap.put("file_path", true);
        filedMap.put("rel_path", true);
        filedMap.put("checker", true);
        filedMap.put("message", true);
        filedMap.put("author", true);
        filedMap.put("severity", true);
        filedMap.put("line_update_time", true);
        filedMap.put("create_time", true);
        filedMap.put("create_build_number", true);
        filedMap.put("status", true);
        filedMap.put("mark", true);
        filedMap.put("mark_time", true);
        filedMap.put("mark_but_no_fixed", true);
        filedMap.put("fixed_time", true);
        filedMap.put("ignore_time", true);
        filedMap.put("tool_name", true);
        filedMap.put("ignore_comment_defect", true);
        filedMap.put("task_id", true);
        filedMap.put("ignore_reason_type", true);
        filedMap.put("ignore_reason", true);
        filedMap.put("ignore_author", true);
        // 需求(120433908)需要展示分支信息
        filedMap.put("url", true);
        filedMap.put("branch", true);
        filedMap.put("ignore_approval_id", true);
        filedMap.put("ignore_approval_status", true);
        return filedMap;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            String projectId,
            QueryCheckersAndAuthorsRequest request
    ) {
        List<String> statusList = request.getStatusList();
        Set<String> statusSet = CollectionUtils.isEmpty(statusList) ? Sets.newHashSet() : Sets.newHashSet(statusList);
        List<String> toolNameList = request.getToolNameList();
        List<String> dimensionList = request.getDimensionList();
        List<Long> taskIdList = request.getTaskIdList();
        String checkerSetId = request.getCheckerSet();
        String buildId = request.getBuildId();
        boolean isMultiTaskQuery = Boolean.TRUE.equals(request.getMultiTaskQuery());

        taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(taskIdList, projectId, userId);
        dimensionList = ParamUtils.allDimensionIfEmptyForLint(dimensionList);
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                toolNameList,
                dimensionList,
                taskIdList,
                buildId
        );

        QueryWarningPageInitRspVO response = new QueryWarningPageInitRspVO();
        if (MapUtils.isEmpty(taskToolMap)) {
            return response;
        }

        // 规则集筛选
        toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        Set<String> pkgChecker = getCheckers(checkerSetId, null, null, toolNameList, dimensionList);
        Set<String> finalAuthors = Sets.newHashSet();
        Set<String> checkerList = Sets.newHashSet();
        Set<String> defectPaths = Sets.newTreeSet();

        if (isMultiTaskQuery) {
            finalAuthors.addAll(taskPersonalStatisticDao.getLintAuthorSet(taskToolMap.keySet()));
            checkerList.addAll(pkgChecker);
        } else {
            // 快照查补偿处理
            if (StringUtils.isNotEmpty(buildId) && CollectionUtils.isNotEmpty(statusSet)) {
                String newStatusStr = String.valueOf(DefectStatus.NEW.value());
                String fixedStatusStr = String.valueOf(DefectStatus.FIXED.value());

                if (statusSet.contains(newStatusStr)) {
                    statusSet.add(newStatusStr);
                    statusSet.add(fixedStatusStr);
                } else {
                    // 快照查，不存在已修复
                    statusSet.remove(newStatusStr);
                    statusSet.remove(fixedStatusStr);
                }
            }

            Entry<Long, List<String>> kv = taskToolMap.entrySet().stream().findFirst().get();
            Long taskIdForSingleQuery = kv.getKey();
            List<String> toolNameListForSingleQuery = kv.getValue();

            // 根据状态过滤后获取规则，处理人、文件路径
            List<LintFileVO> aggInfoList = lintDefectV2Dao.getCheckerAndAuthorAndPath(
                    taskIdForSingleQuery,
                    toolNameListForSingleQuery,
                    statusSet,
                    pkgChecker
            );
            log.info("get file info size is: {}, tool name: {}", aggInfoList.size(), toolNameListForSingleQuery);

            for (LintFileVO fileInfo : aggInfoList) {
                // 设置作者
                if (CollectionUtils.isNotEmpty(fileInfo.getAuthorList())) {
                    List<String> authorSet = fileInfo.getAuthorList().stream()
                            .filter(CollectionUtils::isNotEmpty)
                            .flatMap(Collection::stream)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toList());

                    finalAuthors.addAll(authorSet);
                }

                // 设置规则
                if (CollectionUtils.isNotEmpty(fileInfo.getCheckerList())) {
                    checkerList.addAll(fileInfo.getCheckerList());
                }

                // 获取所有警告文件的相对路径
                String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
                if (StringUtils.isNotBlank(relativePath)) {
                    defectPaths.add(relativePath);
                } else {
                    if (fileInfo.getFilePath() != null) {
                        defectPaths.add(fileInfo.getFilePath());
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(defectPaths)) {
            TreeService treeService = treeServiceBizServiceFactory.createBizService(
                    toolNameList.get(0), ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
            TreeNodeVO treeNode = treeService.getTreeNode(taskIdList.get(0), defectPaths);
            response.setFilePathTree(treeNode);
        }

        List<String> sortedAuthors = finalAuthors.stream()
                .filter(StringUtils::isNotEmpty)
                .sorted(Collator.getInstance(Locale.SIMPLIFIED_CHINESE))
                .collect(Collectors.toList());
        response.setAuthorList(sortedAuthors);
        response.setCheckerList(handleCheckerList(toolNameList, checkerList, checkerSetId));

        return response;
    }

    @Override
    @Deprecated
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(List<Long> taskId, String toolName,
            String dimension, Set<String> statusSet, String checkerSet, String buildId,
            Boolean dataMigrationSuccessful) {
        // todo: 待清理
        return new QueryWarningPageInitRspVO();
    }

    @Override
    public Object pageInit(String projectId, DefectQueryReqVO request) {
        QueryWarningPageInitRspVO response = new QueryWarningPageInitRspVO();
        // 跨任务查询时，不执行聚合统计
        if (Boolean.TRUE.equals(request.getMultiTaskQuery())) {
            return response;
        }

        String buildId = request.getBuildId();
        String userId = request.getUserId();
        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        List<Long> taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(
                request.getTaskIdList(),
                request.getProjectId(),
                request.getUserId()
        );
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(), dimensionList, taskIdList, buildId
        );

        log.info("begin pageInit, task tool map: {}", taskToolMap);

        if (MapUtils.isEmpty(taskToolMap)) {
            return response;
        }

        // 规则集筛选
        Set<String> pkgChecker = getCheckers(
                request.getCheckerSet(), request.getChecker(),
                taskToolMap, dimensionList
        );

        // 规则不属于该规则集
        if (request.getCheckerSet() != null && CollectionUtils.isEmpty(pkgChecker)) {
            return response;
        }

        // 快照查询
        Pair<Set<String>, Set<String>> defectIdsPair = StringUtils.isNotEmpty(buildId)
                ? getDefectIdsPairByBuildId(taskToolMap, buildId)
                : Pair.of(Sets.newHashSet(), Sets.newHashSet());

        request.setSeverity(null);
        request.setDefectType(null);

        // 默认查询待修复的告警
        Set<String> condStatusList = request.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
            request.setStatus(condStatusList);
        }

        String statisticType = request.getStatisticType();

        boolean opsStatistic = ComConstants.StatisticType.OPERATE.name().equalsIgnoreCase(statisticType);
        boolean needIssueFilter = CollectionUtils.isNotEmpty(request.getOperates())
                && (request.getOperates().contains(ComConstants.CodeCCDefectOpsType.NO_OPS.name())
                || request.getOperates().contains(ComConstants.CodeCCDefectOpsType.ISSUE_SUBMIT.name()));
        // 如果查询包含告警操作筛选，需要加上issueIds的过滤
        if (opsStatistic || needIssueFilter) {
            Set<String> issueDefectIds = defectIssueService.getDefectIdByTaskIdAndToolMap(taskToolMap);
            request.setSubmitDefectIds(issueDefectIds);
        }

        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(statisticType)) {
            // 1.根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
            statisticByStatus(taskToolMap, request, pkgChecker, defectIdsPair, response, projectId, userId);
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(statisticType)) {
            // 2.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各严重级别告警数
            statisticBySeverity(taskToolMap, request, pkgChecker, defectIdsPair, response, projectId, userId);
        } else if (ComConstants.StatisticType.DEFECT_TYPE.name().equalsIgnoreCase(statisticType)) {
            // 3.根据规则、处理人、快照、路径、日期、状态过滤后计算: 新老告警数
            statisticByDefectType(taskToolMap, request, pkgChecker, defectIdsPair, response);
        } else if (ComConstants.StatisticType.OPERATE.name().equalsIgnoreCase(statisticType)) {
            // 4.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各个操作的告警数
            statisticByOps(taskToolMap, request, pkgChecker, defectIdsPair, response);
        } else {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(request));
        }

        return response;
    }

    protected void statisticBySeverity(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker,
            Pair<Set<String>, Set<String>> defectIdsPair,
            QueryWarningPageInitRspVO rspVO, String projectId, String userId
    ) {
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticBySeverity(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                projectId,
                userId
        );

        groups.forEach(it -> {
            if (ComConstants.SERIOUS == it.getSeverity()) {
                rspVO.setSeriousCount(rspVO.getSeriousCount() + it.getDefectCount());
            } else if (ComConstants.NORMAL == it.getSeverity()) {
                rspVO.setNormalCount(rspVO.getNormalCount() + it.getDefectCount());
            } else if (ComConstants.PROMPT_IN_DB == it.getSeverity() || ComConstants.PROMPT == it.getSeverity()) {
                rspVO.setPromptCount(rspVO.getPromptCount() + it.getDefectCount());
            }
        });
    }

    protected void statisticByDefectType(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO defectQueryReqVO,
            Set<String> pkgChecker,
            Pair<Set<String>, Set<String>> defectIdsPair,
            QueryWarningPageInitRspVO respVO
    ) {
        List<LintDefectGroupStatisticVO> newDefectGroupList = lintDefectV2Dao.statisticByDefectType(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.DefectType.NEW.value()
        );

        respVO.setNewCount(
                CollectionUtils.isNotEmpty(newDefectGroupList)
                        ? newDefectGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum()
                        : 0
        );

        List<LintDefectGroupStatisticVO> historyDefectGroupList = lintDefectV2Dao.statisticByDefectType(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.DefectType.HISTORY.value()
        );

        respVO.setHistoryCount(
                CollectionUtils.isNotEmpty(historyDefectGroupList)
                        ? historyDefectGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum()
                        : 0
        );
    }

    protected void statisticByOps(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO defectQueryReqVO,
            Set<String> pkgChecker,
            Pair<Set<String>, Set<String>> defectIdsPair,
            QueryWarningPageInitRspVO respVO
    ) {
        // 查询标记处理告警数
        List<LintDefectGroupStatisticVO> markGroupList = lintDefectV2Dao.statisticOps(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.CodeCCDefectOpsType.MARK
        );
        int markCount = CollectionUtils.isNotEmpty(markGroupList)
                ? markGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum() : 0;
        respVO.setMaskOpsCount(markCount);

        // 查询标记处理未修复告警数
        List<LintDefectGroupStatisticVO> markNotFixGroupList = lintDefectV2Dao.statisticOps(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.CodeCCDefectOpsType.MARK_NOT_FIXED
        );
        int markNotFixCount = CollectionUtils.isNotEmpty(markNotFixGroupList)
                ? markNotFixGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum() : 0;
        respVO.setMaskNotFixCount(markNotFixCount);

        // 查询评论总告警数
        List<LintDefectGroupStatisticVO> commentGroupList = lintDefectV2Dao.statisticOps(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.CodeCCDefectOpsType.COMMENT
        );
        int commentCount = CollectionUtils.isNotEmpty(commentGroupList)
                ? commentGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum() : 0;
        respVO.setCommentOpsCount(commentCount);

        // 查询提单总告警数
        List<LintDefectGroupStatisticVO> tapdGroupList = lintDefectV2Dao.statisticOps(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.CodeCCDefectOpsType.ISSUE_SUBMIT
        );
        int tapdCount = CollectionUtils.isNotEmpty(tapdGroupList)
                ? tapdGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum() : 0;
        respVO.setTapdOpsCount(tapdCount);

        // 查询未操作总告警数
        List<LintDefectGroupStatisticVO> allGroupList = lintDefectV2Dao.statisticOps(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                ComConstants.CodeCCDefectOpsType.NO_OPS
        );
        int notOpsCount = CollectionUtils.isNotEmpty(allGroupList)
                ? allGroupList.stream().mapToInt(LintDefectGroupStatisticVO::getDefectCount).sum() : 0;
        respVO.setNotOpsCount(notOpsCount);
    }

    protected void statisticByStatus(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker,
            Pair<Set<String>, Set<String>> defectIdsPair, QueryWarningPageInitRspVO rspVO,
            String projectId, String userId
    ) {
        Set<String> condStatusSet = defectQueryReqVO.getStatus();
        List<LintDefectGroupStatisticVO> groups = lintDefectV2Dao.statisticByStatus(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair,
                pkgChecker,
                projectId,
                userId
        );

        groups.forEach(it -> {
            int status = it.getStatus();

            if (DefectStatus.NEW.value() == status) {
                rspVO.setExistCount(rspVO.getExistCount() + it.getDefectCount());
            } else if ((DefectStatus.FIXED.value() & status) > 0) {
                rspVO.setFixCount(rspVO.getFixCount() + it.getDefectCount());
            } else if ((DefectStatus.IGNORE.value() & status) > 0) {
                rspVO.setIgnoreCount(rspVO.getIgnoreCount() + it.getDefectCount());
            } else if ((DefectStatus.PATH_MASK.value() & status) > 0
                    || (DefectStatus.CHECKER_MASK.value() & status) > 0) {
                rspVO.setMaskCount(rspVO.getMaskCount() + it.getDefectCount());
            }
        });

        // 若是快照查，则修正统计；快照查已移除"已修复"状态
        if (StringUtils.isNotEmpty(defectQueryReqVO.getBuildId())) {
            // 已忽略、已屏蔽在多分支下是共享的；而待修复与已修复是互斥的
            rspVO.setExistCount(rspVO.getExistCount() + rspVO.getFixCount());
            rspVO.setFixCount(0);
        }

        defectQueryReqVO.setStatus(condStatusSet);
    }

    /**
     * 获取规则类型
     *
     * @param toolNameSet
     * @param checkerSet
     * @return
     */
    private List<CheckerCustomVO> handleCheckerList(
            List<String> toolNameSet,
            Set<String> checkerList,
            String checkerSet
    ) {
        if (CollectionUtils.isEmpty(checkerList)) {
            return new ArrayList<>();
        }

        // 获取工具对应的所有警告类型 [初始化新增时一定要检查规则名称是否重复]
        Map<String, CheckerDetailVO> checkerDetailVOMap =
                multitoolCheckerService.queryAllCheckerI18NWrapper(toolNameSet, checkerSet, true)
                        .stream()
                        .collect(Collectors.toMap(CheckerDetailVO::getCheckerKey, Function.identity(), (k, v) -> v));

        if (MapUtils.isEmpty(checkerDetailVOMap)) {
            return Lists.newArrayList();
        }

        // 若规则集不为空，需要把告警对应的规则checkerList做一次交集处理，过滤掉规则集以外的规则
        if (StringUtils.isNotEmpty(checkerSet)) {
            checkerList.retainAll(checkerDetailVOMap.keySet());
        }

        // 一种规则类型有多个告警规则: <checkerType, List<checkerKey>>
        Map<String, List<String>> checkerTypeToKeyMap = Maps.newHashMapWithExpectedSize(checkerList.size());
        String checkerTypeCustom = I18NUtils.getMessage("CHECKER_TYPE_CUSTOM");

        for (String checker : checkerList) {
            CheckerDetailVO checkerDetailVO = checkerDetailVOMap.get(checker);
            String checkerType = (checkerDetailVO != null && StringUtils.isNotBlank(checkerDetailVO.getCheckerType()))
                    ? checkerDetailVO.getCheckerType() : checkerTypeCustom;

            List<String> checkerKeyList = checkerTypeToKeyMap.get(checkerType);
            if (CollectionUtils.isEmpty(checkerKeyList)) {
                checkerKeyList = Lists.newArrayList(checker);
                checkerTypeToKeyMap.put(checkerType, checkerKeyList);
            } else {
                checkerKeyList.add(checker);
            }
        }

        List<CheckerCustomVO> checkerCustomVOList = Lists.newArrayList();

        for (Entry<String, List<String>> kv : checkerTypeToKeyMap.entrySet()) {
            List<String> checkers = kv.getValue();
            String checkerType = kv.getKey();
            Collections.sort(checkers, Collator.getInstance(Locale.SIMPLIFIED_CHINESE));
            checkerCustomVOList.add(new CheckerCustomVO(checkerType, checkers));
        }

        return checkerCustomVOList;
    }

    private void getCheckerDetailAndType(LintDefectVO lintDefectVO, String toolName, String pattern) {
        String checker = lintDefectVO.getChecker();
        CheckerDetailVO checkerDetailVO = multitoolCheckerService.queryCheckerDetailWithI18N(toolName, checker);
        String checkerDesc = "";

        if (ComConstants.ToolPattern.LINT.name().equals(pattern)) {
            if (null != checkerDetailVO) {
                lintDefectVO.setCheckerType(checkerDetailVO.getCheckerType());
                checkerDesc = checkerDetailVO.getCheckerDesc();
            } else {
                lintDefectVO.setCheckerType(I18NUtils.getMessage("CHECKER_TYPE_CUSTOM"));
                checkerDesc = I18NUtils.getMessage("CHECKER_TYPE_DESC_CUSTOM");
            }
        } else {
            if (checkerDetailVO != null) {
                checkerDesc = checkerDetailVO.getCheckerDesc();
            }
        }

        lintDefectVO.setCheckerDetail(checkerDesc);
    }


    @Override
    public List<ToolDefectIdVO> queryDefectsByQueryCond(long taskId, DefectQueryReqVO request) {
        log.info("queryDefectsByQueryCond: taskId: {}, reqVO: {}", taskId, JsonUtil.INSTANCE.toJson(request));

        String buildId = request.getBuildId();
        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                dimensionList,
                Lists.newArrayList(taskId),
                buildId
        );

        if (MapUtils.isEmpty(taskToolMap)) {
            return Lists.newArrayList();
        }

        // 这里是单任务逻辑
        List<String> toolNameList = taskToolMap.entrySet().iterator().next().getValue();

        // 获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();
        // 获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(
                request.getPkgId(),
                toolNameList,
                taskDetailVO
        );
        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("id", true);
        filedMap.put("tool_name", true);

        Pair<Set<String>, Set<String>> defectIdsPair = getDefectIdsPairByBuildId(taskToolMap, buildId);
        log.info("pkgChecker {}, {}, {}", taskToolMap, pkgChecker, defectIdsPair);
        List<LintDefectV2Entity> defectEntityList = lintDefectV2Dao.findDefectByCondition(
                taskToolMap,
                request,
                defectIdsPair.getFirst(),
                pkgChecker,
                filedMap,
                defectIdsPair.getSecond()
        );

        log.info("defectEntityList size: {}", defectEntityList.size());
        return defectEntityList.stream()
                .map(item -> new ToolDefectIdVO(item.getTaskId(), item.getToolName(), item.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public Pair<Set<String>, Set<String>> getDefectIdsPairByBuildId(
            Map<Long, List<String>> taskToolMap,
            String buildId
    ) {
        Pair<Set<String>, Set<String>> retEmpty = Pair.of(Sets.newHashSet(), Sets.newHashSet());
        if (StringUtils.isEmpty(buildId) || MapUtils.isEmpty(taskToolMap)) {
            return retEmpty;
        }

        if (taskToolMap.size() > 1) {
            throw new IllegalArgumentException("build id must be empty when task list size more than 1");
        }

        // 只有单任务支持快照
        Entry<Long, List<String>> firstEntry = taskToolMap.entrySet().iterator().next();
        Long taskId = firstEntry.getKey();
        List<String> toolNameList = firstEntry.getValue();

        // 筛出成功执行的工具
        Map<String, Boolean> commitResult = taskLogService.defectCommitSuccess(
                taskId, toolNameList,
                buildId, getSubmitStepNum()
        );
        Set<String> successTools = commitResult.entrySet().stream()
                .filter(Entry::getValue)
                .map(Entry::getKey)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(successTools)) {
            return retEmpty;
        }

        // 迁移的工具，唯一标识均为第三方平台特有的Id
        Set<String> migrationTools = commonDefectMigrationService.matchToolNameSet();
        List<BuildDefectV2Entity> buildDefectV2List =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndToolNameIn(taskId, buildId, successTools);

        Set<String> mongoIds = Sets.newHashSet();
        Set<String> thirdPartyIds = Sets.newHashSet();

        for (BuildDefectV2Entity buildDefectV2Entity : buildDefectV2List) {
            if (migrationTools.contains(buildDefectV2Entity.getToolName())) {
                thirdPartyIds.add(buildDefectV2Entity.getDefectId());
            } else {
                mongoIds.add(buildDefectV2Entity.getDefectId());
            }
        }

        return Pair.of(mongoIds, thirdPartyIds);
    }

    /**
     * 根据工具列表、维度获取规则
     *
     * @param checkerSet 规则集
     * @param checker 当checkerSet不为空时，才会校验checker归属
     * @param toolNameList 任务与工具映射
     * @param dimensionList
     * @return
     */
    @Override
    public Set<String> getCheckers(
            CheckerSet checkerSet,
            String checker,
            List<String> toolNameList,
            List<String> dimensionList
    ) {
        String checkerSetId = checkerSet != null ? checkerSet.getCheckerSetId() : null;
        Integer checkerSetVersion = checkerSet != null ? checkerSet.getVersion() : null;

        return getCheckers(checkerSetId, checkerSetVersion, checker, toolNameList, dimensionList);
    }

    private Set<String> getCheckers(
            CheckerSet checkerSet,
            String checker,
            Map<Long, List<String>> taskToolMap,
            List<String> dimensionList
    ) {
        List<String> toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        return getCheckers(checkerSet, checker, toolNameList, dimensionList);
    }

    private Set<String> getCheckers(
            String checkerSetId,
            Integer checkerSetVersion,
            String checker,
            List<String> toolNameList,
            List<String> dimensionList
    ) {
        return getCheckerDetails(checkerSetId, checkerSetVersion, checker, toolNameList, dimensionList).stream()
                .map(CheckerDetailVO::getCheckerKey).collect(Collectors.toSet());
    }

    @Override
    public List<CheckerDetailVO> getCheckerDetails(CheckerSet checkerSet, String checker, List<String> toolNameList,
            List<String> dimensionList) {
        String checkerSetId = checkerSet != null ? checkerSet.getCheckerSetId() : null;
        Integer checkerSetVersion = checkerSet != null ? checkerSet.getVersion() : null;
        return getCheckerDetails(checkerSetId, checkerSetVersion, checker, toolNameList, dimensionList);
    }

    private List<CheckerDetailVO> getCheckerDetails(
            String checkerSetId,
            Integer checkerSetVersion,
            String checker,
            List<String> toolNameList,
            List<String> dimensionList
    ) {
        List<String> checkerCategoryList = ParamUtils.getCheckerCategoryListByDimensionList(dimensionList);
        List<String> checkerListCondition = Lists.newArrayList();
        Set<String> checkersByCheckerSetId = null;

        if (StringUtils.isNotEmpty(checkerSetId)) {
            if (checkerSetVersion != null) {
                CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                        checkerSetId,
                        checkerSetVersion
                );

                if (checkerSetEntity == null || CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps())) {
                    checkersByCheckerSetId = Sets.newHashSet();
                } else {
                    checkersByCheckerSetId = checkerSetEntity.getCheckerProps().stream()
                            .map(CheckerPropsEntity::getCheckerKey)
                            .collect(Collectors.toSet());
                }

            } else {
                checkersByCheckerSetId = checkerSetRepository.findByCheckerSetId(checkerSetId).stream()
                        .filter(x -> CollectionUtils.isNotEmpty(x.getCheckerProps()))
                        .flatMap(y -> y.getCheckerProps().stream())
                        .map(CheckerPropsEntity::getCheckerKey)
                        .collect(Collectors.toSet());
            }

            if (CollectionUtils.isEmpty(checkersByCheckerSetId)) {
                return Lists.newArrayList();
            }

            checkerListCondition.addAll(checkersByCheckerSetId);
        }

        // 若前端传入的规则不为空
        if (StringUtils.isNotEmpty(checker)) {
            if (CollectionUtils.isNotEmpty(checkersByCheckerSetId) && !checkersByCheckerSetId.contains(checker)) {
                log.error("checker does not belong this checker set: {}, {}, {}",
                        checkerSetId, checkerSetVersion, checker);

                return Lists.newArrayList();
            }

            checkerListCondition.clear();
            checkerListCondition.add(checker);
        }

        return checkerDetailDao.findByToolNameInAndCheckerCategory(
                toolNameList,
                checkerCategoryList,
                checkerListCondition
        ).stream().map(it -> {
            CheckerDetailVO vo = new CheckerDetailVO();
            vo.setCheckerKey(it.getCheckerKey());
            vo.setToolName(it.getToolName());
            vo.setCheckerCategory(it.getCheckerCategory());
            vo.setPublisher(it.getPublisher());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ToolDefectPageVO queryDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO reqVO, Integer pageNum,
                                                            Integer pageSize) {
        log.warn("queryDefectsByQueryCond is unimplemented: taskId: {} reqVO: {}, pageNum: {}, pageSize: {}",
                taskId, reqVO, pageNum, pageSize);

        LintDefectQueryRspVO repVo = (LintDefectQueryRspVO) processQueryWarningRequestCore(reqVO,
                pageNum, pageSize, "fileName", Direction.ASC);

        List<LintDefectVO> defectEntityList = repVo.getDefectList().getRecords();
        log.info("toolDefectIdVOS {}", repVo);

        List<String> ids = defectEntityList.stream().map(LintDefectVO::getId).collect(Collectors.toList());
        log.info("ids {}", ids);
        return idListToToolDefectPageVO(
                taskId, reqVO.getToolNameList().stream().map(String::valueOf).collect(Collectors.joining(",")),
                ids, pageNum, pageSize, repVo.getDefectList().getCount());
    }

    @Override
    public List<GrayDefectStaticVO> getGaryDefectStaticList(GrayBuildNumAndTaskVO grayBuildNumAndTaskVO) {
        return lintStatisticDao.getByGrayDefectCountData(grayBuildNumAndTaskVO.getTaskList(),
                grayBuildNumAndTaskVO.getBuildIds());
    }


    /**
     * lint从scmFiLe中获取代码提交人
     *
     * @param taskId 任务id
     * @param toolName 工具名称
     * @param lintDefectV2Entity 告警
     * @return 提交人
     */
    private String getCommitAuthor(long taskId, String toolName, LintDefectV2Entity lintDefectV2Entity) {
        int defectLine = lintDefectV2Entity.getLineNum();
        String commitAuthor = "";
        ScmFileInfoCacheEntity scmFileInfoCacheEntity =
                scmFileInfoCacheRepository.findFirstByTaskIdAndToolNameAndFileRelPath(taskId, toolName,
                        lintDefectV2Entity.getRelPath());
        if (scmFileInfoCacheEntity != null) {
            List<ScmFileInfoCacheEntity.ScmBlameChangeRecordVO> records = scmFileInfoCacheEntity.getChangeRecords();
            // 从记录中过滤出line_update_time = records中的line提交的提交人
            if (CollectionUtils.isNotEmpty(records)) {
                // 告警中的行号为0的改成1
                if (defectLine == 0) {
                    defectLine = 1;
                }
                for (ScmFileInfoCacheEntity.ScmBlameChangeRecordVO changeRecord : records) {
                    boolean isFound = false;
                    List<Object> lines = changeRecord.getLines();
                    if (lines != null && !lines.isEmpty()) {
                        for (Object line : lines) {
                            if (line instanceof Integer && defectLine == (int) line) {
                                isFound = true;
                            } else if (line instanceof List) {
                                List<Integer> lineScope = (List<Integer>) line;
                                if (CollectionUtils.isNotEmpty(lineScope) && lineScope.size() > 1) {
                                    if (lineScope.get(0) <= defectLine
                                            && lineScope.get(lineScope.size() - 1) >= defectLine) {
                                        isFound = true;
                                    }
                                }
                            }
                            if (isFound) {
                                commitAuthor = ToolParamUtils.trimUserName(changeRecord.getAuthor());
                                break;
                            }
                        }
                    }
                    if (isFound) {
                        break;
                    }
                }
            }
        }
        if (StringUtils.isBlank(commitAuthor)) {
            commitAuthor = StringUtils.join(lintDefectV2Entity.getAuthor(), TOOL_NAMES_SEPARATOR);
        }
        return commitAuthor;
    }

    private Set<String> hasCodeCommonDefectId(List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return Collections.emptySet();
        }
        List<String> defectIds = defectList.stream().map(LintDefectV2Entity::getEntityId)
                .collect(Collectors.toList());
        List<Long> taskIds = defectList.stream().map(LintDefectV2Entity::getTaskId).distinct()
                .collect(Collectors.toList());
        return lintDefectV2Dao.filterHasCodeCommonDefectId(taskIds, defectIds);
    }
}


