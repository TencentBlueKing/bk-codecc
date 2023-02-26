package com.tencent.devops.common.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 国际化字段标记
 * 定义"T"为数据的真实类型，则支持返回类型有：T、List<T>、Result<T>、Result<List<T>、? extend List<T>
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface I18NResponse {

}
