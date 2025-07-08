/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RBACAuthProperties {
    /**
     * 后台接口根路径
     */
    @Value("\${bkci.private.url:#{null}}")
    val url: String? = null

    /**
     * 后台接口根路径
     */
    @Value("\${bkci.public.schemes:http}")
    val schemes : String = "http"

    /**
     * RBAC权限系统资源类型
     */
    @Value("\${auth.rbac.resourceType:codecc_task}")
    val rbacResourceType: String? = null

    /**
     * RBAC权限系统资源类型
     */
    @Value("\${auth.rbac.pipelineResourceType:pipeline}")
    val pipeLineResourceType: String? = null

    /**
     * 规则集资源类型
     */
    @Value("\${auth.rbac.rulesetResourceType:codecc_rule_set}")
    val rulesetResourceType: String? = null

    /**
     * 忽略类型资源类型
     */
    @Value("\${auth.rbac.ignoreTypeResourceType:codecc_ignore_type}")
    val ignoreTypeResourceType: String? = null

    /**
     * 接口access token
     */
    @Value("\${auth.rbac.token:#{null}}")
    val token: String? = null
}
