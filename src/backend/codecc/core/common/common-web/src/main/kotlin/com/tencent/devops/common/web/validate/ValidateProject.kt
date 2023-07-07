package com.tencent.devops.common.web.validate

import java.lang.annotation.Inherited

/**
 * 根据 Project Header 校验
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class ValidateProject()
