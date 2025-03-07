package com.tencent.bk.codecc.codeccjob.component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalConfigEntity;
import com.tencent.bk.codecc.defect.vo.ignore.DefectIgnoreApprovalVO;
import com.tencent.bk.codecc.task.vo.itsm.ItsmSystemInfoVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverType;
import com.tencent.devops.common.util.OkhttpUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.NORMAL;
import static com.tencent.devops.common.constant.ComConstants.PROMPT;
import static com.tencent.devops.common.constant.ComConstants.PROMPT_IN_DB;
import static com.tencent.devops.common.constant.ComConstants.SERIOUS;

@Slf4j
@Component
public class BkItsmClientApi {

    @Value("${itsm.codecc.appcode:#{null}}")
    private String appCode;

    @Value("${itsm.codecc.appsecret:#{null}}")
    private String appSecret;

    @Value("${itsm.codecc.username:#{null}}")
    private String userName;

    @Value("${itsm.codecc.accessToken:#{null}}")
    private String accessToken;

    @Value("${codecc.public.url}")
    private String codeccUrl;

    public static final String RUNNING_STATUS = "RUNNING";

    public static final String OPERATE_TERMINATE = "TERMINATE";

    public static final String OPERATE_TERMINATE_MSG = "Approval Timeout";

    /**
     * 创建ITSM单据
     *
     * @param approvalVO
     * @param approvers
     * @param approvalConfig
     * @param systemInfoVO
     * @return
     */
    public Pair<Boolean, String> createTicket(DefectIgnoreApprovalVO approvalVO, List<String> approvers,
            IgnoreApprovalConfigEntity approvalConfig, ItsmSystemInfoVO systemInfoVO) {
        try {
            String url = systemInfoVO.getCreateTicketUrl();
            String bodyTemplate = systemInfoVO.getCreateTicketBody();
            String body = bodyTemplate.replace("{bk_app_code}", appCode)
                    .replace("{bk_app_secret}", appSecret)
                    .replace("{creator}", approvalVO.getIgnoreAuthor())
                    .replace("{approval_id}", approvalVO.getApprovalId())
                    .replace("{project_id}", approvalVO.getProjectId())
                    .replace("{dimensions}", String.join(ComConstants.COMMA, approvalConfig.getDimensions()))
                    .replace("{severities}", approvalConfig.getSeverities().stream().map(String::valueOf)
                            .collect(Collectors.joining(ComConstants.COMMA)))
                    .replace("{ignore_reason_type}", approvalVO.getIgnoreTypeName())
                    .replace("{ignore_count}", approvalVO.getDefectCount() != null
                            ? String.valueOf(approvalVO.getDefectCount()) : "0")
                    .replace("{defect_detail_url}", getDetailUrl(approvalVO, approvalConfig))
                    .replace("{approver_type}", approvalConfig.getApproverType())
                    .replace("{approvers}", CollectionUtils.isEmpty(approvers)
                            ? ComConstants.EMPTY_STRING : String.join(ComConstants.COMMA, approvers));
            CodeCCItsmRespVO<CodeCCItsmCreateTicketResp> response = OkhttpUtils.INSTANCE.doHttpPost(url,
                    body, Collections.emptyMap(),
                    new TypeReference<CodeCCItsmRespVO<CodeCCItsmCreateTicketResp>>() {
                    });
            if (response == null || !response.isOk() || response.getData() == null) {
                log.error("create ticket return is not ok. {} {}", approvalVO.getApprovalId(), response != null
                        ? response.message : "response is null");
                return Pair.of(false, JSONObject.toJSONString(response));
            }
            return Pair.of(true, JSONObject.toJSONString(response.getData()));
        } catch (Exception e) {
            log.error("create ticket {} cause error.", approvalVO.getApprovalId(), e);
            return Pair.of(false, e.getMessage());
        }
    }

