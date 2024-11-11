package com.tencent.codecc.common.mq.common;


import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

public class NonEmptyRabbitMQPropertiesCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes =
                metadata.getAnnotationAttributes(ConditionalOnNonEmptyRabbitMQProperties.class.getName());
        if (attributes == null) {
            return ConditionOutcome.noMatch("Property is empty or not defined");
        }
        if (attributes.get("prefix") == null || attributes.get("name") == null) {
            return ConditionOutcome.noMatch("Property is empty or not defined");
        }
        String prefix = (String) attributes.get("prefix");
        String[] names = (String[]) attributes.get("name");
        for (String name : names) {
            String propertyValue = context.getEnvironment().getProperty(prefix + "." + name);
            if (!StringUtils.hasText(propertyValue)) {
                return ConditionOutcome.noMatch("Property " + prefix + "." + name + " is empty or not defined");
            }
        }
        return ConditionOutcome.match();
    }
}
