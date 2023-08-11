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

package com.tencent.bk.codecc.defect.resources;

import static com.tencent.devops.common.constant.ComConstants.CODE_COMMENT_ADD;
import static com.tencent.devops.common.constant.ComConstants.CODE_COMMENT_DEL;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CODE_COMMENT_ADD;
import static com.tencent.devops.common.constant.ComConstants.FUNC_CODE_COMMENT_DEL;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.UserDefectRestResource;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.FileDefectGatherService;
import com.tencent.bk.codecc.defect.service.IDefectOperateBizService;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.IStatQueryWarningService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.impl.CLOCQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessRspVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.ListToolNameRequest;
import com.tencent.bk.codecc.defect.vo.ListToolNameResponse;
import com.tencent.bk.codecc.defect.vo.ListToolNameResponse.ToolBase;
import com.tencent.bk.codecc.defect.vo.QueryCheckersAndAuthorsRequest;
import com.tencent.bk.codecc.defect.vo.QueryFileDefectGatherRequest;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.defect.vo.StatDefectQueryRespVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO_Old;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BusinessType;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import com.tencent.devops.common.web.condition.CommunityCondition;
import com.tencent.devops.common.web.security.AuthMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;

/**
 * 告警查询服务实现
 */
@Slf4j
@RestResource
@Conditional(CommunityCondition.class)
@AuthMethod(permission = {CodeCCAuthAction.DEFECT_VIEW})
public class UserDefectRestResourceImpl implements UserDefectRestResource {

    @Autowired
    CLOCQueryWarningBizServiceImpl clocQueryWarningBizService;
    @Autowired
    IStatQueryWarningService iStatQueryWarningService;
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;
    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;
    @Autowired
    private BizServiceFactory<IDefectOperateBizService> defectOperateBizServiceFactory;
    @Autowired
    private TaskLogService taskLogService;
    @Autowired
    private AuthExPermissionApi authExPermissionApi;
    @Autowired
    private FileDefectGatherService fileDefectGatherService;
    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private IIgnoreTypeService iIgnoreTypeService;
    @Autowired
    private ToolMetaCacheService toolMetaCache;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private Client client;

