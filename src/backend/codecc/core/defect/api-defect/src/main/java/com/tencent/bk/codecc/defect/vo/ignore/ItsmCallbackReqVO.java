package com.tencent.bk.codecc.defect.vo.ignore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * ISTM 回调BODY
 */
@Data
public class ItsmCallbackReqVO {

    /**
     * 标题
     */
    private String title;

    /**
     * 当前状态，为FINISHED为正常结束
     */
    @JsonProperty("current_status")
    private String currentStatus;

    /**
     * 单号
     */
    private String sn;

    /**
     * 单据链接
     */
    @JsonProperty("ticket_url")
    private String ticketUrl;

    /**
     * 单据更新时间
     */
    @JsonProperty("update_at")
    private String updateAt;

    /**
     * 单据更新人
     */
    @JsonProperty("updated_by")
    private String updatedBy;

    /**
     * 单据审批结果True or False
     */
    @JsonProperty("approve_result")
    private String approveResult;

    /**
     * token
     */
    private String token;

    /**
     * 最后一个节点的审批人
     */
    @JsonProperty("last_approver")
    private String lastApprover;
}
