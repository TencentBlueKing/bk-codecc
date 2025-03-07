package com.tencent.devops.common.audit;

import com.tencent.bk.audit.DefaultAuditRequestProvider;
import com.tencent.bk.audit.constants.UserIdentifyTypeEnum;
import com.tencent.bk.audit.model.AuditHttpRequest;
import lombok.extern.slf4j.Slf4j;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 生成 BK-Audit 的请求
 *
 * @date 2024/11/15
 */
@Slf4j
public class CodeccAuditRequestProvider extends DefaultAuditRequestProvider {
    public static final String USER_IDENTIFY = "/user/";

    @Override
    public String getUsername() {
        AuditHttpRequest httpRequest = getRequest();
        return httpRequest.getHttpServletRequest().getHeader(AUTH_HEADER_DEVOPS_USER_ID);
    }

    @Override
    public UserIdentifyTypeEnum getUserIdentifyType() {
        AuditHttpRequest httpRequest = getRequest();
        String uri = httpRequest.getUri();

        // 如果 uri 中带 "/user/", 就认为是用户点击页面发起的请求
        if (uri.contains(USER_IDENTIFY)) {
            return UserIdentifyTypeEnum.PERSONAL;
        }
        // 否则是平台发起的请求
        return UserIdentifyTypeEnum.PLATFORM;
    }

}
