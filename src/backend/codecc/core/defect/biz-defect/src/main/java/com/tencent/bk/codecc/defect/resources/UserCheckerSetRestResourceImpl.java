package com.tencent.bk.codecc.defect.resources;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.api.UserCheckerSetRestResource;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerSetsRequest;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerSetsResponse;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetPermissionType;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.checkerset.AuthManagementPermissionReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.UpdateCheckersOfSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;

/**
 * 规则包接口实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@RestResource
public class UserCheckerSetRestResourceImpl implements UserCheckerSetRestResource {

    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Override
    public Result<CheckerSetParamsVO> getParams(String projectId) {
        return new Result<>(checkerSetBizService.getParams(projectId));
    }

    @Override
    public Result<Boolean> createCheckerSet(String user, String projectId,
            CreateCheckerSetReqVO createCheckerSetReqVO) {
        checkerSetBizService.createCheckerSet(user, projectId, createCheckerSetReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> updateCheckersOfSet(String checkerSetId, String projectId, String user,
            UpdateCheckersOfSetReqVO updateCheckersOfSetReq) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            if (!checkerSetEntities.get(0).getProjectId().equals(projectId)) {
                String errMsg = "只可以更新本项目内的规则集！";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        checkerSetBizService.updateCheckersOfSet(checkerSetId, user, updateCheckersOfSetReq.getCheckerProps(), null);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq) {
        return new Result<>(checkerSetBizService.updateCheckersOfSetForAll(user, updateAllCheckerReq));
    }

    @Override
    @I18NResponse
    public Result<List<CheckerSetVO>> getCheckerSets(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetBizService.getCheckerSetsOfTask(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetBizService.getCheckerSetsOfProject(queryCheckerSetReq));
        }
    }

    @Override
    public Result<List<CheckerSetVO>> getTaskCheckerSets(
            String userId, String projectId, long taskId,
            String toolName, String dimension, String buildId
    ) {
        Pair<List<String>, List<String>> pair = ParamUtils.parseToolNameAndDimensions(toolName, dimension);
        List<String> toolNameList = pair.getFirst();
        List<String> dimensionList = pair.getSecond();
        QueryTaskCheckerSetsRequest request = new QueryTaskCheckerSetsRequest(
                Lists.newArrayList(taskId),
                toolNameList,
                dimensionList,
                buildId
        );

        return new Result<>(queryTaskCheckerSets(userId, projectId, request).getData());
    }

    @Override
    @I18NResponse
    public Result<QueryTaskCheckerSetsResponse> queryTaskCheckerSets(
            String userId,
            String projectId,
            QueryTaskCheckerSetsRequest request
    ) {
        List<Long> taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(request.getTaskIdList(), projectId, userId);
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                Lists.newArrayList(),
                taskIdList,
                request.getBuildId()
        );

        if (MapUtils.isEmpty(taskToolMap)) {
            return new Result<>(new QueryTaskCheckerSetsResponse(Lists.newArrayList()));
        }

        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        List<String> toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        List<CheckerSetVO> checkerSets = checkerSetBizService.getTaskCheckerSets(
                projectId,
                taskIdList,
                toolNameList
        );

        return new Result<>(new QueryTaskCheckerSetsResponse(checkerSets));
    }

    @Override
    public Result<Page<CheckerSetVO>> getCheckerSetsPageable(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetBizService.getCheckerSetsOfTaskPage(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetBizService.getCheckerSetsOfProjectPage(queryCheckerSetReq));
        }
    }

    @Override
    public Result<Page<CheckerSetVO>> getOtherCheckerSets(String projectId,
            OtherCheckerSetListQueryReq queryCheckerSetReq) {
        return new Result<>(checkerSetBizService.getOtherCheckerSets(projectId, queryCheckerSetReq));
    }

    @Override
    public Result<List<CheckerCommonCountVO>> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq) {
        return new Result<>(checkerSetBizService.queryCheckerSetCountList(checkerSetListQueryReq));
    }

    @Override
    @I18NResponse
    public Result<CheckerSetVO> getCheckerSetDetail(String checkerSetId, Integer version) {
        return new Result<>(checkerSetBizService.getCheckerSetDetail(checkerSetId, version));
    }

    @Override
    public Result<Boolean> updateCheckerSetBaseInfo(String checkerSetId, String projectId,
            V3UpdateCheckerSetReqVO updateCheckerSetReq) {
        checkerSetBizService.updateCheckerSetBaseInfo(checkerSetId, projectId, updateCheckerSetReq);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setRelationships(String checkerSetId, String user,
            CheckerSetRelationshipVO checkerSetRelationshipVO) {
        checkerSetBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setRelationshipsOnce(
            String user,
            String projectId,
            long taskId,
            String toolName
    ) {
        try {
            Pair<Boolean, String> pair = checkerSetBizService.setRelationshipsOnce(user, projectId, taskId, toolName);
            boolean success = Boolean.TRUE.equals(pair.getFirst());
            String errorMsg = pair.getSecond();
            if (success) {
                return new Result<>(true);
            } else {
                return new Result<>(-1, "-1", errorMsg);
            }
        } catch (Throwable t) {
            log.error("setRelationshipsOnce error: {}, {}, {}, {}", user, projectId, taskId, toolName, t);

            return new Result<>(-1, "-1", "配置规则集异常");
        }
    }

    @Override
    public Result<Boolean> management(String user, String checkerSetId,
            CheckerSetManagementReqVO checkerSetManagementReqVO) {
        checkerSetBizService.management(user, checkerSetId, checkerSetManagementReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(String projectId) {
        return new Result<>(checkerSetBizService.getAvailableCheckerSetsOfProject(projectId));
    }

    @Override
    public Result<List<CheckerSetPermissionType>> getUserManagementPermission(
            AuthManagementPermissionReqVO authManagementPermissionReqVO) {
        List<CheckerSetPermissionType> checkerSetPermissionTypes = new ArrayList<>();
        if (authManagementPermissionReqVO.getCheckerSetId() != null) {
            List<CheckerSetEntity> checkerSetEntities =
                    checkerSetRepository.findByCheckerSetId(authManagementPermissionReqVO.getCheckerSetId());
            if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                if (checkerSetEntities.get(0).getCreator().equals(authManagementPermissionReqVO.getUser())) {
                    checkerSetPermissionTypes.add(CheckerSetPermissionType.CREATOR);
                }
            }
        }
        try {
            if (authExPermissionApi.authProjectManager(authManagementPermissionReqVO.getProjectId(),
                    authManagementPermissionReqVO.getUser())) {
                checkerSetPermissionTypes.add(CheckerSetPermissionType.MANAGER);
            }
        } catch (Exception e) {
            log.info("get checker set auth fail! ");
        }
        return new Result<>(checkerSetPermissionTypes);
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSetsForPreCI() {
        return new Result<>(checkerSetBizService.queryCheckerDetailForPreCI());
    }
}
