package com.tencent.devops.common.storage;

import com.tencent.devops.common.storage.sdk.BkRepoApi;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class BkRepoStorageAutoConfiguration {


    @Bean
    public BkRepoApi bkRepoApi(
            @Value("${storage.bkrepo.username:#{null}}") String username,
            @Value("${storage.bkrepo.password:#{null}}") String password,
            @Value("${storage.bkrepo.project:#{null}}") String project,
            @Value("${storage.bkrepo.repo:#{null}}") String repo,
            @Value("${storage.bkrepo.host:#{null}}") String bkrepoHost,
            @Value("${storage.bkrepo.tenant:#{null}}") String tenantId,
            @Value("${codecc.enableMultiTenant:#{null}}") Boolean enableMultiTenant
    ) {
        String finalProject = project;
        if (BooleanUtils.isTrue(enableMultiTenant) && StringUtils.isNotBlank(tenantId)) {
            finalProject = tenantId + "." + project;
        }

        return new BkRepoApi(username, password, finalProject, repo, bkrepoHost);
    }

}