    /**
     * 拼接详情链接
     *
     * @param approvalVO
     * @param approvalConfig
     * @return
     */
    private String getDetailUrl(DefectIgnoreApprovalVO approvalVO, IgnoreApprovalConfigEntity approvalConfig) {
        if (CollectionUtils.isEmpty(approvalVO.getTaskIds())) {
            return ComConstants.EMPTY_STRING;
        }
        StringBuilder urlBuilder = new StringBuilder("https://" + codeccUrl)
                .append("/codecc/").append(approvalVO.getProjectId());
        if (approvalVO.getTaskIds().size() == 1) {
            urlBuilder.append("/task/").append(approvalVO.getTaskIds().stream().findFirst().orElse(0L))
                    .append("/defect/security/list");
        } else {
            urlBuilder.append("/defect/list");
        }
        urlBuilder.append("?dimension=").append(String.join(ComConstants.COMMA, approvalConfig.getDimensions()));
        urlBuilder.append("&severity=").append(approvalConfig.getSeverities().stream().filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum());
        urlBuilder.append("&status=").append(ComConstants.DefectStatus.NEW.value()
                | ComConstants.DefectStatus.IGNORE.value());
        urlBuilder.append("&ignoreApprovalId=").append(approvalVO.getApprovalId());
        urlBuilder.append("&x-devops-project-id=").append(approvalVO.getProjectId());
        return urlBuilder.toString();
    }

    /**
     * 获取单据状态
     *
     * @param snList
     * @param systemInfoVO
     * @return
     */
    public List<CodeCCItsmApprovalResultResp> getTicketStatusResult(List<String> snList,
            ItsmSystemInfoVO systemInfoVO) {
        try {
            String url = systemInfoVO.getGetTicketStatusUrl();
            String bodyTemplate = systemInfoVO.getGetTicketStatusBody();
            String body = bodyTemplate.replace("{bk_app_code}", appCode)
                    .replace("{bk_app_secret}", appSecret)
                    .replace("{bk_username}", userName)
                    .replace("{access_token}", accessToken)
                    .replace("{sn}", JSONObject.toJSONString(snList));
            CodeCCItsmRespVO<List<CodeCCItsmApprovalResultResp>> response = OkhttpUtils.INSTANCE.doHttpPost(url,
                    body, Collections.emptyMap(),
                    new TypeReference<CodeCCItsmRespVO<List<CodeCCItsmApprovalResultResp>>>() {
                    });
            if (response == null || !response.isOk() || response.getData() == null) {
                log.error("get ticket status return is not ok. {} {}", JSONObject.toJSONString(snList),
                        response != null ? response.message : "response is null");
                return Collections.emptyList();
            }
            return response.getData();
        } catch (Exception e) {
            log.error("get ticket status {} cause error.", JSONObject.toJSONString(snList), e);
            return Collections.emptyList();
        }
    }

    /**
     * 停止单据
     *
     * @param sn
     * @param systemInfoVO
     * @return
     */
    public boolean terminateTicket(String sn, ItsmSystemInfoVO systemInfoVO) {
        try {
            String url = systemInfoVO.getOperateTicketUrl();
            String bodyTemplate = systemInfoVO.getOperateTicketBody();
            String body = bodyTemplate.replace("{bk_app_code}", appCode)
                    .replace("{bk_app_secret}", appSecret)
                    .replace("{bk_username}", userName)
                    .replace("{access_token}", accessToken)
                    .replace("{action_type}", OPERATE_TERMINATE)
                    .replace("{action_message}", OPERATE_TERMINATE_MSG)
                    .replace("{operator}", userName)
                    .replace("{sn}", sn);
            CodeCCItsmRespVO<List<Object>> response = OkhttpUtils.INSTANCE.doHttpPost(url, body, Collections.emptyMap(),
                    new TypeReference<CodeCCItsmRespVO<List<Object>>>() {
                    });
            if (response == null || !response.isOk()) {
                log.error("get ticket status return is not ok. {} {}", sn, response != null ? response.message
                        : "response is null");
                return false;
            }
            return response.result != null && response.result;
        } catch (Exception e) {
            log.error("terminate ticket status {} cause error.", sn, e);
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class CodeCCItsmRespVO<T> {

        private Boolean result;

        private Integer code;

        private String message;

        private T data;

        public boolean isOk() {
            return code != null && code == 0;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CodeCCItsmCreateTicketResp {

        private String sn;

        @JsonProperty("ticket_url")
        private String ticketUrl;

        private Long id;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CodeCCItsmApprovalResultResp {

        private String sn;

        @JsonProperty("callback_url")
        private String callbackUrl;

        @JsonProperty("ticket_url")
        private String ticketUrl;

        @JsonProperty("current_status")
        private String currentStatus;

        @JsonProperty("approve_result")
        private Boolean approveResult;

        @JsonProperty("updated_by")
        private String updatedBy;

    }

}
