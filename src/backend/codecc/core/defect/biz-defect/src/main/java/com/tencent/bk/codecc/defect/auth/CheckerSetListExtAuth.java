package com.tencent.bk.codecc.defect.auth;

import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.external.CodeCCExtAuthProcessor;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.web.security.filter.PermissionAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

import java.util.Collections;
import java.util.List;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 拓展鉴权：专用于规则集列表接口鉴权处理
 */
@Slf4j
@Component
public class CheckerSetListExtAuth implements CodeCCExtAuthProcessor {
    @Autowired
    private AuthTaskService authTaskService;
    @Autowired
    AuthExPermissionApi authExPermissionApi;

    @Override
    public boolean isPassAuth(@NotNull ContainerRequestContext requestContext, @NotNull ContainerRequestFilter filter) {
        String taskId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_TASK_ID);
        String user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID);
        String projectId = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_PROJECT_ID);

        // 请求头中包含任务id，则进行任务资源级鉴权
        if (StringUtils.isNotBlank(taskId)) {
            if (!(filter instanceof PermissionAuthFilter)) {
                log.error("auth filter class type incorrect!");
                return false;
            }
            PermissionAuthFilter authFilter = (PermissionAuthFilter) filter;
            List<BkAuthExResourceActionModel> results = authFilter.validUserTaskPermission(
                    requestContext,
                    authExPermissionApi,
                    authTaskService,
                    user,
                    projectId,
                    Collections.singletonList(CodeCCAuthAction.TASK_MANAGE),
                    taskId
            );

            if (CollectionUtils.isEmpty(results)) {
                return false;
            }

            for (BkAuthExResourceActionModel result : results) {
                if (result == null || Boolean.FALSE.equals(result.isPass())) {
                    return false;
                }

            }
            return true;
        }
        return false;
    }
}
