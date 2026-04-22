package com.codecc.preci.service.oauth

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OAuthConfigLoader 测试")
class OAuthConfigLoaderTest {

    @Test
    @DisplayName("从 classpath 加载 config.json")
    fun testLoadConfig() {
        val config = OAuthConfigLoader.load()
        assertNotNull(config)
        assertEquals("https://bkauth-test.example.com/realms/devops", config.bkauthBaseUrl)
        assertEquals("preci-ide-plugin-test", config.clientId)
        assertEquals("service:codecc", config.resource)
        assertEquals("", config.scope)
        assertEquals("/api/preci/oauth/callback", config.redirectPath)
    }
}
