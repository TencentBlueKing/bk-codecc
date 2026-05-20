/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.web.security

import java.lang.annotation.Inherited

/**
 * 标注于 OP 路径（`/op/...`）下接口的类或方法，表示其调用方校验由实现层自行负责，
 * 框架层 PermissionAuthDynamicFeature 不再对其做兜底拦截。
 *
 * 仅在确实由实现类做了等价或更严格的调用方校验时使用；如无明确理由，请不要使用本注解。
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class AuthBypass
