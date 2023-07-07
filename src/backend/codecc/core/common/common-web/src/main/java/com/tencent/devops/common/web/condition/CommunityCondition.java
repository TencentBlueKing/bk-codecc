package com.tencent.devops.common.web.condition;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CommunityCondition implements Condition {

    private Logger logger = LoggerFactory.getLogger(CommunityCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String envType = context.getEnvironment().getProperty("codecc.common.envType");
        logger.info("get spring app env type is: {}", envType);
        return StringUtils.isBlank(envType) || envType.equalsIgnoreCase("community");
    }
}
