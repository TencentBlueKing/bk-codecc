package com.codecc.preci.service.oauth

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * OAuth 配置加载器
 *
 * 从 classpath 中的 config.json 加载 [OAuthConfig]。
 *
 * @since 2.0
 */
object OAuthConfigLoader {

    private val json = Json { ignoreUnknownKeys = true }

    private val cached: OAuthConfig by lazy {
        val inputStream = OAuthConfigLoader::class.java.classLoader
            .getResourceAsStream("config.json")
            ?: throw IllegalStateException("config.json not found in classpath")

        val content = inputStream.bufferedReader().use { it.readText() }
        json.decodeFromString<OAuthConfig>(content)
    }

    /**
     * 加载 OAuth 配置
     *
     * 首次调用从 classpath 读取并缓存，后续调用返回缓存。
     *
     * @return OAuth 配置
     * @throws IllegalStateException 如果 config.json 不存在于 classpath
     * @throws kotlinx.serialization.SerializationException 如果 JSON 格式错误或无法反序列化为 [OAuthConfig]
     */
    fun load(): OAuthConfig = cached
}
