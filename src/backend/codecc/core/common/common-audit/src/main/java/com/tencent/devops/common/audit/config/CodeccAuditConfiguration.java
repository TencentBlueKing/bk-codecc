package com.tencent.devops.common.audit.config;

import com.tencent.bk.audit.AuditRequestProvider;
import com.tencent.bk.audit.config.AuditAutoConfiguration;
import com.tencent.bk.audit.config.AuditProperties;
import com.tencent.devops.common.audit.CodeccAuditPostFilter;
import com.tencent.devops.common.audit.CodeccAuditRequestProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BK-Audit 配置类
 *
 * @date 2024/11/15
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true")
@AutoConfigureBefore(AuditAutoConfiguration.class)
@Slf4j
public class CodeccAuditConfiguration {
    @Bean
    public AuditRequestProvider auditRequestProvider() {
        log.info("Init CodeccAuditRequestProvider");
        return new CodeccAuditRequestProvider();
    }

    @Bean
    public CodeccAuditPostFilter codeccAuditPostFilter() {
        return new CodeccAuditPostFilter();
    }


}