    @Override
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            String userId, Long taskId, String toolName,
            String status, String buildId, String projectId
    ) {
        // 目前在用：CCN、DUPC
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(
                toolName,
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        Set<String> statusSet = Sets.newHashSet(List2StrUtil.fromString(status, ComConstants.STRING_SPLIT));

        return new Result<>(
                // 兼容老版本接口，当时还没有跨任务，所以multiTaskQuery固定为false
                queryWarningBizService.processQueryWarningPageInitRequest(
                        userId,
                        taskId == null ? Lists.newArrayList() : Lists.newArrayList(taskId),
                        Lists.newArrayList(toolName),
                        null,
                        statusSet,
                        null,
                        buildId,
                        projectId,
                        false
                )
        );
    }

    @Override
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            Long taskId, String toolName, String dimension,
            String status, String checkerSet, String buildId
    ) {
        List<Long> taskIdList = Lists.newArrayList(taskId);
        Pair<List<String>, List<String>> pair = ParamUtils.parseToolNameAndDimensions(toolName, dimension);
        List<String> toolNameList = pair.getFirst();
        List<String> dimensionList = pair.getSecond();
        List<String> statusList = List2StrUtil.fromString(status, ComConstants.STRING_SPLIT);
        // 兼容老版本接口，当时还没有跨任务，所以multiTaskQuery固定为false
        QueryCheckersAndAuthorsRequest request = new QueryCheckersAndAuthorsRequest(
                taskIdList, toolNameList, dimensionList,
                statusList, checkerSet, buildId, false
        );

        return queryCheckersAndAuthors("", "", request);
    }

    @Override
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(
            String userId,
            String projectId,
            QueryCheckersAndAuthorsRequest request
    ) {
        List<String> statusList = request.getStatusList();
        Set<String> statusSet = CollectionUtils.isEmpty(statusList) ? Sets.newHashSet() : Sets.newHashSet(statusList);
        List<String> toolNameList = request.getToolNameList();
        List<String> dimensionList = request.getDimensionList();
        List<Long> taskIdList = request.getTaskIdList();
        String checkerSet = request.getCheckerSet();
        String buildId = request.getBuildId();
        boolean isMultiTaskQuery = Boolean.TRUE.equals(request.getMultiTaskQuery());

        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                toolNameList,
                dimensionList,
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        QueryWarningPageInitRspVO response = service.processQueryWarningPageInitRequest(
                userId,
                taskIdList,
                toolNameList,
                dimensionList,
                statusSet,
                checkerSet,
                buildId,
                projectId,
                isMultiTaskQuery
        );

        return new Result<>(response);
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryDefectList(
            String userId, long taskId, DefectQueryReqVO_Old requestVO,
            int pageNum, int pageSize, String sortField, Direction sortType
    ) {
        DefectQueryReqVO newReq = new DefectQueryReqVO();
        BeanUtils.copyProperties(requestVO, newReq);
        Pair<List<String>, List<String>> pair = ParamUtils.parseToolNameAndDimensions(
                requestVO.getToolName(),
                requestVO.getDimension()
        );

        newReq.setToolNameList(pair.getFirst());
        newReq.setDimensionList(pair.getSecond());
        newReq.setTaskIdList(Lists.newArrayList(taskId));
        newReq.setUserId(userId);

        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                newReq.getToolNameList(),
                newReq.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        return new Result<>(
                service.processQueryWarningRequest(
                        taskId, newReq,
                        pageNum, pageSize, sortField, sortType
                )
        );
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryDefectListWithIssue(String userId, String projectId,
            DefectQueryReqVO defectQueryReqVO, int pageNum, int pageSize, String sortField, Direction sortType) {

        defectQueryReqVO.setProjectId(projectId);
        defectQueryReqVO.setUserId(userId);

        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                defectQueryReqVO.getToolNameList(),
                defectQueryReqVO.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        long taskId = CollectionUtils.isEmpty(defectQueryReqVO.getTaskIdList()) ? 0L
                : defectQueryReqVO.getTaskIdList().get(0);


        return new Result<>(
                service.processQueryWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType)
        );
    }

    @Override
    public Result<CommonDefectDetailQueryRspVO> queryDefectDetail(
            Long taskId,
            String userId,
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            String sortField,
            Sort.Direction sortType
    ) {
        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                Lists.newArrayList(commonDefectDetailQueryReqVO.getToolName()),
                Lists.newArrayList(commonDefectDetailQueryReqVO.getDimension()),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        return new Result<>(
                service.processQueryWarningDetailRequest(
                        taskId, userId, commonDefectDetailQueryReqVO, sortField, sortType
                )
        );
    }

    @Override
    public Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithIssue(Long taskId, String userId,
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO, String sortField, Direction sortType) {
        return queryDefectDetail(taskId, userId, commonDefectDetailQueryReqVO, sortField, sortType);
    }


    @Override
    public Result<CommonDefectDetailQueryRspVO> getFileContentSegment(long taskId, String userId,
            GetFileContentSegmentReqVO getFileContentSegmentReqVO) {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(getFileContentSegmentReqVO.getToolName(),
                        getFileContentSegmentReqVO.getDimension(),
                        ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(
                queryWarningBizService.processGetFileContentSegmentRequest(taskId, userId, getFileContentSegmentReqVO));
    }


    @Override
    @AuthMethod(permission = {CodeCCAuthAction.DEFECT_MANAGE})
    public Result<List<BatchDefectProcessRspVO>> batchDefectProcess(String projectId, String userName,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        batchDefectProcessReqVO.setUserName(userName);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        batchDefectProcessReqVO.setProjectId(projectId);
        String bizType = batchDefectProcessReqVO.getBizType();
        if (StringUtils.isBlank(bizType)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"bizType"});
        }
        List<BatchDefectProcessRspVO> rspVOS = new ArrayList<>();
        // 如果是忽略并标记处理，需要将revertAndMark置为true。让标记处理可以先标记忽略的告警
        if (bizType.contains(BusinessType.REVERT_IGNORE.value())
                && bizType.contains(BusinessType.MARK_DEFECT.value())) {
            log.info("batchDefectProcess with revert And mark");
            batchDefectProcessReqVO.setRevertAndMark(true);
        }
        String[] bizTypes = bizType.split("\\|");
        for (String type : bizTypes) {
            // 如果是标记处理，先看下是否需要恢复忽略
            BatchDefectProcessReqVO reqVO = new BatchDefectProcessReqVO();
            BeanUtils.copyProperties(batchDefectProcessReqVO, reqVO);
            reqVO.setBizType(type.trim());
            Result<Long> result = singleBizTypeBatchProcess(userName, reqVO);
            if (result.isNotOk()) {
                return new Result<>(result.getStatus(), result.getCode(), result.getMessage(), null);
            }
            rspVOS.add(new BatchDefectProcessRspVO(reqVO.getBizType(), result.getData()));
        }
        return new Result<>(rspVOS);
    }

    private Result<Long> singleBizTypeBatchProcess(String userName,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        batchDefectProcessReqVO.setIgnoreAuthor(userName);

        IBizService<BatchDefectProcessReqVO> service;
        String bizType = batchDefectProcessReqVO.getBizType();

        // 该值为true，必定是新版lint的请求
        service = bizServiceFactory.createBizService(
                batchDefectProcessReqVO.getToolNameList(),
                batchDefectProcessReqVO.getDimensionList(),
                ComConstants.BATCH_PROCESSOR_INFIX + bizType,
                IBizService.class
        );
        return service.processBiz(batchDefectProcessReqVO);
    }

    @Override
    public Result<List<BuildVO>> queryBuildInfos(Long taskId) {
        return new Result<>(taskLogService.getTaskBuildInfos(taskId));
    }

    @Override
    public Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(Long taskId) {
        return new Result<>(buildSnapshotService.getRecentBuildSnapshotSummary(taskId));
    }

    @Override
    public Result<DeptTaskDefectRspVO> queryDeptTaskDefect(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }

        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(deptTaskDefectReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processDeptTaskDefectReq(deptTaskDefectReqVO));
    }

    @Override
    @OperationHistory(funcId = FUNC_CODE_COMMENT_ADD, operType = CODE_COMMENT_ADD)
    @AuthMethod(permission = {CodeCCAuthAction.DEFECT_MANAGE})
    public Result<Boolean> addCodeComment(
            String defectId, String toolName, String commentId, String userName,
            SingleCommentVO singleCommentVO, String fileName, String nameCn,
            String checker, String projectId, String taskId
    ) {
        IDefectOperateBizService service = getServiceForCodeCommentBiz(toolName);
        service.addCodeComment(
                defectId, toolName, commentId, userName, singleCommentVO,
                fileName, nameCn, checker, projectId, taskId
        );

        return new Result<>(true);
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.DEFECT_MANAGE})
    public Result<Boolean> updateCodeComment(
            String commentId, String userName, String toolName,
            SingleCommentVO singleCommentVO
    ) {
        IDefectOperateBizService service = getServiceForCodeCommentBiz(toolName);
        service.updateCodeComment(commentId, userName, singleCommentVO);

        return new Result<>(true);
    }

    @Override
    @OperationHistory(funcId = FUNC_CODE_COMMENT_DEL, operType = CODE_COMMENT_DEL)
    @AuthMethod(permission = {CodeCCAuthAction.DEFECT_MANAGE})
    public Result<Boolean> deleteCodeComment(
            String commentId, String singleCommentId, String toolName,
            String userName, String entityId, String comment
    ) {
        IDefectOperateBizService service = getServiceForCodeCommentBiz(toolName);
        service.deleteCodeComment(commentId, singleCommentId, userName);

        return new Result<>(true);
    }

    @Override
    public Result<FileDefectGatherVO> queryFileDefectGather(long taskId, String toolName) {
        return new Result<>(
                fileDefectGatherService.getFileDefectGather(
                        new HashMap<Long, List<String>>() {{
                            put(taskId, Lists.newArrayList(toolName));
                        }}
                )
        );
    }

    @Override
    public Result<FileDefectGatherVO> queryFileDefectGather(
            String userId,
            String projectId,
            QueryFileDefectGatherRequest request
    ) {
        List<Long> taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(request.getTaskIdList(), projectId, userId);
        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                dimensionList,
                taskIdList,
                request.getBuildId()
        );

        return new Result<>(fileDefectGatherService.getFileDefectGather(taskToolMap));
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryCLOCList(
            String userId, long taskId, String toolName, ComConstants.CLOCOrder orderBy
    ) {
        if (!ComConstants.Tool.CLOC.name().equalsIgnoreCase(toolName)
                && !ComConstants.Tool.SCC.name().equalsIgnoreCase(toolName)) {
            throw new IllegalArgumentException("tool name must be CLOC or SCC");
        }

        DefectQueryReqVO defectQueryReqVO = new DefectQueryReqVO();
        defectQueryReqVO.setToolNameList(Lists.newArrayList(toolName));
        defectQueryReqVO.setOrder(orderBy);
        defectQueryReqVO.setUserId(userId);

        return new Result<>(
                clocQueryWarningBizService.processQueryWarningRequest(
                        taskId, defectQueryReqVO,
                        0, 0, null, null
                )
        );
    }

    @Override
    public Result<Object> pageInit(String userId, String projectId, DefectQueryReqVO defectQueryReqVO) {
        defectQueryReqVO.setProjectId(projectId);
        defectQueryReqVO.setUserId(userId);

        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                defectQueryReqVO.getToolNameList(),
                defectQueryReqVO.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        return new Result<>(service.pageInit(projectId, defectQueryReqVO));
    }

    @Override
    public Result<List<StatDefectQueryRespVO>> queryStatList(long taskId, String toolName, long startTime,
            long endTime) {
        if (StringUtils.isBlank(toolName)) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME);
        }
        return new Result<>(iStatQueryWarningService.processQueryWarningRequest(taskId, toolName, startTime, endTime));
    }

    @Override
    @I18NResponse
    public Result<ListToolNameResponse> listToolName(
            String userId,
            String projectId,
            ListToolNameRequest request
    ) {
        List<Long> taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(request.getTaskIdList(), projectId, userId);
        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        List<String> toolNameList = ParamUtils.listToolNameForFrontend(
                dimensionList,
                taskIdList,
                request.getBuildId()
        );

        ListToolNameResponse resp = new ListToolNameResponse();

        for (String toolName : toolNameList) {
            ToolMetaBaseVO toolMetaBaseVO = toolMetaCache.getToolBaseMetaCache(toolName);
            if (toolMetaBaseVO == null) {
                continue;
            }

            resp.add(new ToolBase(toolMetaBaseVO.getEntityId(), toolName, toolMetaBaseVO.getDisplayName()));
        }

        return new Result<>(resp);
    }

    @Override
    public Result<Boolean> commonToLintMigrationSuccessful(long taskId) {
        return new Result<>(commonDefectMigrationService.isMigrationSuccessful(taskId));
    }

    @Override
    public Result<Long> getNewDefectFileCount(CountDefectFileRequest request) {
        IQueryWarningBizService service;

        if (CollectionUtils.isEmpty(request.getDimensionList())
                || request.getDimensionList().contains(ToolType.DEFECT.name())
                || request.getDimensionList().contains(ToolType.SECURITY.name())
                || request.getDimensionList().contains(ToolType.STANDARD.name())) {
            service = fileAndDefectQueryFactory.createBizService(
                    "",
                    ToolType.STANDARD.name(),
                    ComConstants.BusinessType.QUERY_WARNING.value(),
                    IQueryWarningBizService.class
            );
        } else if (request.getDimensionList().contains(ToolType.CCN.name())) {
            service = fileAndDefectQueryFactory.createBizService(
                    ToolType.CCN.name(),
                    "",
                    ComConstants.BusinessType.QUERY_WARNING.value(),
                    IQueryWarningBizService.class
            );
        } else if (request.getDimensionList().contains(ToolType.DUPC.name())) {
            service = fileAndDefectQueryFactory.createBizService(
                    ToolType.DUPC.name(),
                    "",
                    ComConstants.BusinessType.QUERY_WARNING.value(),
                    IQueryWarningBizService.class
            );
        } else {
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return new Result<>(service.countNewDefectFile(request));
    }

    private IQueryWarningBizService getServiceForQueryWarningBiz(DefectQueryReqVO vo) {
        return fileAndDefectQueryFactory.createBizService(
                vo.getToolNameList(),
                vo.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );
    }

    private IQueryWarningBizService getServiceForQueryWarningBiz(CommonDefectDetailQueryReqVO vo) {
        return fileAndDefectQueryFactory.createBizService(
                vo.getToolName(),
                vo.getDimension(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );
    }

    private IDefectOperateBizService getServiceForCodeCommentBiz(String toolName) {
        // 评论功能目前只有圈复杂度以及问题管理三大维度LINT
        if (StringUtils.isEmpty(toolName)) {
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        if (Tool.CCN.name().equalsIgnoreCase(toolName)) {
            return defectOperateBizServiceFactory.createBizService(
                    toolName, ComConstants.BusinessType.DEFECT_OPERATE.value(),
                    IDefectOperateBizService.class
            );
        } else {
            return defectOperateBizServiceFactory.createBizService(
                    "", ToolType.STANDARD.name(), ComConstants.BusinessType.DEFECT_OPERATE.value(),
                    IDefectOperateBizService.class
            );
        }
    }
}
