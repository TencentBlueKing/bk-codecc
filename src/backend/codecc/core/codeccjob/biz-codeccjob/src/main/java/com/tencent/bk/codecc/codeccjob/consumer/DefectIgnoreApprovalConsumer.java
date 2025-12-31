package com.tencent.bk.codecc.codeccjob.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.tencent.bk.codecc.codeccjob.component.BkItsmClientApi;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.BgSecurityApproverRepository;
import com.tencent.bk.codecc.codeccjob.dao.core.mongorepository.IgnoreApprovalConfigRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.IgnoreApprovalRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.ignore.BgSecurityApproverEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalConfigEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalEntity;
import com.tencent.bk.codecc.defect.vo.ignore.DefectIgnoreApprovalVO;
import com.tencent.bk.codecc.task.api.ServiceItsmSystemInfoResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceUserInfoRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverType;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.util.BeanUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.tencent.devops.common.constant.ComConstants.DEFAULT_BG_ID;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_DEFECT_IGNORE_APPROVAL;

@Component
@Slf4j
public class DefectIgnoreApprovalConsumer implements IConsumer<DefectIgnoreApprovalVO> {

    @Autowired
    private BkItsmClientApi bkItsmClientApi;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Autowired
    private IgnoreApprovalConfigRepository ignoreApprovalConfigRepository;

    @Autowired
    private IgnoreApprovalRepository ignoreApprovalRepository;

    @Autowired
    private Client client;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BgSecurityApproverRepository bgSecurityApproverRepository;


    @Value("${itsm.version:#{null}}")
    private Integer version;


