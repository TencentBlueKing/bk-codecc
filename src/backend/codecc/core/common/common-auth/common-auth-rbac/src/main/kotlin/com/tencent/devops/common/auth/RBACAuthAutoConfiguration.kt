package com.tencent.devops.common.auth

import com.tencent.devops.common.auth.api.external.RBACAuthPermissionApi
import com.tencent.devops.common.auth.api.external.RBACAuthProperties
import com.tencent.devops.common.auth.api.external.RBACAuthPropertiesData
import com.tencent.devops.common.auth.api.external.RBACAuthRegisterApi
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.data.redis.core.RedisTemplate

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "auth", name = ["idProvider"], havingValue = "rbac")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class RBACAuthAutoConfiguration {

    @Bean
    fun rbacAuthPermissionApi(
        authPropertiesData: RBACAuthPropertiesData,
        redisTemplate: RedisTemplate<String, String>,
        client: Client
    ) = RBACAuthPermissionApi(client, redisTemplate, authPropertiesData)

    @Bean
    fun rbacAuthRegisterApi(
        rbacAuthProperties: RBACAuthProperties,
        client: Client
    ) = RBACAuthRegisterApi(client, rbacAuthProperties)

    @Bean
    fun rbacAuthProperties() = RBACAuthProperties()

    @Bean
    fun rbacAuthPropertiesDev(rbacAuthProperties: RBACAuthProperties) = RBACAuthPropertiesData(
        rbacResourceType = rbacAuthProperties.rbacResourceType,
        url = rbacAuthProperties.url,
        token = rbacAuthProperties.token
    )
}
