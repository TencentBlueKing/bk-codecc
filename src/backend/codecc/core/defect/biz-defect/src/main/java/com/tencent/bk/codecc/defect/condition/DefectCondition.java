package com.tencent.bk.codecc.defect.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DefectCondition implements Condition {

    private Logger logger = LoggerFactory.getLogger(DefectCondition.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String springAppName = context.getEnvironment().getProperty("spring.application.name");
        logger.info("get spring app name is: {}", springAppName);
        return springAppName.contains("defect");
    }
}
