package com.tencent.bk.codecc.codeccjob.consumer;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.codeccjob.component.BkItsmClientApi;
import com.tencent.bk.codecc.codeccjob.component.BkItsmClientApi.CodeCCItsmApprovalResultResp;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.IgnoreApprovalRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalEntity;
import com.tencent.bk.codecc.task.api.ServiceItsmSystemInfoResource;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverStatus;
import com.tencent.devops.common.service.IConsumer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class IgnoreApprovalTimingCheckConsumer implements IConsumer<String> {

    @Autowired
    private BkItsmClientApi bkItsmClientApi;

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    @Autowired
    private IgnoreApprovalRepository ignoreApprovalRepository;

    @Autowired
    private Client client;


    @Override
    public void consumer(String args) {
        try {
            log.info("IgnoreApprovalTimingCheckConsumer start");
            List<IgnoreApprovalEntity> approvals =
                    ignoreApprovalRepository.findByStatusInAndNextCheckStatusTimeLessThanEqual(
                            ApproverStatus.UNDER_APPROVAL_STATUS, System.currentTimeMillis());

            if (CollectionUtils.isEmpty(approvals)) {
                log.info("IgnoreApprovalTimingCheckConsumer approvals is empty");
                return;
            }
            log.info("IgnoreApprovalTimingCheckConsumer approvals size:{}", approvals.size());
            // 如果没有提单成功，直接标记拒绝，这里不重试
            AtomicLong failSubmitCount = new AtomicLong();
            approvals.stream().filter(it -> it.getStatus() == ApproverStatus.SEND_TO_QUEUE.status()
                    || (it.getStatus() == ApproverStatus.START_TO_APPROVAL.status()
                    && StringUtils.isBlank(it.getItsmSn()))).forEach(it -> {
                failSubmitCount.getAndIncrement();
                it.setStatus(ApproverStatus.SUBMIT_FAIL.status());
                it.applyAuditInfoOnUpdate(it.getUpdatedBy());
            });
            log.info("IgnoreApprovalTimingCheckConsumer failSubmitCount size:{}", failSubmitCount.get());

            // 目前仅支持BK ITSM
            ItsmSystemInfoVO systemInfoVO = client.get(ServiceItsmSystemInfoResource.class)
                    .getSystemInfo(ComConstants.ItsmSystem.BK_ITSM.name()).getData();
            // 如果提单成功，主动查询去找下当前的状态
            checkAndSetTicketStatus(approvals, systemInfoVO);

            // 如果还没被审批，检查是否超时，超时则终止审批，更新下次的检查时间
            processRunningTicket(approvals, systemInfoVO);

            // 更新状态
            for (IgnoreApprovalEntity approval : approvals) {
                // 已完成状态
                if (ApproverStatus.APPROVAL_FINISH_STATUS.contains(approval.getStatus())) {
                    lintDefectV2Dao.updateIgnoreApprovalStatusByTaskIdsAndApprovalId(approval.getTaskIds(),
                            approval.getEntityId(), approval.getStatus(), approval.getIgnoreTypeId(),
                            approval.getIgnoreReason(), approval.getIgnoreAuthor());
                }
            }
            ignoreApprovalRepository.saveAll(approvals);
        } catch (Exception e) {
            log.error("consumer cause error.", e);
        }
    }

    /**
     * 查询单据状态并设置
     *
     * @param approvals
     */
    private void checkAndSetTicketStatus(List<IgnoreApprovalEntity> approvals, ItsmSystemInfoVO systemInfoVO) {
        if (CollectionUtils.isEmpty(approvals) || systemInfoVO == null) {
            return;
        }
        // 如果提单成功，需要去找下当前的状态
        Map<String, IgnoreApprovalEntity> snToApprovalMap = approvals.stream().filter(
                        it -> it.getStatus() == ApproverStatus.START_TO_APPROVAL.status()
                                && StringUtils.isNotBlank(it.getItsmSn()))
                .collect(Collectors.toMap(IgnoreApprovalEntity::getItsmSn,
                        Function.identity(), (o1, o2) -> o1));
        log.info("checkAndSetTicketStatus size: {}", snToApprovalMap.size());
        if (!snToApprovalMap.isEmpty()) {
            List<String> snList = new ArrayList<>(snToApprovalMap.keySet());
            List<List<String>> snPageQueryList = Lists.partition(snList, ComConstants.SMALL_PAGE_SIZE);
            for (List<String> snPage : snPageQueryList) {
                List<CodeCCItsmApprovalResultResp> respList =
                        bkItsmClientApi.getTicketStatusResult(snPage, systemInfoVO);
                if (CollectionUtils.isEmpty(respList)) {
                    continue;
                }
                for (CodeCCItsmApprovalResultResp resp : respList) {
                    // 不是RUNNING，表示结束
                    if (StringUtils.isNotBlank(resp.getCurrentStatus()) && StringUtils.isNotBlank(resp.getSn())
                            && !BkItsmClientApi.RUNNING_STATUS.equals(resp.getCurrentStatus())
                            && snToApprovalMap.containsKey(resp.getSn())) {
                        IgnoreApprovalEntity approval = snToApprovalMap.get(resp.getSn());
                        approval.setStatus(resp.getApproveResult() != null && resp.getApproveResult()
                                ? ApproverStatus.SUBMIT_SUCC.status() : ApproverStatus.SUBMIT_FAIL.status());
                        approval.applyAuditInfoOnUpdate(resp.getUpdatedBy());
                        log.info("approval Id: {} status: {}", approval.getEntityId(), approval.getStatus());
                    }
                }
            }
        }
    }

    private void processRunningTicket(List<IgnoreApprovalEntity> approvals, ItsmSystemInfoVO systemInfoVO) {
        if (CollectionUtils.isEmpty(approvals) || systemInfoVO == null) {
            return;
        }
        long curTime = System.currentTimeMillis();
        // 还没有审批成功的，设置下次查询时间
        approvals.stream().filter(it -> it.getStatus() == ApproverStatus.START_TO_APPROVAL.status()).forEach(it -> {
            // 判断是否已经达到了最大重试次数
            if (it.getCreatedDate() == null
                    || curTime > (it.getCreatedDate() + IgnoreApprovalConstants.IGNORE_APPROVAL_EXPIRED_MS)) {
                boolean terminate = bkItsmClientApi.terminateTicket(it.getItsmSn(), systemInfoVO);
                // 如果终止成功，就等待回调或者下次查询状态，否则直接标记失败
                if (!terminate) {
                    it.setStatus(ApproverStatus.SUBMIT_FAIL.status());
                }
            }
            it.setNextCheckStatusTime(it.getNextCheckStatusTime()
                    + IgnoreApprovalConstants.CHECK_STATUS_INTERVAL_MS);
            it.applyAuditInfoOnUpdate(it.getUpdatedBy());
        });
    }

}
