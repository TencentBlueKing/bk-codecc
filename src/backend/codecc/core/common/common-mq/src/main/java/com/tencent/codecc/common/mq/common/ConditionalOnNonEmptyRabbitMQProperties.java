package com.tencent.codecc.common.mq.common;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(NonEmptyRabbitMQPropertiesCondition.class)
public @interface ConditionalOnNonEmptyRabbitMQProperties {
    String prefix() default "";
    String[] name() default {};
}