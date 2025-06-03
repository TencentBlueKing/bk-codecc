package com.tencent.devops.common.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableMap
import com.tencent.devops.common.api.auth.AUTH_HEADER_BK_API_AUTH
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TENANT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.pojo.BkUserGetUserRespVO
import com.tencent.devops.common.codecc.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils.doGet
import org.apache.commons.lang.StringUtils

object BkUserClient {
    private fun genCommonHeader(bkIamAppCode: String, bkIamAppSecret: String, tenantId: String): Map<String, String> {
        val authValue = String.format(
            "{\"bk_app_code\": \"%s\", \"bk_app_secret\": \"%s\"}",
            bkIamAppCode, bkIamAppSecret
        )
        return ImmutableMap.of<String, String>(
            AUTH_HEADER_DEVOPS_TENANT_ID, tenantId,
            AUTH_HEADER_BK_API_AUTH, authValue
        )
    }

    fun getUserName(
        host: String?,
        bkIamAppCode: String?,
        bkIamAppSecret: String?,
        tenantId: String,
        userId: String
    ): String {
        if (host.isNullOrBlank() || bkIamAppCode.isNullOrBlank() || bkIamAppSecret.isNullOrBlank()) {
            return userId
        }

        val url = String.format(
            "%s/api/bk-user/prod/api/v3/open/tenant/users/%s/",
            host, userId
        )
        val header = genCommonHeader(bkIamAppCode, bkIamAppSecret, tenantId)
        val originResp = doGet(url, header)

        if (StringUtils.isBlank(originResp)) {
            return userId
        }

        val resp = JsonUtil.to(originResp, object : TypeReference<Result<BkUserGetUserRespVO>>() {})
        if (resp.isNotOk() || resp.data == null || resp.data!!.display_name == null) {
            return userId
        }

        return resp.data!!.display_name!!
    }

    fun batchSearchUser(tenantId: String, userName: List<String>): Map<String, String> {
        // TODO
        return emptyMap()
    }
}