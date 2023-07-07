package com.tencent.bk.codecc.defect.aop

import com.tencent.devops.common.web.aop.annotation.LogTime
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit

@Aspect
@Component
class LogTimeAop {

    companion object {
        private val logger = LoggerFactory.getLogger(LogTimeAop::class.java)
    }

    @Pointcut("@annotation(com.tencent.devops.common.web.aop.annotation.LogTime)")
    fun logTime() {
    }

    @Around("logTime()")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint : ProceedingJoinPoint) : Any?{
        //出去类名
        val className = joinPoint.target.javaClass.name
        //取出方法名
        val method = (joinPoint.signature as MethodSignature).method
        val methodName = method.name
        //取出打印阙值
        var threshold:Long = -1
        if(method.isAnnotationPresent(LogTime::class.java)){
            threshold = method.getAnnotation(LogTime::class.java).threshold.toLong()
            val unit = method.getAnnotation(LogTime::class.java).unit
            threshold = TimeUnit.MILLISECONDS.convert(threshold,unit)
        }
        val startTime = Date()
        val result = try {
            //执行原来的方法
            joinPoint.proceed()
        }catch (throwable : Throwable){
            val endTime = Date()
            val executeTime = endTime.time - startTime.time
            //输出执行时间
            logger.info("ClassName:$className ,MethodName:$methodName ,Cause Error Msg:${throwable.message}" +
                    ",Start In:$startTime ,End In:$endTime ,Execute Time:$executeTime")
            throw throwable
        }
        val endTime = Date()
        val executeTime = endTime.time - startTime.time
        //输出执行时间
        if(threshold < executeTime){
            logger.info("ClassName:$className ,MethodName:$methodName ,Start In:$startTime " +
                    ",End In:$endTime ,Execute Time:$executeTime")
        }
        return result
    }

}