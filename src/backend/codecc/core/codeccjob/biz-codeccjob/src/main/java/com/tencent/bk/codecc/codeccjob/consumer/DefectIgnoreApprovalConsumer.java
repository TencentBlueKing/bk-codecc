package com.tencent.bk.codecc.codeccjob.consumer;

import com.alibaba.fastjson.JSONObject;
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
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                    .getSystemInfo(ComConstants.ItsmSystem.BK_ITSM.name()).getData();
            if (systemInfoVO == null || approvalConfig == null) {
                log.warn("systemInfoVO or approvalConfig is empty. {} {}", approvalVO.getApprovalId(),
                        approvalVO.getDefectMatchId());
                return;
            }
            IgnoreApprovalConstants.ApproverType approverType =
                    IgnoreApprovalConstants.ApproverType.getByType(approvalConfig.getApproverType());
            if (approverType == null) {
                log.warn("approverType is illegal. {} {} {}", approvalVO.getApprovalId(),
                        approvalVO.getDefectMatchId(), approvalConfig.getApproverType());
                return;
            }
            // 填充基础信息
            IgnoreApprovalEntity approval = new IgnoreApprovalEntity();
            BeanUtils.copyProperties(approvalVO, approval);
            approval.setEntityId(approvalVO.getApprovalId());
            approval.setDimensions(approvalConfig.getDimensions());
            approval.setSeverities(approvalConfig.getSeverities());
            approval.setApproverType(approvalConfig.getApproverType());
            approval.applyAuditInfoOnCreate(approvalVO.getIgnoreAuthor());
            approval.setNextCheckStatusTime(System.currentTimeMillis()
                    + IgnoreApprovalConstants.CHECK_STATUS_INTERVAL_MS);
            // 查找审批人
            List<String> approvers = getApprovers(approvalVO.getDefectMatchId(), approvalConfig.getProjectScopeType(),
                    approvalVO.getProjectId(), approverType, approvalConfig.getCustomApprovers());
            log.info("{} {} approvers is {} ", approvalVO.getApprovalId(), approvalVO.getDefectMatchId(), approvers);
            approval.setApprovers(approvers);
            // 找不到审批人，且非Leader, 直接审批拒绝
            if (CollectionUtils.isEmpty(approvers)
                    && approverType != IgnoreApprovalConstants.ApproverType.IGNORE_AUTHOR_LEADER) {
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
            IgnoreApprovalConstants.ApproverType approverType, List<String> customApprovers) {
        if (approverType == IgnoreApprovalConstants.ApproverType.PROJECT_MANAGER) {
            return authExPermissionApi.getProjectManager(projectId);
        } else if (approverType == IgnoreApprovalConstants.ApproverType.TASK_MANAGER) {
            String[] approvalIdInfos = defectMatchId.split("_");
            if (approvalIdInfos.length < 2 || !StringUtils.isNumeric(approvalIdInfos[1])) {
                return Collections.emptyList();
            }
            Long taskId = Long.valueOf(approvalIdInfos[1]);
            TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
            return taskInfo == null || CollectionUtils.isEmpty(taskInfo.getTaskOwner()) ? Collections.emptyList() :
                    taskInfo.getTaskOwner();
        } else if (approverType == IgnoreApprovalConstants.ApproverType.CHECKER_PUBLISHER) {
            String[] approvalIdInfos = defectMatchId.split("_");
            return approvalIdInfos.length < 2 || StringUtils.isBlank(approvalIdInfos[1]) ? Collections.emptyList() :
                    Arrays.asList(approvalIdInfos[1].split(ComConstants.COMMA));
        } else if (approverType == IgnoreApprovalConstants.ApproverType.IGNORE_AUTHOR_LEADER) {
            return Collections.emptyList();
        } else if (approverType == IgnoreApprovalConstants.ApproverType.BG_SECURITY_MANAGER) {
            // BG安全负责人，需要查询是否有配置
            String[] approvalIdInfos = defectMatchId.split("_");
            if (approvalIdInfos.length < 5 || !NumberUtils.isCreatable(approvalIdInfos[1])
                    || !NumberUtils.isCreatable(approvalIdInfos[2]) || !NumberUtils.isCreatable(approvalIdInfos[3])) {
                return Collections.emptyList();
            }
            OrgInfoVO org = new OrgInfoVO();
            org.setBgId(Integer.valueOf(approvalIdInfos[1]));
            org.setBusinessLineId(Integer.valueOf(approvalIdInfos[2]));
            org.setDeptId(Integer.valueOf(approvalIdInfos[3]));
            return getBgSecurityApprovers(projectScopeType, org);
        } else {
            return customApprovers;
        }
    }

    private List<String> getBgSecurityApprovers(String projectScopeType, OrgInfoVO orgInfoVO) {
        List<BgSecurityApproverEntity> bgSecurityApproverEntities =
                bgSecurityApproverRepository.findByProjectScopeType(projectScopeType);
        if (CollectionUtils.isEmpty(bgSecurityApproverEntities)) {
            return Collections.emptyList();
        }
        for (BgSecurityApproverEntity bgSecurityApproverEntity : bgSecurityApproverEntities) {
            if (bgSecurityApproverEntity == null || bgSecurityApproverEntity.getOrgInfoEntity() == null
                    || CollectionUtils.isEmpty(bgSecurityApproverEntity.getApprovers())) {
                continue;
            }
            OrgInfoVO orgConfig = new OrgInfoVO();
            BeanUtils.copyProperties(bgSecurityApproverEntity.getOrgInfoEntity(), orgConfig);
            if (orgConfig.contains(orgInfoVO)) {
                return bgSecurityApproverEntity.getApprovers();
            }
        }
        return Collections.emptyList();
    }
}
