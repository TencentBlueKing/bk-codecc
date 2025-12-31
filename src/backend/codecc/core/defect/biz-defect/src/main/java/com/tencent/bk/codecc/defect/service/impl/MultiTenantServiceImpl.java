package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.ImmutableMap;
import com.tencent.bk.codecc.defect.service.MultiTenantService;
import com.tencent.bk.codecc.defect.vo.BatchGetUserNameReqVO;
import com.tencent.bk.codecc.defect.vo.BatchGetUserNameRespVO;
import com.tencent.bk.codecc.defect.vo.BkUserBatchGetUserRespVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * MultiTenantService 实现类
 *
 * @date 2025/07/28
 */
@Slf4j
@Service
public class MultiTenantServiceImpl implements MultiTenantService {
    @Value("${bkapi.tenant.host:#{null}}")
    private String host;
    @Value("${iam.appCode:#{null}}")
    private String bkIamAppCode;
    @Value("${iam.appSecret:#{null}}")
    private String bkIamAppSecret;

    public static final String TENANT_ID_KEY = "X-Bk-Tenant-Id";
    public static final String BK_API_AUTH_KEY = "X-Bkapi-Authorization";

    @Override
    public Set<String> transUserId(String tenantId, List<String> userIds) {
        BatchGetUserNameReqVO request = new BatchGetUserNameReqVO(tenantId, userIds);
        BatchGetUserNameRespVO resp = batchGetUserName(request);
        Map<String, String> userInfoMap = resp.getUserId2userName();

        Set<String> result = new HashSet<>();
        if (userInfoMap != null) {
            result = userIds.stream().map(it -> {
                // 能转换则转换, 不能就用原始值
                if (userInfoMap.containsKey(it)) {
                    return userInfoMap.get(it);
                }

                return it;
            }).collect(Collectors.toSet());
        }

        return result;
    }

    @Override
    public BatchGetUserNameRespVO batchGetUserName(BatchGetUserNameReqVO request) {
        if (CollectionUtils.isEmpty(request.getUserIds()) || StringUtils.isBlank(request.getTenantId())) {
            log.warn("batchGetUserName params is invalid");
            return new BatchGetUserNameRespVO();
        }

        String userIds = StringUtils.join(request.getUserIds(), ",");
        Map<String, String> userInfoMap = getUserNameMap(request.getTenantId(), userIds);

        return new BatchGetUserNameRespVO(userInfoMap);
    }

    private Map<String, String> getUserNameMap(String tenantId, String userIds) {
        String url = String.format("%s/api/bk-user/prod/api/v3/open/tenant/users/-/display_info/?bk_usernames=%s",
                host, userIds);
        Map<String, String> header = genCommonHeader(tenantId);
        log.info("url: {}, header: {}", url, header);
        String originResp = OkhttpUtils.INSTANCE.doGet(url, header);
        log.info("originResp: {}", originResp);

        if (StringUtils.isBlank(originResp)) {
            log.error("getUserNameMap failed");
            return null;
        }

        BkUserBatchGetUserRespVO resp =
                JsonUtil.INSTANCE.to(originResp, BkUserBatchGetUserRespVO.class);
        if (CollectionUtils.isEmpty(resp.getData())) {
            return null;
        }

        return resp.getData().stream().collect(Collectors.toMap(
                BkUserBatchGetUserRespVO.User::getBk_username,
                it -> {
                    int ind = it.getDisplay_name().lastIndexOf("(");
                    if (ind > 0) {
                        return it.getDisplay_name().substring(0, ind);
                    }

                    // ind = 0/-1 的情况都视为有异常, 直接返回原数据
                    return it.getDisplay_name();
                },
                (a, b) -> b
        ));
    }

    private Map<String, String> genCommonHeader(String tenantId) {
        String authValue = String.format("{\"bk_app_code\": \"%s\", \"bk_app_secret\": \"%s\"}",
                bkIamAppCode, bkIamAppSecret);
        return ImmutableMap.of(
                TENANT_ID_KEY, tenantId,
                BK_API_AUTH_KEY, authValue
        );
    }
}
