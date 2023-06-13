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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.MongoExecutionTimeoutException;
import com.tencent.bk.codecc.defect.component.QueryWarningLogicComponent;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeFileUrlRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.file.ScmFileInfoSnapshotRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.TaskPersonalStatisticDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.file.ScmFileInfoSnapshotDao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.issue.DefectIssueInfoEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.CommonQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.LintQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCustomVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.DefectFilesInfoVO;
import com.tencent.bk.codecc.defect.vo.DefectInstanceVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailVO;
import com.tencent.bk.codecc.defect.vo.LintDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.TreeNodeVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVOBase.CheckerSet;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
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
import com.tencent.devops.common.service.utils.PageableUtils;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
            } catch (CodeCCException | UncategorizedMongoDbException e) {
                CodeCCException tipsEx = new CodeCCException(
                        CommonMessageCode.PROJECT_DEFECT_TOO_MANY,
                        "当前项目问题数过于巨大，请筛选工具后查看",
                        e
                );

                if (e instanceof UncategorizedMongoDbException) {
                    if (e.getCause() instanceof MongoExecutionTimeoutException) {
                        throw tipsEx;
                    } else {
                        log.error("mongodb uncategorized ex", e);
                        throw e;
                    }
                }

                throw tipsEx;
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
        // 前端不强制传入taskId
        taskId = defectEntity.getTaskId();

        if (defectEntity == null) {
            log.info("defect not found by condition: {}", requestVO);
            return responseVO;
        }

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
            String url = PathUtils.getFileUrl(
                    defectEntity.getUrl(), defectEntity.getBranch(), defectEntity.getRelPath()
            );
            responseVO.setRelativePath(relativePath);
            responseVO.setFilePath(StringUtils.isEmpty(url) ? defectEntity.getFilePath() : url);
            responseVO.setFileName(defectEntity.getFileName());
        }

        responseVO.setLintDefectDetailVO(lintDefectDetailVO);

        return responseVO;
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
                if (StringUtils.isEmpty(buildId)) {
                    ToolBuildInfoEntity buildInfo =
                            toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId,
                                    lintDefectDetailVO.getToolName());
                    if (buildInfo != null) {
                        buildId = buildInfo.getDefectBaseBuildId();
                    }
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
            String projectId = taskDetailVO.getProjectId();
            String url = defectEntity.getUrl();
            String repoId = defectEntity.getRepoId();
            String revision = defectEntity.getRevision();
            String branch = defectEntity.getBranch();
            String subModule = defectEntity.getSubModule();

            fileMap.forEach((fileMd5, fileInfoVO) -> {
                String content = null;
                if (!fileInfoVO.getFilePath().equals(lintDefectDetailVO.getFilePath())) {
                    ScmFileInfoSnapshotEntity snapshot = notMainFileScmSnapshotMap.get(fileInfoVO.getFilePath());
                    if (snapshot != null) {
                        content = getFileContent(taskId, projectId, userId,
                                snapshot.getUrl(),
                                snapshot.getRepoId(),
                                snapshot.getRelPath(),
                                snapshot.getRevision(),
                                snapshot.getBranch(),
                                snapshot.getSubModule());
                    }
                } else {
                    content = getFileContent(taskId, projectId, userId,
                            url, repoId, relPath, revision, branch, subModule);
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
        return filedMap;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            List<Long> taskIdList,
            List<String> toolNameList,
            List<String> dimensionList,
            Set<String> statusSet,
            String checkerSetId,
            String buildId,
            String projectId,
            boolean isMultiTaskQuery
    ) {
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

        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(statisticType)) {
            // 1.根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
            statisticByStatus(taskToolMap, request, pkgChecker, defectIdsPair, response, projectId, userId);
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(statisticType)) {
            // 2.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各严重级别告警数
            statisticBySeverity(taskToolMap, request, pkgChecker, defectIdsPair, response, projectId, userId);
        } else if (ComConstants.StatisticType.DEFECT_TYPE.name().equalsIgnoreCase(statisticType)) {
            // 3.根据规则、处理人、快照、路径、日期、状态过滤后计算: 新老告警数
            statisticByDefectType(taskToolMap, request, pkgChecker, defectIdsPair, response);
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
            Map<Long,List<String>> taskToolMap,
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

    protected void statisticByStatus(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO, Set<String> pkgChecker,
            Pair<Set<String>, Set<String>> defectIdsPair, QueryWarningPageInitRspVO rspVO,
            String projectId,String userId
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
                multitoolCheckerService.queryAllCheckerWithI18N(toolNameSet, checkerSet, true)
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
        Set<String> conditionDefectType = request.getDefectType();

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
                return Sets.newHashSet();
            }

            checkerListCondition.addAll(checkersByCheckerSetId);
        }

        // 若前端传入的规则不为空
        if (StringUtils.isNotEmpty(checker)) {
            if (CollectionUtils.isNotEmpty(checkersByCheckerSetId) && !checkersByCheckerSetId.contains(checker)) {
                log.error("checker does not belong this checker set: {}, {}, {}",
                        checkerSetId, checkerSetVersion, checker);

                return Sets.newHashSet();
            }

            checkerListCondition.clear();
            checkerListCondition.add(checker);
        }

        return checkerDetailDao.findByToolNameInAndCheckerCategory(
                toolNameList,
                checkerCategoryList,
                checkerListCondition
        );
    }

    @Deprecated
    private Set<String> getPkgChecker(DefectQueryReqVO reqVO, List<String> toolNameList, TaskDetailVO taskDetailVO) {
        Set<String> pkgChecker = Sets.newHashSet();
        Set<String> checkersByCheckerSetId = null;

        if (StringUtils.isNotEmpty(reqVO.getPkgId())) {
            pkgChecker.addAll(
                    multitoolCheckerService.queryPkgRealCheckers(reqVO.getPkgId(), toolNameList, taskDetailVO)
            );
        }

        DefectQueryReqVO.CheckerSet queryCheckerSet = reqVO.getCheckerSet();
        if (queryCheckerSet != null) {
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                    queryCheckerSet.getCheckerSetId(),
                    queryCheckerSet.getVersion()
            );

            if (checkerSetEntity == null || CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps())) {
                return pkgChecker;
            }

            checkersByCheckerSetId = checkerSetEntity.getCheckerProps().stream()
                    .filter(x -> toolNameList.contains(x.getToolName()))
                    .map(CheckerPropsEntity::getCheckerKey)
                    .collect(Collectors.toSet());

            if (CollectionUtils.isEmpty(checkersByCheckerSetId)) {
                return Sets.newHashSet();
            }

            pkgChecker.addAll(checkersByCheckerSetId);
        }

        String checker = reqVO.getChecker();
        if (StringUtils.isNotEmpty(checker)) {
            if (CollectionUtils.isNotEmpty(checkersByCheckerSetId) && !checkersByCheckerSetId.contains(checker)) {
                log.error("checker does not belong this checker set: {}, {}, {}, {}",
                        taskDetailVO.getTaskId(),
                        queryCheckerSet.getCheckerSetId(),
                        queryCheckerSet.getVersion(),
                        checker
                );

                return Sets.newHashSet();
            }

            pkgChecker.clear();
            pkgChecker.add(checker);
        }

        return pkgChecker;
    }

    @Deprecated
    private Set<String> getDefectIds(Long taskId, List<String> toolNameSet, String buildId) {
        Set<String> defectIdSet = new HashSet<>();
        Map<String, Boolean> toolDefectResultMap = taskLogService.defectCommitSuccess(taskId,
                toolNameSet, buildId, ComConstants.Step4MutliTool.COMMIT.value());
        List<String> successTools = toolDefectResultMap.entrySet().stream().filter(Map.Entry::getValue)
                .map(Map.Entry::getKey).collect(Collectors.toList());
        List<BuildDefectV2Entity> buildDefectV2Entities =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndToolNameIn(taskId, buildId, successTools);
        if (CollectionUtils.isNotEmpty(buildDefectV2Entities)) {
            for (BuildDefectV2Entity buildDefectV2Entity : buildDefectV2Entities) {
                defectIdSet.add(buildDefectV2Entity.getDefectId());
            }
        }
        //如果新的快照查询不到，查询老的快照
        if (defectIdSet.isEmpty()) {
            List<BuildDefectEntity> buildFiles =
                    buildDefectRepository.findByTaskIdAndToolNameInAndBuildId(taskId, successTools, buildId);
            if (CollectionUtils.isNotEmpty(buildFiles)) {
                for (BuildDefectEntity buildDefectEntity : buildFiles) {
                    defectIdSet.addAll(buildDefectEntity.getFileDefectIds());
                }
            }
        }
        return defectIdSet;
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
}


