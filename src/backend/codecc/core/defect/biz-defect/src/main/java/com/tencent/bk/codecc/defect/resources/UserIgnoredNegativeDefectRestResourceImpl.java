package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserIgnoredNegativeDefectRestResource;
import com.tencent.bk.codecc.defect.auth.ToolDeveloperExtAuth;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.IIgnoredNegativeDefectService;
import com.tencent.bk.codecc.defect.service.impl.LintQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.DefectFileContentSegmentQueryRspVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectStatisticVO;
import com.tencent.bk.codecc.defect.vo.IgnoredNegativeDefectVO;
import com.tencent.bk.codecc.defect.vo.LintDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.OptionalInfoVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestResource
public class UserIgnoredNegativeDefectRestResourceImpl implements UserIgnoredNegativeDefectRestResource {

    @Autowired
    IIgnoredNegativeDefectService iIgnoredNegativeDefectService;

    @Autowired
    CheckerService checkerService;

    @Autowired
    @Qualifier("LINTQueryWarningBizService")
    private LintQueryWarningBizServiceImpl lintQueryWarningBizServiceImpl;

    private final String ERROR_MESSAGE_FOR_GONGFENG_PRIVATE = "fail to get git file content with: 403): ";

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<CommonDefectDetailQueryRspVO> queryDefectFileContentSegment(
            String userId,
            String toolName,
            QueryDefectFileContentSegmentReqVO request
    ) {
        request.setTryBestForPrivate(false);
        request.setToolName(toolName);
        DefectFileContentSegmentQueryRspVO rsp = lintQueryWarningBizServiceImpl.processQueryDefectFileContentSegment(
                null, userId, request);

        if (StringUtils.isBlank(rsp.getFileContent())
                || rsp.getFileContent().equals(ERROR_MESSAGE_FOR_GONGFENG_PRIVATE)) {
            rsp.setFileContent(I18NUtils.getMessage("NO_PERMISSION_FOR_REPO_TIPS"));
        }

        return new Result<>(rsp);
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<CommonDefectDetailQueryRspVO> queryDefectDetailWithoutFileContent(
            String userId,
            String toolName,
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO
    ) {
        if (!toolName.equals(commonDefectDetailQueryReqVO.getToolName())) {
            log.error("there are 2 different toolName: {} & {}", toolName, commonDefectDetailQueryReqVO.getToolName());
            throw new UnauthorizedException("unauthorized user permission!");
        }

        if (!commonDefectDetailQueryReqVO.getPattern().equals(ComConstants.ToolPattern.LINT.name())) {
            log.error("this interface just run on LINT tool.");
            throw new CodeCCException("this interface just run on LINT tool.");
        }

        LintDefectDetailQueryRspVO lintDefectQueryRspVO =
                (LintDefectDetailQueryRspVO) lintQueryWarningBizServiceImpl.processQueryDefectDetailWithoutFileContent(
                        null,
                        userId,
                        commonDefectDetailQueryReqVO,
                        null,
                        null
                );

        return new Result<>(lintDefectQueryRspVO);
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<Boolean> processNegativeDefect(
            String userName,
            String entityId,
            ProcessNegativeDefectReqVO processNegativeDefectReq
    ) {
        return new Result<>(iIgnoredNegativeDefectService.processNegativeDefect(entityId, processNegativeDefectReq));
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<List<IgnoredNegativeDefectVO>> listDefect(
            String userName,
            String toolName,
            Integer n,
            String lastInd,
            Integer pageSize,
            String orderBy,
            String orderDirection,
            ListNegativeDefectReqVO listNegativeDefectReq
    ) {
        List<IgnoredNegativeDefectVO> ignoredNegativeDefects = iIgnoredNegativeDefectService.listDefectAfterFilter(
                toolName,
                n,
                lastInd,
                pageSize,
                orderBy,
                orderDirection,
                listNegativeDefectReq
        );

        return new Result<>(ignoredNegativeDefects);
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<Long> countDefectAfterFilter(
            String userName,
            String toolName,
            Integer n,
            ListNegativeDefectReqVO listNegativeDefectReq
    ) {
        return new Result<>(iIgnoredNegativeDefectService.countDefectAfterFilter(toolName, n, listNegativeDefectReq));
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<OptionalInfoVO> listOptionalByToolName(String userName, String toolName) {
        List<CheckerDetailVO> checkerData = checkerService.queryCheckerDetailListByToolNameWithI18N(toolName);
        Set<String> checkers = new HashSet<>();
        Set<String> publishers = new HashSet<>();
        Set<String> tags = new HashSet<>();
        checkerData.forEach(checkerDetail -> {
            log.info("checker name: {}, tag: {}", checkerDetail.getCheckerName(), checkerDetail.getCheckerTag());
            checkers.add(checkerDetail.getCheckerName());
            tags.addAll(checkerDetail.getCheckerTag());
            if (StringUtils.isNotBlank(checkerDetail.getPublisher())) {
                publishers.add(checkerDetail.getPublisher());
            }
        });

        List<String> checkerList = new ArrayList<>(checkers);
        Collections.sort(checkerList);
        List<String> publisherList = new ArrayList<>(publishers);
        Collections.sort(publisherList);
        List<String> tagList = new ArrayList<>(tags);
        Collections.sort(tagList);

        OptionalInfoVO result = new OptionalInfoVO();
        result.setToolName(toolName);
        result.setCheckers(checkerList);
        result.setPublishers(publisherList);
        result.setTags(tagList);

        return new Result<>(result);
    }

    @Override
    @AuthMethod(extPassClassName = ToolDeveloperExtAuth.class)
    public Result<IgnoredNegativeDefectStatisticVO> statistic(String userName, String toolName, Integer n) {
        return new Result<>(iIgnoredNegativeDefectService.statistic(toolName, n));
    }
}