    @Override
    public void consumer(DefectIgnoreApprovalVO approvalVO) {
        try {
            // 基础信息校验，如果不通过直接返回，不做记录
            if (approvalVO == null || StringUtils.isBlank(approvalVO.getApprovalConfigId())
                    || StringUtils.isBlank(approvalVO.getApprovalId())) {
                log.warn("consumer start. param check fail");
                return;
            }
            log.info("consumer start. {} {}", approvalVO.getApprovalId(), approvalVO.getDefectMatchId());
            IgnoreApprovalConfigEntity approvalConfig =
                    ignoreApprovalConfigRepository.findById(approvalVO.getApprovalConfigId()).orElse(null);
            // 目前仅支持BK ITSM
            ItsmSystemInfoVO systemInfoVO = client.get(ServiceItsmSystemInfoResource.class)
                    .getSystemInfo(ComConstants.ItsmSystem.BK_ITSM.name(), version).getData();
            if (systemInfoVO == null || approvalConfig == null) {
                log.warn("systemInfoVO or approvalConfig is empty. {} {}", approvalVO.getApprovalId(),
                        approvalVO.getDefectMatchId());
                return;
            }
            List<ApproverType> approverTypes = approvalConfig.getApproverTypes().stream().map(
                    ApproverType::getByType
            ).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(approverTypes)) {
                log.warn("approverType is illegal. {} {} {}", approvalVO.getApprovalId(),
                        approvalVO.getDefectMatchId(), approverTypes);
                return;
            }
            // 填充基础信息
            IgnoreApprovalEntity approval = new IgnoreApprovalEntity();
            BeanUtils.copyProperties(approvalVO, approval);
            approval.setEntityId(approvalVO.getApprovalId());
            approval.setDimensions(approvalConfig.getDimensions());
            approval.setSeverities(approvalConfig.getSeverities());
            approval.setApproverTypes(approvalConfig.getApproverTypes());
            approval.applyAuditInfoOnCreate(approvalVO.getIgnoreAuthor());
            approval.setNextCheckStatusTime(System.currentTimeMillis()
                    + IgnoreApprovalConstants.CHECK_STATUS_INTERVAL_MS);
            // 查找审批人
            List<String> approvers = getApprovers(approvalVO.getDefectMatchId(), approvalConfig.getProjectScopeType(),
                    approvalVO.getProjectId(), approverTypes, approvalConfig.getCustomApprovers(),
                    approvalVO.getIgnoreAuthor());
            log.info("{} {} approvers is {} ", approvalVO.getApprovalId(), approvalVO.getDefectMatchId(), approvers);
            approval.setApprovers(approvers);
            // 找不到审批人，且非Leader, 直接审批拒绝
            if (CollectionUtils.isEmpty(approvers)) {
                approval.setStatus(IgnoreApprovalConstants.ApproverStatus.SEND_TO_QUEUE.status());
                approval.setCreateTicketStatus(ComConstants.Status.DISABLE.value());
                approval.setCreateTicketMsg("no approver");
                ignoreApprovalRepository.save(approval);
                return;
            }
            // 开始提交审批
            Pair<Boolean, String> resp = bkItsmClientApi.createTicket(approvalVO, approvers, approvalConfig,
                    systemInfoVO);
            // 提单失败 且 是第一次提单，重试一次
            if (!resp.getFirst() && !BooleanUtils.isTrue(approvalVO.getRetryCommit())) {
                log.info("{} {} retry commit", approvalVO.getApprovalId(), approvalVO.getDefectMatchId());
                approvalVO.setRetryCommit(true);
                rabbitTemplate.convertAndSend(
                        EXCHANGE_CODECC_DEFECT_IGNORE_APPROVAL,
                        ROUTE_CODECC_DEFECT_IGNORE_APPROVAL,
                        approvalVO
                );
                return;
            }

            // 记录提单信息
            if (resp.getFirst()) {
                log.info("{} {} commit success.", approvalVO.getApprovalId(), approvalVO.getDefectMatchId());
                approval.setStatus(IgnoreApprovalConstants.ApproverStatus.START_TO_APPROVAL.status());
                approval.setCreateTicketStatus(ComConstants.Status.ENABLE.value());
                BkItsmClientApi.CodeCCItsmCreateTicketResp createTicketResp =
                        JSONObject.parseObject(resp.getSecond(), BkItsmClientApi.CodeCCItsmCreateTicketResp.class);
                approval.setItsmUrl(createTicketResp.getTicketUrl());
                approval.setItsmSn(createTicketResp.getSn());
                approval.setItsmId(createTicketResp.getId());
            } else {
                log.info("{} {} commit fail.", approvalVO.getApprovalId(), approvalVO.getDefectMatchId());
                // 第二次失败
                approval.setStatus(IgnoreApprovalConstants.ApproverStatus.SEND_TO_QUEUE.status());
                approval.setCreateTicketStatus(ComConstants.Status.DISABLE.value());
                approval.setCreateTicketMsg(resp.getSecond());
            }
            ignoreApprovalRepository.save(approval);
            // 更新告警审核单状态
            if (resp.getFirst()) {
                long count = lintDefectV2Dao.updateIgnoreApprovalStatusByTaskIdsAndApprovalId(new ArrayList<>(
                                approvalVO.getTaskIds()), approvalVO.getApprovalId(),
                        IgnoreApprovalConstants.ApproverStatus.START_TO_APPROVAL.status(), null, null,
                        approvalVO.getIgnoreAuthor());
                log.info("{} {} update defect size : {}.", approvalVO.getApprovalId(),
                        approvalVO.getDefectMatchId(), count);
            }
        } catch (Exception e) {
            log.error("consumer cause error. id:"
                    + (approvalVO == null ? ComConstants.EMPTY_STRING : approvalVO.getApprovalId()), e);
        }
    }

    private List<String> getApprovers(String defectMatchId, String projectScopeType, String projectId,
            List<IgnoreApprovalConstants.ApproverType> approverTypes, List<String> customApprovers, String author) {
        // 解析defectMatchId格式：前缀|审批类型_参数|...|后缀
        String[] infos = defectMatchId.split("\\|");
        if (infos.length <= 2) {
            return Collections.emptyList();
        }

        Set<String> approvers = new HashSet<>();
        Map<String, String> approverTypeParams = new HashMap<>();
        // 格式如下：CONFIG_ID|TYPE1|TYPE2:TYPE_PARAM|TYPE3|...|OPS_ID
        // 解析中间段的审批类型参数（跳过首尾元素）
        Arrays.stream(infos)
                .skip(1)
                .limit(infos.length - 2L)
                .filter(StringUtils::isNotBlank)
                .forEach(param -> {
                    String[] parts = param.split(ComConstants.SEPARATOR_SEMICOLON, 2);
                    approverTypeParams.put(
                            parts[0],
                            parts.length > 1 ? parts[1] : ComConstants.EMPTY_STRING
                    );
                });

        // 遍历审批类型获取对应审批人
        approverTypes.stream()
                .filter(type -> approverTypeParams.containsKey(type.type()))
                .forEach(type -> {
                    String param = approverTypeParams.get(type.type());
                    List<String> typeApprovers = getSingleApprovalTypeApprover(
                            param, projectScopeType, projectId, type, customApprovers, author);
                    approvers.addAll(typeApprovers);
                });

        return new ArrayList<>(approvers);
    }

    private List<String> getSingleApprovalTypeApprover(String approvalInfo, String projectScopeType, String projectId,
            IgnoreApprovalConstants.ApproverType approverType, List<String> customApprovers, String author) {
        try {
            switch (approverType) {
                case PROJECT_MANAGER:
                    return authExPermissionApi.getProjectManager(projectId);

                case TASK_MANAGER:
                    return parseTaskManagerApprovers(approvalInfo);

                case CHECKER_PUBLISHER:
                    return parseCheckerPublisherApprovers(approvalInfo);

                case IGNORE_AUTHOR_LEADER:
                    return getAuthorLeader(author);

                case BG_SECURITY_MANAGER:
                    return parseBgSecurityApprovers(approvalInfo, projectScopeType, author);

                default:
                    return CollectionUtils.isEmpty(customApprovers)
                            ? Collections.emptyList()
                            : new ArrayList<>(customApprovers);
            }
        } catch (Exception e) {
            log.error("Get {} approver error: {}", approverType, e.getMessage());
            return Collections.emptyList();
        }
    }

    // 新增辅助方法保持主逻辑清晰
    private List<String> parseTaskManagerApprovers(String approvalInfo) {
        if (!StringUtils.isNumeric(approvalInfo)) {
            return Collections.emptyList();
        }

        Long taskId = Long.valueOf(approvalInfo);
        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class)
                .getTaskInfoById(taskId)
                .getData();

        return Optional.ofNullable(taskInfo)
                .map(TaskDetailVO::getTaskOwner)
                .filter(owners -> !CollectionUtils.isEmpty(owners))
                .orElse(Collections.emptyList());
    }

    private List<String> parseCheckerPublisherApprovers(String approvalInfo) {
        if (StringUtils.isBlank(approvalInfo)) {
            return Collections.emptyList();
        }
        return Arrays.stream(approvalInfo.split(ComConstants.COMMA))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private List<String> getAuthorLeader(String author) {
        return Optional.ofNullable(client.get(ServiceUserInfoRestResource.class)
                        .getUserDirectLeader(author)
                        .getData())
                .filter(StringUtils::isNotBlank)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }

    private List<String> parseBgSecurityApprovers(String approvalInfo, String projectScopeType, String author) {
        String[] parts = approvalInfo.split(ComConstants.KEY_UNDERLINE);
        if (parts.length < 3 || !Arrays.stream(parts).allMatch(NumberUtils::isCreatable)) {
            return Collections.emptyList();
        }

        OrgInfoVO org = new OrgInfoVO();
        org.setBgId(Integer.parseInt(parts[0]));
        org.setBusinessLineId(Integer.parseInt(parts[1]));
        org.setDeptId(Integer.parseInt(parts[2]));

        return getBgSecurityApprovers(projectScopeType, org, author);
    }

    private List<String> getBgSecurityApprovers(String projectScopeType, OrgInfoVO orgInfoVO, String author) {
        List<BgSecurityApproverEntity> bgSecurityApproverEntities =
                bgSecurityApproverRepository.findByProjectScopeType(projectScopeType);
        if (CollectionUtils.isEmpty(bgSecurityApproverEntities)) {
            return Collections.emptyList();
        }
        if (orgInfoVO == null || orgInfoVO.getBgId() == DEFAULT_BG_ID) {
            // 使用忽略人的部门信息
            orgInfoVO = client.get(ServiceUserInfoRestResource.class).getOrgInfo(author).getData();
        }
        List<String> approvers = null;
        int maxScore = -1;
        for (BgSecurityApproverEntity bgSecurityApproverEntity : bgSecurityApproverEntities) {
            if (bgSecurityApproverEntity == null || bgSecurityApproverEntity.getOrgInfoEntity() == null
                    || CollectionUtils.isEmpty(bgSecurityApproverEntity.getApprovers())) {
                continue;
            }
            OrgInfoVO orgConfig = new OrgInfoVO();
            BeanUtils.copyProperties(bgSecurityApproverEntity.getOrgInfoEntity(), orgConfig);
            if (orgConfig.contains(orgInfoVO)) {
                int score = orgConfig.getMatchScore(orgInfoVO);
                if (score > maxScore) {
                    maxScore = score;
                    approvers = bgSecurityApproverEntity.getApprovers();
                }
            }
        }
        return CollectionUtils.isEmpty(approvers) ? Collections.emptyList() : approvers;
    }
}
