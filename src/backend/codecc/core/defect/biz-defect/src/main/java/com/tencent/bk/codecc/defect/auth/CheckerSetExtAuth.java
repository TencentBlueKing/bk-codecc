package com.tencent.bk.codecc.defect.auth;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.devops.common.auth.api.external.CodeCCExtAuthProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;


@Slf4j
@Component
public class CheckerSetExtAuth implements CodeCCExtAuthProcessor {

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Override
    public boolean isPassAuth(@NotNull ContainerRequestContext requestContext, @NotNull ContainerRequestFilter filter) {
        String user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID);
        MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
        String checkerSetId = pathParameters.getFirst("checkerSetId");

        CheckerSetEntity firstByCheckerSetId = checkerSetRepository.findFirstByCheckerSetId(checkerSetId);
        // 校验当然用户是否为规则集创建者
        return firstByCheckerSetId != null && firstByCheckerSetId.getCreator().equals(user);
    }
}
