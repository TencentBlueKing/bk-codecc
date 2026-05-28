/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.security

import com.tencent.devops.common.web.security.filter.OpAdminAuthFilter
import com.tencent.devops.common.web.security.filter.PermissionAuthFilter
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Method
import javax.ws.rs.Path
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext

class PermissionAuthDynamicFeature : DynamicFeature {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionAuthDynamicFeature::class.java)
    }

    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        /* 开启CGLib代理后，无法通过getResourceMethod()方法直接获取原始方法
        if (resourceInfo.resourceMethod.isAnnotationPresent(AuthMethod::class.java)) {
            val annotation = resourceInfo.resourceMethod.getAnnotation(AuthMethod::class.java)
            context.register(PermissionAuthFilter(annotation.permission.toList()))
            return
        }*/

        // 方法级的注解比类级的注解优先级高
        var method: Method = resourceInfo.resourceMethod
        // 如果是编译器生成的桥接方法，要使用getDeclaringClass()方法获取原始方法的声明类
        if (method.isBridge) {
            method = method.declaringClass.getDeclaredMethod(
                method.name, *method.parameterTypes
            )
        }
        val annotatedMethod = AnnotationUtils.findAnnotation(
            method, AuthMethod::class.java
        )
        if (annotatedMethod != null) {
            with(annotatedMethod) {
                context.register(
                    PermissionAuthFilter(
                        resourceType,
                        permission.toList(),
                        roles.toList(),
                        extPassClassName.simpleName!!
                    )
                )
            }

            return
        }

        // 子类优先，如果有则也对父类生效
        if (resourceInfo.resourceClass.isAnnotationPresent(AuthMethod::class.java)) {
            val annotation = resourceInfo.resourceClass.getAnnotation(AuthMethod::class.java)
            context.register(PermissionAuthFilter(annotation.resourceType, annotation.permission.toList(), annotation
                .roles.toList(), annotation.extPassClassName.simpleName!!))
            return
        }

        // 获取方法声明类是否有AuthMethod配置
        val clazz = resourceInfo.resourceMethod.declaringClass
        if (clazz.isAnnotationPresent(AuthMethod::class.java)) {
            val annotation = clazz.getAnnotation(AuthMethod::class.java)
            context.register(PermissionAuthFilter(annotation.resourceType, annotation.permission.toList(), annotation
                .roles.toList(), annotation.extPassClassName.simpleName!!))
            return
        }

        // 兜底：未声明 @AuthMethod 的 OP 路径（/op/...）默认按管理员维度做请求方校验，
        // 避免新增运营接口因忘加注解或忘写实现层校验而出现裸奔。
        // 若接口确有理由跳过此兜底（例如完全公开或实现层自行处理更严格校验），
        // 可在类或方法上显式标注 @AuthBypass。
        if (shouldApplyOpAdminFallback(resourceInfo, method)) {
            context.register(OpAdminAuthFilter())
            logger.info(
                "op fallback admin filter registered: ${resourceInfo.resourceClass.simpleName}#${method.name}"
            )
        }
    }

    private fun shouldApplyOpAdminFallback(resourceInfo: ResourceInfo, method: Method): Boolean {
        if (AnnotationUtils.findAnnotation(method, AuthBypass::class.java) != null) {
            return false
        }
        if (resourceInfo.resourceClass.isAnnotationPresent(AuthBypass::class.java)) {
            return false
        }
        // resourceClass 是 RestResource 实现类，@Path 一般在接口上；用 method.declaringClass 兜底
        val classPath = findClassPath(resourceInfo.resourceClass)
            ?: findClassPath(method.declaringClass)
            ?: return false
        val methodPath = AnnotationUtils.findAnnotation(method, Path::class.java)?.value.orEmpty()
        val normalizedClass = normalize(classPath)
        val normalizedMethod = normalize(methodPath)
        val full = if (normalizedMethod.isEmpty()) normalizedClass else "$normalizedClass/$normalizedMethod"
        return full == "/op" || full.startsWith("/op/")
    }

    private fun findClassPath(clazz: Class<*>): String? {
        val direct = AnnotationUtils.findAnnotation(clazz, Path::class.java)?.value
        if (direct != null) return direct
        clazz.interfaces.forEach {
            val v = AnnotationUtils.findAnnotation(it, Path::class.java)?.value
            if (v != null) return v
        }
        return null
    }

    private fun normalize(raw: String): String {
        if (raw.isEmpty()) return ""
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}
