/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.web.security

/**
 * 可插拔的网关下发 token 校验接口
 *
 * 不实现该 bean 时，PermissionAuthFilter 仅做 header 存在性检查；
 * 通过 Spring Bean 注册实现（例如 JWT 验签、HMAC 校验、对接 IAM 等）后，
 * 框架会自动调用 verify(token, userName) 做真正的来源校验。
 */
interface GatewayTokenVerifier {

    /**
     * 校验请求是否经由可信网关签发，且 token 与 userName 匹配
     *
     * @param token    请求头中携带的 token 原始字符串
     * @param userName 请求头中携带的 X-DEVOPS-UID
     * @return true 表示来源可信；false 表示拒绝
     */
    fun verify(token: String, userName: String?): Boolean
}
