package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.IgnoreApprovalConfigRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.IgnoreApprovalConfigDao;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.IgnoreApprovalRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalConfigEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalEntity;
import com.tencent.bk.codecc.defect.service.IgnoreApprovalService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreApprovalVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IgnoreApprovalServiceImpl implements IgnoreApprovalService {


    @Autowired
    protected IgnoreApprovalConfigDao ignoreApprovalConfigDao;

    @Autowired
    protected IgnoreApprovalConfigRepository ignoreApprovalConfigRepository;

    @Autowired
    protected IgnoreApprovalRepository ignoreApprovalRepository;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Override
    public boolean savaApprovalConfig(String projectId, String userName,
            IgnoreApprovalConfigVO ignoreApprovalConfigVO) {
        // 参数校验
        if (StringUtils.isBlank(projectId) || !validApprovalSaveParam(ignoreApprovalConfigVO)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
        }
        // 重复检验，防止一个类型下配置多个审批
        List<IgnoreApprovalConfigEntity> repeats = getRepeatConfig(projectId, ignoreApprovalConfigVO);
        if (!CollectionUtils.isEmpty(repeats)) {
            throw new CodeCCException(CommonMessageCode.RECORD_RANGE_IS_DUPLICATED,
                    new String[]{repeats.get(0).getName()});
        }
        // 存储实体
        IgnoreApprovalConfigEntity configEntity = new IgnoreApprovalConfigEntity();
        BeanUtils.copyProperties(ignoreApprovalConfigVO, configEntity);
        configEntity.setProjectScopeType(IgnoreApprovalConstants.ProjectScopeType.SINGLE.type());
        configEntity.setProjectId(projectId);
        configEntity.setStatus(ComConstants.Status.ENABLE.value());
        if (StringUtils.isBlank(configEntity.getEntityId())) {
            configEntity.applyAuditInfoOnCreate(userName);
        } else {
            configEntity.applyAuditInfoOnUpdate(userName);
        }
        ignoreApprovalConfigRepository.save(configEntity);
        return true;
    }

    /**
     * 参数校验
     *
     * @param approvalConfigVO
     * @return
     */
    private boolean validApprovalSaveParam(IgnoreApprovalConfigVO approvalConfigVO) {
        if (approvalConfigVO == null || StringUtils.isBlank(approvalConfigVO.getName())) {
            return false;
        }
        // 告警匹配条件检验
        if (CollectionUtils.isEmpty(approvalConfigVO.getDimensions())
                || CollectionUtils.isEmpty(approvalConfigVO.getSeverities())
                || CollectionUtils.isEmpty(approvalConfigVO.getIgnoreTypeIds())) {
            return false;
        }
        // 任务范围检验
        IgnoreApprovalConstants.TaskScopeType taskScopeType =
                IgnoreApprovalConstants.TaskScopeType.getByType(approvalConfigVO.getTaskScopeType());
        if (taskScopeType == null) {
            return false;
        } else if ((taskScopeType == IgnoreApprovalConstants.TaskScopeType.EXCLUDE
                || taskScopeType == IgnoreApprovalConstants.TaskScopeType.INCLUDE)
                && CollectionUtils.isEmpty(approvalConfigVO.getTaskScopeList())) {
            return false;
        }
        // 审核人类型校验
        IgnoreApprovalConstants.ApproverType approverType =
                IgnoreApprovalConstants.ApproverType.getByType(approvalConfigVO.getApproverType());
        if (approverType == null) {
            return false;
        } else {
            return approverType != IgnoreApprovalConstants.ApproverType.CUSTOM_APPROVER
                    || !CollectionUtils.isEmpty(approvalConfigVO.getCustomApprovers());
        }
    }


    /**
     * 判断是否会出现范围重复的问题
     *
     * @param projectId
     * @param ignoreApprovalConfigVO
     * @return
     */
    private List<IgnoreApprovalConfigEntity> getRepeatConfig(String projectId,
            IgnoreApprovalConfigVO ignoreApprovalConfigVO) {
        List<IgnoreApprovalConfigEntity> approvalConfigs =
                ignoreApprovalConfigDao.findSingleProjectMatchConfig(projectId, ignoreApprovalConfigVO.getDimensions(),
                        ignoreApprovalConfigVO.getSeverities(), ignoreApprovalConfigVO.getIgnoreTypeIds());
        // 没有重复项，通过
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            return Collections.emptyList();
        }
        // 去除本次的修改的配置
        if (StringUtils.isNotBlank(ignoreApprovalConfigVO.getEntityId())) {
            approvalConfigs = approvalConfigs.stream()
                    .filter(it -> !ignoreApprovalConfigVO.getEntityId().equals(it.getEntityId()))
                    .collect(Collectors.toList());
        }
        // 没有重复项，通过
        if (CollectionUtils.isEmpty(approvalConfigs)) {
            return Collections.emptyList();
        }
        // 检查任务范围
        // 是否存在所有任务的选择
        List<IgnoreApprovalConfigEntity> allTaskScopeConfigs = approvalConfigs.stream()
                .filter(it -> IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType()) != null
                        && (IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType())
                        == IgnoreApprovalConstants.TaskScopeType.ALL)).collect(Collectors.toList());
        IgnoreApprovalConstants.TaskScopeType taskScopeType =
                IgnoreApprovalConstants.TaskScopeType.getByType(ignoreApprovalConfigVO.getTaskScopeType());
        if (taskScopeType == IgnoreApprovalConstants.TaskScopeType.ALL || !allTaskScopeConfigs.isEmpty()) {
            // 所有任务，只有有就重复
            return CollectionUtils.isEmpty(allTaskScopeConfigs) ? approvalConfigs : allTaskScopeConfigs;
        } else if (taskScopeType == IgnoreApprovalConstants.TaskScopeType.INCLUDE) {
            List<Long> taskList = ignoreApprovalConfigVO.getTaskScopeList();
            // 所有的EXCLUDE都包含这个任务列表
            List<IgnoreApprovalConfigEntity> repeats = approvalConfigs.stream()
                    .filter(it -> IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType())
                            == IgnoreApprovalConstants.TaskScopeType.EXCLUDE
                            && !new HashSet<>(it.getTaskScopeList()).containsAll(taskList))
                    .collect(Collectors.toList());
            // 所有的Include都不包含这个列表
            repeats.addAll(approvalConfigs.stream()
                    .filter(it -> IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType())
                            == IgnoreApprovalConstants.TaskScopeType.INCLUDE
                            && it.getTaskScopeList().stream().anyMatch(taskList::contains))
                    .collect(Collectors.toList()));
            return repeats;
        } else if (taskScopeType == IgnoreApprovalConstants.TaskScopeType.EXCLUDE) {
            List<Long> taskList = ignoreApprovalConfigVO.getTaskScopeList();
            // 不允许对同一个类型配置多条EXCLUDE
            List<IgnoreApprovalConfigEntity> repeats = approvalConfigs.stream()
                    .filter(it -> IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType())
                            == IgnoreApprovalConstants.TaskScopeType.EXCLUDE)
                    .collect(Collectors.toList());
            // 所有的Include都不包含这个列表
            repeats.addAll(approvalConfigs.stream()
                    .filter(it -> IgnoreApprovalConstants.TaskScopeType.getByType(it.getTaskScopeType())
                            == IgnoreApprovalConstants.TaskScopeType.INCLUDE
                            && !new HashSet<>(taskList).containsAll(it.getTaskScopeList()))
                    .collect(Collectors.toList()));
            return repeats;
        }
        return Collections.emptyList();
    }


    @Override
    public Page<IgnoreApprovalConfigVO> projectConfigList(String projectId, String userName,
            Integer pageNum, Integer pageSize) {
        IgnoreApprovalConstants.ProjectScopeType projectScopeType =
                IgnoreApprovalConstants.ProjectScopeType.getByProjectId(projectId);
        Long count = ignoreApprovalConfigDao.getProjectScopeCount(projectScopeType, projectId);
        List<IgnoreApprovalConfigEntity> configs =
                ignoreApprovalConfigDao.findConfigByProjectScopeTypeByPage(projectScopeType, projectId,
                        pageNum, pageSize);
        if (CollectionUtils.isEmpty(configs)) {
            return new Page<>(count, pageNum, pageSize, 0, Lists.newArrayList());
        }
        boolean projectManager = isProjectManager(projectId, userName);
        List<IgnoreApprovalConfigVO> vos = configs.stream().map(it -> {
            boolean edit = projectScopeType == IgnoreApprovalConstants.ProjectScopeType.SINGLE && projectManager;
            return createConfigVOByEntity(it, edit, projectScopeType);
        }).collect(Collectors.toList());
        return new Page<>(count, pageNum, pageSize, 0, vos);
    }

    private boolean isProjectManager(String projectId, String userName) {
        try {
            return authExPermissionApi.authProjectManager(projectId, userName);
        } catch (Throwable e) {
            return false;
        }
    }

    private IgnoreApprovalConfigVO createConfigVOByEntity(IgnoreApprovalConfigEntity entity, Boolean edit,
            IgnoreApprovalConstants.ProjectScopeType projectScopeType) {
        IgnoreApprovalConfigVO vo = new IgnoreApprovalConfigVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setEdit(edit);
        vo.setDisableEditReason(projectScopeType == IgnoreApprovalConstants.ProjectScopeType.SINGLE
                ? IgnoreApprovalConstants.DisableConfigEditReason.ONLY_PROJECT_MANAGER.name() :
                IgnoreApprovalConstants.DisableConfigEditReason.UNIFIED_CONFIG.name());
        return vo;
    }

    private IgnoreApprovalConfigVO opCreateConfigVOByEntity(IgnoreApprovalConfigEntity entity) {
        IgnoreApprovalConfigVO vo = new IgnoreApprovalConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public IgnoreApprovalConfigVO approvalConfigDetail(String projectId, String userName,
            String ignoreApprovalConfigId) {
        Optional<IgnoreApprovalConfigEntity> configOptional =
                ignoreApprovalConfigRepository.findById(ignoreApprovalConfigId);
        if (!configOptional.isPresent()) {
            return null;
        }
        IgnoreApprovalConstants.ProjectScopeType projectScopeType =
                IgnoreApprovalConstants.ProjectScopeType.getByProjectId(projectId);
        IgnoreApprovalConfigEntity config = configOptional.get();
        boolean edit = projectScopeType == IgnoreApprovalConstants.ProjectScopeType.SINGLE
                && authExPermissionApi.authProjectManager(projectId, userName);
        return createConfigVOByEntity(config, edit, projectScopeType);
    }

    @Override
    public boolean approvalConfigDelete(String projectId, String userName, String ignoreApprovalConfigId) {
        IgnoreApprovalConstants.ProjectScopeType projectScopeType =
                IgnoreApprovalConstants.ProjectScopeType.getByProjectId(projectId);
        if (projectScopeType != IgnoreApprovalConstants.ProjectScopeType.SINGLE) {
            return false;
        }
        Optional<IgnoreApprovalConfigEntity> configOptional =
                ignoreApprovalConfigRepository.findById(ignoreApprovalConfigId);
        if (!configOptional.isPresent()) {
            return false;
        }
        IgnoreApprovalConfigEntity config = configOptional.get();
        ignoreApprovalConfigDao.updateStatus(config.getEntityId(), ComConstants.Status.DISABLE.value());
        return true;
    }

    @Override
    public List<IgnoreApprovalVO> getApprovalListByIds(List<String> ignoreApprovalIds) {
        List<IgnoreApprovalVO> vos = new ArrayList<>();
        ignoreApprovalRepository.findAllById(ignoreApprovalIds).forEach(ignoreApproval -> {
            vos.add(createApprovalVOByEntity(ignoreApproval));
        });
        return vos;
    }

    @Override
    public IgnoreApprovalVO getApprovalById(String ignoreApprovalId) {
        Optional<IgnoreApprovalEntity> approvalOptional =
                ignoreApprovalRepository.findById(ignoreApprovalId);
        if (!approvalOptional.isPresent()) {
            return null;
        }
        IgnoreApprovalEntity approval = approvalOptional.get();
        return createApprovalVOByEntity(approval);
    }

    @Override
    public List<IgnoreApprovalConfigVO> getProjectMatchConfig(String projectId, Integer ignoreTypeId,
            List<String> dimensions, List<Integer> severities) {
        IgnoreApprovalConstants.ProjectScopeType projectScopeType =
                IgnoreApprovalConstants.ProjectScopeType.getByProjectId(projectId);
        List<IgnoreApprovalConfigEntity> configs =
                ignoreApprovalConfigDao.findProjectConfig(projectScopeType, projectId, ignoreTypeId,
                        dimensions, severities);
        return configs.stream().map(it -> createConfigVOByEntity(it, null, projectScopeType))
                .collect(Collectors.toList());
    }

    @Override
    public void updateApprovalAndDefectWhenCallback(String approvalId, Integer status, String sn,
            String url, String username) {
        IgnoreApprovalEntity ignoreApproval = ignoreApprovalRepository.findById(approvalId).orElse(null);
        if (ignoreApproval == null) {
            log.error("updateApprovalAndDefectWhenCallback ignoreApproval is empty. approvalId: {}", approvalId);
            return;
        }
        ignoreApproval.setStatus(status);
        ignoreApproval.setItsmSn(sn);
        ignoreApproval.setItsmUrl(url);
        ignoreApproval.setApprover(username);
        ignoreApprovalRepository.save(ignoreApproval);

        // 更新告警数据
        List<Long> taskIds = ParamUtils.allTaskByProjectIdIfEmpty(ignoreApproval.getTaskIds(),
                ignoreApproval.getProjectId(), ignoreApproval.getIgnoreAuthor());
        if (CollectionUtils.isEmpty(taskIds)) {
            log.error("updateApprovalAndDefectWhenCallback tasks is empty. approvalId: {}", approvalId);
            return;
        }
        long totalModifiedCount = 0L;
        List<List<Long>> taskIdPages = Lists.partition(taskIds, ComConstants.COMMON_PAGE_SIZE);
        for (List<Long> taskIdPage : taskIdPages) {
            long modifiedCount = lintDefectV2Dao.updateIgnoreApprovalResultByTaskIdsAndApprovalId(taskIdPage,
                    approvalId, status, ignoreApproval.getIgnoreTypeId(), ignoreApproval.getIgnoreReason(),
                    ignoreApproval.getIgnoreAuthor());
            totalModifiedCount += modifiedCount;
        }
        log.info("updateApprovalAndDefectWhenCallback, approvalId: {}, modify count: {}", approvalId,
                totalModifiedCount);
    }

    @Override
    public Page<IgnoreApprovalConfigVO> getIgnoreApprovalConfigList(String taskScopeType, String projectId,
                                                                 Integer pageNum, Integer pageSize,
                                                                 String sortField, String sortType) {
        log.info("getIgnoreApprovalConfigList taskScopeType: [{}],projectId: [{}] pageNum: [{}], pageSize:[{}]",
                taskScopeType, projectId, pageNum, pageSize);
        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "create_date" : sortField);
        Sort.Direction direction = Sort.Direction.fromString(StringUtils.isEmpty(sortType) ? "DESC" : sortType);
        Pageable pageable =
                PageableUtils.getPageable(pageNum, pageSize, sortFieldInDb, direction, "create_date");
        org.springframework.data.domain.Page<IgnoreApprovalConfigEntity> resultPage =
                ignoreApprovalConfigDao.findProjectConfigPage(taskScopeType, projectId, pageable);

        List<IgnoreApprovalConfigVO> result = resultPage.getContent().stream()
                .map(this::opCreateConfigVOByEntity)
                .collect(Collectors.toList());
        log.info("getIgnoreApprovalConfigList result size : {}", resultPage.getSize());

        return new Page<>(resultPage.getTotalElements(), resultPage.getNumber(), resultPage.getSize(),
                resultPage.getTotalPages(), result);
    }

    @Override
    public Boolean upsertIgnoreApprovalConfig(IgnoreApprovalConfigVO reqVO) {
        log.info("upsertIgnoreApprovalConfig reqVO: {}", JsonUtil.INSTANCE.toJson(reqVO));
        if (reqVO == null || StringUtils.isBlank(reqVO.getName()) || CollectionUtils.isEmpty(reqVO.getDimensions())
                || CollectionUtils.isEmpty(reqVO.getSeverities()) || CollectionUtils.isEmpty(reqVO.getIgnoreTypeIds())
                || StringUtils.isBlank(reqVO.getProjectScopeType()) || StringUtils.isBlank(reqVO.getTaskScopeType())
                || StringUtils.isBlank(reqVO.getApproverType())) {
            log.error("reportRepoInfo param valid fail");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL);
        }
        IgnoreApprovalConfigEntity ignoreApprovalConfigEntity =
                ignoreApprovalConfigRepository.findByEntityId(reqVO.getEntityId());
        if (ignoreApprovalConfigEntity == null) {
            ignoreApprovalConfigEntity = new IgnoreApprovalConfigEntity();
            ignoreApprovalConfigEntity.applyAuditInfoOnCreate(reqVO.getUpdatedBy());
        } else {
            ignoreApprovalConfigEntity.applyAuditInfoOnUpdate(reqVO.getUpdatedBy());
        }
        ignoreApprovalConfigEntity.setName(reqVO.getName());
        ignoreApprovalConfigEntity.setDimensions(reqVO.getDimensions());
        ignoreApprovalConfigEntity.setSeverities(reqVO.getSeverities());
        ignoreApprovalConfigEntity.setIgnoreTypeIds(reqVO.getIgnoreTypeIds());
        ignoreApprovalConfigEntity.setProjectScopeType(reqVO.getProjectScopeType());
        ignoreApprovalConfigEntity.setProjectId(reqVO.getProjectId());
        ignoreApprovalConfigEntity.setTaskScopeType(reqVO.getTaskScopeType());
        ignoreApprovalConfigEntity.setApproverType(reqVO.getApproverType());
        ignoreApprovalConfigEntity.setLimitedProjectIds(reqVO.getLimitedProjectIds());
        ignoreApprovalConfigEntity.setTaskScopeList(reqVO.getTaskScopeList());
        ignoreApprovalConfigEntity.setCustomApprovers(reqVO.getCustomApprovers());
        ignoreApprovalConfigEntity.setStatus(ComConstants.Status.ENABLE.value());
        ignoreApprovalConfigRepository.save(ignoreApprovalConfigEntity);
        log.info("upsertIgnoreApprovalConfig finished!");
        return true;
    }

    @Override
    public Boolean deleteIgnoreApprovalConfig(String entityId, String userId) {
        log.info("deleteIgnoreApprovalConfig entityId: {}, userId: {}", entityId, userId);

        if (StringUtils.isBlank(entityId)) {
            log.warn("entityId is blank! update failed!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL);
        }

        IgnoreApprovalConfigEntity ignoreApprovalConfigEntity = ignoreApprovalConfigRepository.findByEntityId(entityId);
        if (ignoreApprovalConfigEntity == null) {
            log.warn("ignoreApprovalConfigEntity entityId is not found!");
            String errorMsg = "审批配置未找到";
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, errorMsg);
        }
        // 更改status状态用作删除
        ignoreApprovalConfigEntity.setStatus(ComConstants.Status.DISABLE.value());
        ignoreApprovalConfigEntity.applyAuditInfoOnUpdate(userId);
        ignoreApprovalConfigRepository.save(ignoreApprovalConfigEntity);
        log.info("deleteGitHubSync finish!");
        return true;
    }


    private IgnoreApprovalVO createApprovalVOByEntity(IgnoreApprovalEntity entity) {
        IgnoreApprovalVO vo = new IgnoreApprovalVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
