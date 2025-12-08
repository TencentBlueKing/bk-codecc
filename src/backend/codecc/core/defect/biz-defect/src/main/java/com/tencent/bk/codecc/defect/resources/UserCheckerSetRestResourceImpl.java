package com.tencent.bk.codecc.defect.resources;

import com.google.common.collect.Lists;
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.codecc.defect.api.UserCheckerSetRestResource;
import com.tencent.bk.codecc.defect.auth.CheckerSetListExtAuth;
import com.tencent.bk.codecc.defect.auth.CheckerSetExtAuth;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerSetsRequest;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerSetsResponse;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.checkerset.TaskUsageDetailVO;
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
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.ResourceType;
import com.tencent.devops.common.auth.api.pojo.external.UserGroupRole;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;

/**
 * 规则包接口实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@ConditionalOnProperty(name = "codecc.enableMultiTenant", havingValue = "false", matchIfMissing = true)
@Slf4j
@RestResource
public class UserCheckerSetRestResourceImpl implements UserCheckerSetRestResource {

    @Autowired
    protected ICheckerSetManageBizService checkerSetManageBizService;

    @Autowired
    protected ICheckerSetQueryBizService checkerSetQueryBizService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    private static final long MAX_LEV_DIS = 1000010;

    @Override
    public Result<CheckerSetParamsVO> getParams(String projectId) {
        return new Result<>(checkerSetQueryBizService.getParams(projectId));
    }

    @Override
    @AuditEntry(actionId = ActionIds.CREATE_CHECKER_SET)
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {CodeCCAuthAction.RULESET_CREATE})
    public Result<Boolean> createCheckerSet(String user, String projectId,
            CreateCheckerSetReqVO createCheckerSetReqVO) {
        checkerSetManageBizService.createCheckerSet(user, projectId, createCheckerSetReqVO);
        return new Result<>(true);
    }

    /**
     * 此处更新规则集鉴权方式：是项目manager or 是规则集创建者
     * 因此permission传空
     */
    @Override
    @AuditEntry(actionId = ActionIds.UPDATE_CHECKER_SET)
    @AuthMethod(resourceType = ResourceType.PROJECT, permission = {}, roles = UserGroupRole.MANAGER,
            extPassClassName = CheckerSetExtAuth.class)
    public Result<Boolean> updateCheckersOfSet(String checkerSetId, String projectId, String user,
            UpdateCheckersOfSetReqVO updateCheckersOfSetReq
    ) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            if (!checkerSetEntities.get(0).getProjectId().equals(projectId)) {
                String errMsg = "只可以更新本项目内的规则集！";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        checkerSetManageBizService.updateCheckersOfSet(checkerSetId, user, updateCheckersOfSetReq.getCheckerProps(),
                null);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq) {
        return new Result<>(checkerSetManageBizService.updateCheckersOfSetForAll(user, updateAllCheckerReq));
    }

    @Override
    @I18NResponse
    @AuthMethod(
            resourceType = ResourceType.PROJECT,
            permission = {CodeCCAuthAction.RULESET_LIST},
            extPassClassName = CheckerSetListExtAuth.class
    )public Result<List<CheckerSetVO>> getCheckerSets(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfTask(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfProject(queryCheckerSetReq));
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

        List<CheckerSetVO> checkerSets = checkerSetQueryBizService.getTaskCheckerSets(
                projectId,
                taskIdList,
                toolNameList
        );

        Collections.sort(checkerSets, (t1, t2) ->
                Collator.getInstance(Locale.SIMPLIFIED_CHINESE).compare(t1.getCheckerSetName(), t2.getCheckerSetName())
        );

        return new Result<>(new QueryTaskCheckerSetsResponse(checkerSets));
    }

    @Override
    @I18NResponse
    public Result<Page<CheckerSetVO>> getCheckerSetsPageable(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfTaskPage(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfProjectPage(queryCheckerSetReq));
        }
    }

    protected int compareCheckerSets(CheckerSetVO a, CheckerSetVO b, String sortField, Sort.Direction sortDirec) {
        // 如果有一个是默认规则集, 另一个不是, 则默认规则集在前
        int aIsDefault = BooleanUtils.toInteger(a.isDefault());
        int bIsDefault = BooleanUtils.toInteger(b.isDefault());
        if (aIsDefault != bIsDefault) {
            return bIsDefault - aIsDefault;
        }

        // 如果有一个是推荐规则集, 另一个不是, 则推荐规则集在前
        int aIsRecommend = BooleanUtils.toInteger(a.isRecommend());
        int bIsRecommend = BooleanUtils.toInteger(b.isRecommend());
        if (aIsRecommend != bIsRecommend) {
            return bIsRecommend - aIsRecommend;
        }

        // 规则集名字与 quickSearch 的编辑距离更小的在前
        long aLevDis = a.getLevDis() == null ? MAX_LEV_DIS : a.getLevDis();
        long bLevDis = b.getLevDis() == null ? MAX_LEV_DIS : b.getLevDis();
        if (aLevDis != bLevDis) {
            return Long.compare(aLevDis, bLevDis);
        }

        // 将 legacy = true 的规则集放到后面, 因为它们适用于 CodeCC 的老插件
        int aIsLegacy = BooleanUtils.toInteger(a.getLegacy(), 1, 0, 0);
        int bIsLegacy = BooleanUtils.toInteger(b.getLegacy(), 1, 0, 0);
        if (aIsLegacy != bIsLegacy) {
            return aIsLegacy - bIsLegacy;
        }

        // 最后才比较用户指定的 sortField, 目前只支持根据 task_usage 和 create_date 这 2 个字段排序
        if (sortField.equals("task_usage")) {
            // 前端其实没有用到 sortField 这个字段, 所有逻辑都是跑到这个 task_usage 的比较
            int aTaskUsage = a.getTaskUsage() == null ? 0 : a.getTaskUsage();
            int bTaskUsage = b.getTaskUsage() == null ? 0 : b.getTaskUsage();
            if (aTaskUsage != bTaskUsage) {
                return Sort.Direction.ASC.equals(sortDirec) ? aTaskUsage - bTaskUsage : bTaskUsage - aTaskUsage;
            }
            // 这里需要保证排序的稳定性 (即对于相同的数据, 每次排序的结果都是完全一样的), 所以一定要分出大小
            // 如果 taskUsage 也一样, 就比较 createTime
        }

        long aCreateTime = a.getCreateTime() == null ? 0 : a.getCreateTime();
        long bCreateTime = b.getCreateTime() == null ? 0 : b.getCreateTime();
        return Sort.Direction.ASC.equals(sortDirec) ? Long.compare(aCreateTime, bCreateTime) :
                Long.compare(bCreateTime, aCreateTime);
    }

    @Override
    @I18NResponse
    public Result<Page<CheckerSetVO>> getOtherCheckerSets(String projectId,
            OtherCheckerSetListQueryReq queryCheckerSetReq) {
        List<CheckerSetVO> checkerSets = checkerSetQueryBizService.getOtherCheckerSets(projectId, queryCheckerSetReq);
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 100 : queryCheckerSetReq.getPageSize();
        // 用于请求返回的分页属性
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        if (CollectionUtils.isEmpty(checkerSets)) {
            Page<CheckerSetVO> page = new PageImpl<>(Lists.newArrayList(), pageable, 0);
            return new Result<>(page);
        }


        List<CheckerSetVO> result = checkerSets.stream()
                .sorted((o1, o2) -> compareCheckerSets(o1, o2, queryCheckerSetReq.getSortField(),
                        queryCheckerSetReq.getSortType()))
                .skip((long) pageNum * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        return new Result<>(new PageImpl<>(result, pageable, checkerSets.size()));
    }

    @Override
    public Result<List<CheckerCommonCountVO>> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq) {
        return new Result<>(checkerSetQueryBizService.queryCheckerSetCountList(checkerSetListQueryReq));
    }

    @Override
    @I18NResponse
    public Result<CheckerSetVO> getCheckerSetDetail(String checkerSetId, Integer version) {
        return new Result<>(checkerSetQueryBizService.getCheckerSetDetail(checkerSetId, version));
    }

    @Override
    public Result<Boolean> updateCheckerSetBaseInfo(String checkerSetId, String projectId,
            V3UpdateCheckerSetReqVO updateCheckerSetReq) {
        checkerSetManageBizService.updateCheckerSetBaseInfo(checkerSetId, projectId, updateCheckerSetReq);
        return new Result<>(true);
    }

    /**
     * 此处更新规则集鉴权方式：是项目manager or 是拥有任务setting权限
     */
    @Override
    @AuthMethod(permission = {CodeCCAuthAction.SETTING}, roles = UserGroupRole.MANAGER)
    public Result<Boolean> setRelationships(String checkerSetId, String user,
            CheckerSetRelationshipVO checkerSetRelationshipVO) {
        checkerSetManageBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> setRelationshipsOnce(String user, String projectId, long taskId, String toolName) {
        try {
            Pair<Boolean, String> pair = checkerSetManageBizService.setRelationshipsOnce(user, projectId, taskId,
                    toolName);
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
        checkerSetManageBizService.management(user, checkerSetId, checkerSetManagementReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Map<String, List<CheckerSetVO>>> getCheckerSetListByCategory(String projectId) {
        return new Result<>(checkerSetQueryBizService.getAvailableCheckerSetsOfProject(projectId));
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
        return new Result<>(checkerSetQueryBizService.queryCheckerDetailForPreCI());
    }

    @Override
    public Result<Page<TaskUsageDetailVO>> getTaskUsageList(String projectId, String checkerSetId) {
        return Result.success(checkerSetQueryBizService.getCheckerSetTaskUsageDetail(projectId, checkerSetId));
    }
}
