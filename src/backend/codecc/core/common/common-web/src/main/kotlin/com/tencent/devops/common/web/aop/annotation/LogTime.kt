package com.tencent.devops.common.web.aop.annotation

import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit

@Inherited
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogTime (val threshold: Int,val unit:TimeUnit){
}