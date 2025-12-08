package com.tencent.bk.codecc.defect.auth;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.ToolDeveloperInfoRepository;
import com.tencent.bk.codecc.defect.model.ToolDeveloperInfoEntity;
import com.tencent.devops.common.api.exception.UnauthorizedException;
import com.tencent.devops.common.auth.api.external.CodeCCExtAuthProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Set;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 工具开发者权限校验
 *
 * @date 2024/01/19
 */
@Slf4j
@Component
public class ToolDeveloperExtAuth implements CodeCCExtAuthProcessor {

    @Autowired
    private ToolDeveloperInfoRepository toolDeveloperInfoRepository;

    @Override
    public boolean isPassAuth(@NotNull ContainerRequestContext requestContext, @NotNull ContainerRequestFilter filter) {
        String userName = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID);
        if (StringUtils.isBlank(userName)) {
            return false;
        }

        MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        String toolName = pathParameters.getFirst("toolName");
        if (toolName == null) {
            log.error("validate permission fail! toolName is null.");
            throw new UnauthorizedException("unauthorized user permission!");
        }

        ToolDeveloperInfoEntity toolDeveloperInfo = toolDeveloperInfoRepository.findFirstByToolName(toolName);

        if (toolDeveloperInfo != null
                && (isNameIn(userName, toolDeveloperInfo.getDevelopers())
                || isNameIn(userName, toolDeveloperInfo.getOwners())
                || isNameIn(userName, toolDeveloperInfo.getMasters()))) {
            return true;
        }

        log.error("validate permission fail! user: {}", userName);
        throw new UnauthorizedException("unauthorized user permission!");
    }

    private boolean isNameIn(String name, Set<String> names) {
        if (names == null) {
            return false;
        }

        for (String it : names) {
            if (name.equalsIgnoreCase(it)) {
                return true;
            }
        }

        return false;
    }
}
