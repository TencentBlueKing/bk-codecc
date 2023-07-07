package com.tencent.devops.common.api.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 国际化字段标记
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface I18NFieldMarker {

    /**
     * 模块
     *
     * @return
     */
    String moduleCode();

    /**
     * 资源编码值的对应字段
     *
     * @return
     */
    String keyFieldHolder();
}
