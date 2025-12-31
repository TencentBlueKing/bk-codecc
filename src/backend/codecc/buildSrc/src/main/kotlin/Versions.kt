object Release {
    const val Group = "com.tencent.devops"
    const val Version = "0.0.1"
}

object Versions {
    const val Kotlin = "2.0.21"
    const val swaggerVersion = "2.2.16"
    const val jaxrsVersion = "3.1.0"
    const val hashidsVersion = "1.0.3"
    const val feignVersion = "13.5"  // 升级：支持 Jakarta EE (feign-jaxrs3)
    const val svnkitVersion = "1.9.3"
    const val vmwareVersion = "5.1"
    const val okHttpVersion = "4.9.0"
    const val commonExecVersion = "1.3"
    const val bouncyCastleVersion = "1.77"  // 升级：从 1.46，使用 jdk18on 模块（1.71+）
    const val dom4jVersion = "2.1.4"  // 升级：从 1.6.1，修复安全漏洞
    const val slf4jVersion = "2.0.7"
    const val mockitoVersion = "5.7.0"  // 升级：从 1.10.19，JDK 17 兼容
    const val httpclientVersion = "4.5.13"
    const val compressVersion = "1.25.0"
    const val jjwtVersion = "0.11.5"  // 升级：从 0.9.0，JDK 17 兼容
    const val jsonLibVersion = "2.4"
    const val commonLang3Version = "3.5"
    const val guavaVersion = "31.0.1-jre"
    const val cronutilsVersion = "9.1.6"
    const val reflectionsVersion = "0.10.2"
    const val jsonSchemaVersion = "2.2.6"
    const val logbackVersion = "1.4.11"  // 升级：从 1.1.11，Spring Boot 3 兼容
    const val jasyptVersion = "3.0.5"
    const val jolokiaVersion = "1.6.2"
    const val awsS3Version = "1.11.461"
    const val poiVersion = "4.1.1"
    const val springWebSocketVersion = "2.3.4.RELEASE"
    const val lombokVersion = "1.18.34"
    // const val tomcatEmbedCoreVersion = "9.0.39"  // 移除：让 Spring Boot 3 自动管理 Tomcat 10+
    const val commonCollection = "3.2.2"

    const val commonPool2Version = "2.11.1"  // 添加：Jedis 连接池需要
    const val jedisVersion = "5.1.0"  // 添加：Spring Boot 3 兼容的 Jedis 版本
    const val lettuceVersion = "6.2.6.RELEASE"  // 升级：从 4.5.0.Final，Spring Boot 3 兼容
    const val jerseyValidationVersion = "3.1.3"
    const val commonsIOVersion = "2.8.0"
    const val xmlrpcVersion = "3.1.3"
    const val commonsHttpclientVersion = "3.1"
    const val jsonVersion = "20180130"
    const val redissionVersion = "3.17.3"
    const val log4jVersion = "2.23.1"  // 升级：从 2.17.1，Spring Boot 3.3.2 要求 2.19.0+
    const val easyexcel = "2.2.7"
    const val redisson = "3.15.4"
    const val ktlintVersion = "0.29.0"
    const val lucene = "8.11.0"
    const val fastjson = "1.2.83"
    const val fastjson2 = "2.0.53"  // Fastjson2: 支持 Jakarta EE, 性能更优, 修复安全漏洞
    const val quartz = "2.3.2"  // 升级：从 2.1.3
    const val quartzJobs = "2.3.2"  // 升级：从 2.2.3，与 quartz 版本统一
    const val micrometerVersion = "1.10.12"  // 升级：从 1.6.6，Spring Boot 3 兼容
    const val opentelemetryVersion = "1.17.0"
    const val iamSdkVersion = "2.0.0"
    const val reflectasmVersion = "1.11.9"
    const val cryptSdkVersion = "1.1.3"
    const val kotlinxVersion = "1.6.4"
    const val cosVersion = "5.6.185"
    const val xzVersion = "1.9"
    const val bkAudit = "2.0.0"

    // Ktor 框架 (用于 LLM 模块) - JDK 8+
    const val ktorVersion = "2.2.1"

    // WebSocket API - Jakarta EE 9+ 版本 (JDK 11+)
    const val jakartaWebsocketVersion = "2.1.0"

    // JSON 处理库 - JDK 8+
    const val gsonVersion = "2.8.9"

    // 工具库
    const val groovyVersion = "3.0.10"  // ⚠️ 从 2.5.3 升级，Groovy 3.0+ 支持 JDK 17
    const val freemarkerVersion = "2.3.31"  // JDK 8+
    const val networkntJsonSchemaVersion = "1.0.49"  // JDK 8+
    const val commonsLangVersion = "2.6"  // JDK 1.5+
    const val commonsCodecVersion = "1.9"  // JDK 1.5+

    // 测试依赖
    const val mockitoKotlinVersion = "1.6.0"

    // 扩展模块依赖
    const val antVersion = "1.10.5"  // Apache Ant - JDK 8+
    const val commonsTextVersion = "1.9"  // Apache Commons Text - JDK 8+
    const val openai4jVersion = "0.17.0"  // OpenAI4J - AI 客户端
    const val retrofit2Version = "2.9.0"  // Retrofit2 - HTTP 客户端


    // Devops 依赖版本
    const val devopsVersion = "4.1.0-rc.3"
}
