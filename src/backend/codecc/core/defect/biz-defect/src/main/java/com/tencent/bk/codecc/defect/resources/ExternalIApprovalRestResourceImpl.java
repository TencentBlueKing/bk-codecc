package com.tencent.bk.codecc.defect.resources;

import com.alibaba.fastjson.JSONObject;
import com.tencent.bk.codecc.defect.api.ExternalIApprovalRestResource;
import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.bk.codecc.defect.service.IgnoreApprovalService;
import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackReqVO;
import com.tencent.bk.codecc.defect.vo.ignore.ItsmCallbackRespVO;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestResource
public class ExternalIApprovalRestResourceImpl implements ExternalIApprovalRestResource {

    @Autowired
    private IgnoreApprovalService ignoreApprovalService;

    private static final String SUCCESS_FLAG = "true";

    @Override
    public ItsmCallbackRespVO callback(String approvalId, ItsmCallbackReqVO reqVO) {
        log.info("itsm callback enter {}", approvalId);
        if (StringUtils.isEmpty(approvalId) || reqVO == null) {
            log.error("itsm callback is empty {}, {}", approvalId, JSONObject.toJSONString(reqVO));
            return ItsmCallbackRespVO.fail();
        }
        Integer status = StringUtils.isNotBlank(reqVO.getApproveResult())
                && SUCCESS_FLAG.equalsIgnoreCase(reqVO.getApproveResult())
                ? IgnoreApprovalConstants.ApproverStatus.SUBMIT_SUCC.status()
                : IgnoreApprovalConstants.ApproverStatus.SUBMIT_FAIL.status();
        ignoreApprovalService.updateApprovalAndDefectWhenCallback(approvalId, status, reqVO.getSn(),
                reqVO.getTicketUrl(), reqVO.getLastApprover());
        log.info("itsm callback end {}", approvalId);
        return ItsmCallbackRespVO.success();
    }
}
